package objectframework.content

import java.time.Instant
import scala.concurrent.ExecutionContext

import cats.data._
import cats.implicits._
import org.json4s._
import org.json4s.JsonDSL._

import core.db.ExPostgresDriver.api._
import core.db._

import objectframework.ObjectFailures._

package object illuminated {
  case class ContentAttribute(t: String, v: JValue)

  type ContentAttributes = Map[String, ContentAttribute]

  type ContentRelations = Map[String, Seq[Commit#Id]]

  type FullContentRelations = Map[String, Seq[Content]]

  /**
    * Content is an illuminated content object: it's the most primitive Object
    * Framework entity that should be used outside this library. Any objects
    * that are implemented on top of this model should leverage Content.
    */
  case class Content(id: Int,
                     viewId: Option[View#Id],
                     commitId: Commit#Id,
                     attributes: ContentAttributes,
                     relations: ContentRelations,
                     createdAt: Instant,
                     updatedAt: Instant,
                     archivedAt: Option[Instant])

  object Content {
    implicit val formats = DefaultFormats

    type ContentHeadTuple = (Head, Commit, Form, Shadow)
    type ContentTuple     = (Commit, Form, Shadow)

    def build(tuple: ContentTuple): Content = {
      val (commit, form, shadow) = tuple
      build(commit, form, shadow)
    }

    def build(tuple: ContentHeadTuple): Content = {
      val (head, commit, form, shadow) = tuple
      build(commit, form, shadow).copy(viewId = Some(head.viewId), archivedAt = head.archivedAt)
    }

    def build(commit: Commit, form: Form, shadow: Shadow): Content =
      Content(id = form.id,
              viewId = None,
              commitId = commit.id,
              attributes = buildContentAttributes(form, shadow),
              relations = buildContentRelations(shadow.relations),
              createdAt = form.createdAt,
              updatedAt = shadow.createdAt,
              archivedAt = None)

    private def buildContentAttributes(form: Form, shadow: Shadow): ContentAttributes = {
      val formJson   = form.attributes
      val shadowJson = shadow.attributes

      (formJson, shadowJson) match {
        case (JObject(form), JObject(shadow)) ⇒
          shadow.obj.foldLeft(Map.empty[String, ContentAttribute]) { (attributes, obj) ⇒
            val (attr, link) = obj
            def typed = (link \ "type").extract[String]
            def ref   = link \ "ref"

            ref match {
              case JString(attrKey) ⇒
                val attrVal = formJson \ attrKey
                attributes + (attrKey → ContentAttribute(t = typed, v = attrVal))
              case _ ⇒
                attributes
            }
          }
        case _ ⇒
          Map.empty[String, ContentAttribute]
      }
    }

    private def buildContentRelations(rawRelations: JValue): ContentRelations = {
      val contentRelations = Map.empty[String, Seq[Commit#Id]]

      rawRelations match {
        case JObject(relationObj) ⇒
          relationObj.foldLeft(contentRelations) { (relations, relation) ⇒
            val (kind, commitList) = relation
            relations + (kind → commitList.extract[Seq[Commit#Id]])
          }
        case _ ⇒
          contentRelations
      }
    }
  }

  object Contents {
    def findLatestById(id: Form#Id, viewId: View#Id, kind: String)(
        implicit ec: ExecutionContext): DbResultT[Content] = {
      for {
        content ← * <~ ContentQueries
                   .filterLatestById(id, viewId)
                   .mustFindOneOr(ObjectNotFound(kind, id, viewId))
      } yield Content.build(content)
    }

    def findByCommit(commitId: Commit#Id, kind: String)(
        implicit ec: ExecutionContext): DbResultT[Content] =
      for {
        content ← * <~ ContentQueries
                   .filterByCommit(commitId)
                   .mustFindOneOr(ObjectNotFoundAtCommit(kind, commitId))
      } yield Content.build(content)

    def populateRelations(content: Content)(
        implicit ec: ExecutionContext): DbResultT[FullContentRelations] = {
      val empty = DbResultT.pure(Map.empty[String, Seq[Content]])

      content.relations.foldLeft(empty) { (accRelations, relation) ⇒
        accRelations.flatMap { relations ⇒
          val (kind, commits) = relation

          for {
            rawRelations ← * <~ ContentQueries.filterRelation(kind, commits).result
            contents     ← * <~ rawRelations.map(Content.build _)
          } yield addRelation(relations, kind, contents)
        }
      }
    }

    private def addRelation(relations: Map[String, Seq[Content]],
                            kind: String,
                            content: Seq[Content]): Map[String, Seq[Content]] =
      relations.get(kind) match {
        case Some(existingContent) ⇒
          val mergedContent = (existingContent ++ content).toSet.toList
          relations + (kind → mergedContent)
        case None ⇒
          relations + (kind → content)
      }

  }

  object ContentQueries {
    type QuerySeq     = Query[(Commits, Forms, Shadows), (Commit, Form, Shadow), Seq]
    type HeadQuerySeq = Query[(Heads, Commits, Forms, Shadows), (Head, Commit, Form, Shadow), Seq]

    def filterLatestById(id: Form#Id, viewId: View#Id): HeadQuerySeq =
      for {
        head   ← Heads.filter(h ⇒ h.id === id && h.viewId === viewId)
        commit ← Commits if commit.id === head.commitId
        form   ← Forms if form.id === commit.formId
        shadow ← Shadows if shadow.id === commit.shadowId
      } yield (head, commit, form, shadow)

    def filterByCommit(commitId: Commit#Id): QuerySeq =
      for {
        commit ← Commits.filter(_.id === commitId)
        form   ← Forms if form.id === commit.formId
        shadow ← Shadows if shadow.id === commit.shadowId
      } yield (commit, form, shadow)

    def filterRelation(kind: String, commits: Seq[Commit#Id]): QuerySeq =
      for {
        commit ← Commits.filter(_.id.inSet(commits))
        form   ← Forms if form.id === commit.formId && form.kind === kind
        shadow ← Shadows if shadow.id === commit.shadowId
      } yield (commit, form, shadow)
  }
}
