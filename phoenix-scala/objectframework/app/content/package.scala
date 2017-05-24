package objectframework.content

import java.time.Instant

import org.json4s._
import org.json4s.JsonDSL._
import utils.db.ExPostgresDriver.api._
import utils.db._

package object illuminated {
  case class ContentAttribute(t: String, v: JValue)

  type ContentAttributes = Map[String, ContentAttribute]

  type ContentRelations = Map[String, Seq[Commit#Id]]

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
    // def build(commit: Commit, form: Form, shadow: Shadow): Content = {
    //   Content(id = form.id, viewId = None, commitId = commit.id, 
    // }

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
  }

  object ContentQueries {
    type QuerySeq = Query[(Commits, Forms, Shadows), (Commit, Form, Shadow), Seq]

    def filterLatestById(id: Form#Id, viewId: View#Id): QuerySeq =
      for {
        head   ← Heads.filter(h ⇒ h.id === id && h.viewId === viewId)
        commit ← Commits if commit.id === head.commitId
        form   ← Forms if form.id === commit.formId
        shadow ← Shadows if shadow.id === commit.shadowId
      } yield (commit, form, shadow)

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
