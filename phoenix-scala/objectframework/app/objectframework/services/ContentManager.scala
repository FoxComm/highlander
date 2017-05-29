package objectframework.services

import org.json4s._

import cats.data._
import cats.implicits._

import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failure

import objectframework.IlluminateAlgorithm
import objectframework.content._
import objectframework.db._
import objectframework.ObjectFailures._
import objectframework.payloads.ContentPayloads._

object ContentManager {
  type FullContentRelations = Map[String, Seq[Content]]

  def findLatestById(id: Form#Id, viewId: View#Id, kind: String)(
      implicit ec: EC): DbResultT[Content] = {
    for {
      contentTuple ← * <~ ContentQueries
                      .filterLatestById(id, viewId)
                      .mustFindOneOr(ObjectNotFound(kind, id, viewId))
      (head, commit, form, shadow) = contentTuple
      content ← * <~ Content.build(head, commit, form, shadow)
    } yield content
  }

  def findByCommit(commitId: Commit#Id, kind: String)(implicit ec: EC): DbResultT[Content] =
    for {
      contentTuple ← * <~ ContentQueries
                      .filterByCommit(commitId)
                      .mustFindOneOr(ObjectNotFoundAtCommit(kind, commitId))
      (commit, form, shadow) = contentTuple
      content ← * <~ Content.build(commit, form, shadow)
    } yield content

  def create(viewId: Int, payload: CreateContentPayload)(implicit ec: EC,
                                                         fmt: Formats): DbResultT[Content] = {
    val (formJson, shadowJson) = ContentUtils.encodeContentAttributes(payload.attributes)

    for {
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributesTypes(formJson, shadowJson))
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributes(formJson, shadowJson))

      form    ← * <~ Forms.create(Form(kind = payload.kind, attributes = formJson))
      shadow  ← * <~ Shadows.create(Shadow.build(form.id, shadowJson, payload.relations))
      commit  ← * <~ Commits.create(Commit(formId = form.id, shadowId = shadow.id))
      head    ← * <~ Heads.create(Head(kind = payload.kind, viewId = viewId, commitId = commit.id))
      created ← * <~ Content.build(head, commit, form, shadow)
    } yield created
  }

  def getRelations(content: Content)(implicit ec: EC): DbResultT[FullContentRelations] = {
    val empty = DbResultT.pure(Map.empty[String, Seq[Content]])

    content.relations.foldLeft(empty) { (accRelations, relation) ⇒
      accRelations.flatMap { relations ⇒
        val (kind, commits) = relation

        for {
          rawRelations ← * <~ ContentQueries.filterRelation(kind, commits).result
          contents ← * <~ rawRelations.map {
                      case (commit, form, shadow) ⇒
                        DbResultT.fromEither(Content.build(commit, form, shadow))
                    }
        } yield aggregateRelation(relations, kind, contents)
      }
    }
  }

  private def aggregateRelation(relations: Map[String, Seq[Content]],
                                kind: String,
                                content: Seq[Content]): Map[String, Seq[Content]] =
    relations.get(kind) match {
      case Some(existingContent) ⇒
        val mergedContent = (existingContent ++ content).toSet.toList
        relations + (kind → mergedContent)
      case None ⇒
        relations + (kind → content)
    }

  private def failIfErrors(errors: Seq[Failure])(implicit ec: EC): DbResultT[Unit] =
    errors match {
      case head :: tail ⇒ DbResultT.failures(NonEmptyList(head, tail))
      case Nil          ⇒ DbResultT.pure(Unit)
    }
}
