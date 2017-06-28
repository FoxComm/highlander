package objectframework.services

import org.json4s._
import cats.data._
import cats.implicits._
import cats.kernel.Monoid
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failure
import objectframework.IlluminateAlgorithm
import objectframework.content._
import objectframework.db._
import objectframework.failures._
import objectframework.ObjectFailures._
import objectframework.payloads.ContentPayloads._

object ContentManager {

  def findLatest(id: Form#Id, viewId: View#Id, kind: String)(implicit ec: EC): DbResultT[Content] =
    for {
      contentTuple ← * <~ ContentQueries
                      .filterLatestById(id, viewId, kind)
                      .mustFindOneOr(ObjectNotFound(kind, id, viewId))
      (head, commit, form, shadow) = contentTuple
      content ← * <~ Content.build(head, commit, form, shadow)
    } yield content

  def findByCommit(commitId: Commit#Id, kind: String)(implicit ec: EC): DbResultT[Content] =
    for {
      contentTuple ← * <~ ContentQueries
                      .filterByCommit(commitId, kind)
                      .mustFindOneOr(ObjectNotFoundAtCommit(kind, commitId))
      (commit, form, shadow) = contentTuple
      content ← * <~ Content.build(commit, form, shadow)
    } yield content

  def create(viewId: Int, payload: CreateContentPayload)(implicit ec: EC, fmt: Formats): DbResultT[Content] = {
    val (formJson, shadowJson) = ContentUtils.encodeContentAttributes(payload.attributes)

    for {
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributesTypes(formJson, shadowJson))
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributes(formJson, shadowJson))
      _ ← * <~ validateRelations(payload.relations)

      form    ← * <~ Forms.create(Form(kind = payload.kind, attributes = formJson))
      shadow  ← * <~ Shadows.create(Shadow.build(form.id, shadowJson, payload.relations))
      commit  ← * <~ Commits.create(Commit(formId = form.id, shadowId = shadow.id))
      head    ← * <~ Heads.create(Head(kind = payload.kind, viewId = viewId, commitId = commit.id))
      created ← * <~ Content.build(head, commit, form, shadow)
    } yield created
  }

  type FullContentRelations = Map[String, Seq[Content]]
  def getRelations(content: Content)(implicit ec: EC): DbResultT[FullContentRelations] = {
    val empty = (Map.empty: FullContentRelations).pure[DbResultT]

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

  private def validateRelations(relations: Content.ContentRelations)(implicit ec: EC): Seq[DbResultT[Unit]] =
    relations.foldLeft(Seq.empty[DbResultT[Unit]]) {
      case (acc, (kind, expectedIds)) ⇒
        acc :+ (for {
          actualIds ← * <~ ContentQueries.filterCommitIds(kind, expectedIds).result
          _         ← * <~ validateAllCommits(kind, expectedIds, actualIds)
        } yield ())
    }

  private def validateAllCommits(kind: String,
                                 expectedCommits: Seq[Commit#Id],
                                 actualCommits: Seq[Commit#Id])(implicit ec: EC): DbResultT[Unit] = {

    val errors = expectedCommits.toSet.diff(actualCommits.toSet).foldLeft(Seq.empty[Failure]) {
      (acc, commitId) ⇒
        acc :+ RelatedContentDoesNotExist(kind, commitId)
    }

    failIfErrors(errors)
  }

  private def failIfErrors(errors: Seq[Failure])(implicit ec: EC): DbResultT[Unit] =
    errors match {
      case head :: tail ⇒ DbResultT.failures(NonEmptyList(head, tail))
      case Nil          ⇒ ().pure[DbResultT]
    }
}
