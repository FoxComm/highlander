package objectframework.services

import java.time.Instant

import org.json4s._
import cats.data._
import cats.implicits._
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.{Failure, Failures}
import objectframework.IlluminateAlgorithm
import objectframework.content._
import objectframework.db._
import objectframework.failures._
import objectframework.ObjectFailures._
import objectframework.payloads.ContentPayloads._

object ContentManager {

  def findLatest(id: Form#Id, viewId: View#Id, kind: String)(implicit ec: EC): DbResultT[Content] = {
    for {
      content ← * <~ mustFind(ContentQueries.filterLatest(id, viewId, kind), id, viewId, kind)
      latest  ← * <~ (Content.buildLatest _).tupled(content)
    } yield latest
  }

  def findByCommit(commitId: Commit#Id, kind: String)(implicit ec: EC): DbResultT[Content] =
    for {
      content ← * <~ ContentQueries
                 .filterByCommit(commitId, kind)
                 .mustFindOneOr(ObjectNotFoundAtCommit(kind, commitId))
      atCommit ← * <~ Content.buildCommitT(content)
    } yield atCommit

  def create(viewId: Int, payload: CreateContentPayload)(implicit ec: EC,
                                                         fmt: Formats): DbResultT[Content] = {
    val (formJson, shadowJson) = ContentUtils.encodeContentAttributes(payload.attributes)

    for {
      _ ← * <~ payload.validate
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributesTypes(formJson, shadowJson))
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributes(formJson, shadowJson))
      _ ← * <~ validateRelations(id = None, kind = payload.kind, relations = payload.relations)

      form    ← * <~ Forms.create(Form(kind = payload.kind, attributes = formJson))
      shadow  ← * <~ Shadows.create(Shadow.build(form.id, shadowJson, payload.relations))
      commit  ← * <~ Commits.create(Commit(formId = form.id, shadowId = shadow.id))
      head    ← * <~ Heads.create(Head(kind = payload.kind, viewId = viewId, commitId = commit.id))
      created ← * <~ Content.buildLatest(head, commit, form, shadow)
    } yield created
  }

  def update(id: Int, viewId: Int, payload: UpdateContentPayload, kind: String)(
      implicit ec: EC,
      fmt: Formats): DbResultT[Content] =
    for {
      // Validate the payload and retrieve the existing Content object.
      _        ← * <~ payload.validate
      existing ← * <~ mustFind(ContentQueries.filterLatest(id, viewId, kind), id, viewId, kind)
      (head, commit, form, shadow) = existing

      // Update the form/shadow attributes and validate for correctness.
      newJson ← * <~ ContentUtils.attributesForUpdate(form, shadow, payload.attributes)
      (newFormJson, newShadowJson) = newJson
      _ ← * <~ failIfErrors(
             IlluminateAlgorithm.validateAttributesTypes(newFormJson, newShadowJson))
      _ ← * <~ failIfErrors(IlluminateAlgorithm.validateAttributes(newFormJson, newShadowJson))

      // Update the relations and validate for correctness.
      relations = ContentUtils.updateRelations(shadow.relations, payload.relations)
      _ ← * <~ validateRelations(id = Some(commit.id), kind = kind, relations = relations)

      // Commit all the changes.
      updatedForm ← * <~ Forms.updateAttributes(form, newFormJson)
      newShadow   ← * <~ Shadows.create(Shadow.build(updatedForm.id, newShadowJson, relations))
      newCommit   ← * <~ Commits.create(Commit(formId = form.id, shadowId = shadow.id))
      updatedHead ← * <~ Heads.updateCommit(head, newCommit.id)
      updated     ← * <~ Content.buildLatest(updatedHead, newCommit, updatedForm, newShadow)
      _           ← * <~ updateParents(commit.id, newCommit.id, head.kind)
    } yield updated

  def archive(id: Int, viewId: Int, kind: String)(implicit ec: EC, fmt: Formats): DbResultT[Unit] =
    for {
      head ← * <~ mustFind(ContentQueries.filterHead(id, viewId, kind), id, viewId, kind)
      _    ← * <~ Heads.archive(head)
      _    ← * <~ removeFromParents(head.commitId, head.kind)
    } yield {}

  type FullContentRelations = Map[String, Seq[Content]]
  def getRelations(content: Content)(implicit ec: EC): DbResultT[FullContentRelations] = {
    val empty = DbResultT.pure(Map.empty[String, Seq[Content]])

    content.relations.foldLeft(empty) { (accRelations, relation) ⇒
      accRelations.flatMap { relations ⇒
        val (kind, commits) = relation

        for {
          rawRelations ← * <~ ContentQueries.filterRelation(kind, commits).result
          contents     ← * <~ rawRelations.map(r ⇒ DbResultT.fromEither((Content.buildCommitT(r))))
        } yield aggregateRelation(relations, kind, contents)
      }
    }
  }

  private def mustFind[M, T](query: Query[T, M, Seq], id: Int, viewId: Int, kind: String)(
      implicit ec: EC): DbResultT[M] =
    query.mustFindOneOr(ObjectNotFound(kind, id, viewId))

  // ------------------------------------------

  type UpdateParent    = (Commit#Id, String) ⇒ DbResultT[Commit#Id]
  type UpdateRelations = (Option[JValue]) ⇒ Content.ContentRelations

  private def _updateParents(commitId: Commit#Id, kind: String, updateParentFn: UpdateParent)(
      implicit ec: EC,
      fmt: Formats): DbResultT[Seq[Commit#Id]] = {
    for {
      parentIds ← * <~ ContentQueries.filterParentIds(commitId, kind)
      newIds    ← * <~ parentIds.map(p ⇒ updateParentFn(p._1, p._2))
    } yield newIds
  }

  private def _updateParent(
      parentId: Commit#Id,
      parentKind: String,
      updateRelationsFn: UpdateRelations)(implicit ec: EC, fmt: Formats): DbResultT[Commit#Id] = {
    for {
      parentTuple ← * <~ ContentQueries
                     .filterByCommit(parentId, parentKind)
                     .mustFindOneOr(ObjectNotFoundAtCommit(parentKind, parentId))

      (commit, form, shadow) = parentTuple
      relations              = updateRelationsFn(shadow.relations)

      newShadow ← * <~ Shadows.create(Shadow.build(form.id, shadow.attributes, relations))
      newCommit ← * <~ Commits.create(Commit(formId = form.id, shadowId = newShadow.id))
      oldHead ← * <~ Heads
                 .filter(h ⇒ h.commitId === commit.id && h.archivedAt.isEmpty)
                 .result
                 .headOption

      _ ← * <~ maybeUpdateHead(oldHead, newCommit.id)
      _ ← * <~ updateParents(commit.id, newCommit.id, parentKind)
    } yield newCommit.id
  }

  private def updateParents(oldCommitId: Commit#Id, newCommitId: Commit#Id, kind: String)(
      implicit ec: EC,
      fmt: Formats): DbResultT[Seq[Int]] = {
    _updateParents(oldCommitId, kind, { (parentCommitId, parentKind) ⇒
      updateParent(parentCommitId, parentKind, oldCommitId, newCommitId, kind)
    })
  }

  private def removeFromParents(commitId: Commit#Id, kind: String)(implicit ec: EC, fmt: Formats) =
    _updateParents(commitId, kind, { (parentCommitId, parentKind) ⇒
      removeFromParent(parentCommitId, parentKind, commitId, kind)
    })

  private def removeFromParent(parentId: Commit#Id,
                               parentKind: String,
                               toRemoveId: Commit#Id,
                               toRemoveKind: String)(implicit ec: EC, fmt: Formats) =
    _updateParent(parentId, parentKind, { relations ⇒
      ContentUtils.removeFromRelations(relations, toRemoveId, toRemoveKind)
    })

  private def updateParent(parentId: Commit#Id,
                           parentKind: String,
                           oldChildId: Commit#Id,
                           newChildId: Commit#Id,
                           childKind: String)(implicit ec: EC, fmt: Formats) =
    _updateParent(parentId, parentKind, { relations ⇒
      ContentUtils.updateRelatedContent(relations, childKind, oldChildId, newChildId)
    })

  // ------------------------------------------

  private def maybeUpdateHead(oldHead: Option[Head], newCommitId: Int)(implicit ec: EC) =
    oldHead match {
      case Some(head) ⇒
        for {
          newHead ← * <~ Heads.update(head,
                                      head.copy(commitId = newCommitId, updatedAt = Instant.now))
        } yield Some(newHead)
      case None ⇒
        DbResultT.pure(None)
    }

  private def collectParentIds(commitId: Commit#Id, kind: String, existingCommits: Seq[Commit#Id])(
      implicit ec: EC): DbResultT[Seq[Int]] = {
    for {
      parents ← * <~ ContentQueries.filterParentIds(commitId, kind)
      collectedIds ← * <~ parents.foldLeft(DbResultT.pure(existingCommits)) {
                      case (acc, (commitId, kind)) ⇒
                        acc.flatMap { existing ⇒
                          collectParentIds(commitId, kind, existing :+ commitId)
                        }
                    }
    } yield collectedIds
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

  /**
    * validateRelations ensures that all commits in the relations exist and that
    * committing these relations will not cause any cycles between content.
    */
  private def validateRelations(
      id: Option[Commit#Id],
      kind: String,
      relations: Content.ContentRelations)(implicit ec: EC): DbResultT[Unit] = {

    for {
      _ ← * <~ validateRelationsExist(relations)
      _ ← * <~ validateCycles(id, kind, relations)
    } yield {}
  }

  // Make sure that all relations exist.
  private def validateRelationsExist(relations: Content.ContentRelations)(implicit ec: EC) =
    relations.foldLeft(Seq.empty[DbResultT[Unit]]) {
      case (results, (kind, expectedIds)) ⇒
        def aggErrors(errors: Seq[Failure], commitId: Commit#Id) =
          errors :+ RelatedContentDoesNotExist(kind, commitId)

        val empty = Seq.empty[Failure]

        results :+ (for {
              actualIds ← * <~ ContentQueries.filterCommitIds(kind, expectedIds).result
              unexpectedIds = expectedIds.toSet.diff(actualIds.toSet)
              _ ← * <~ failIfErrors(unexpectedIds.foldLeft(empty)(aggErrors))
            } yield {})
    }

  // Make sure there are no cycles. 
  private def validateCycles(id: Option[Commit#Id],
                             kind: String,
                             relations: Content.ContentRelations)(implicit ec: EC) =
    id.foldLeft(DbResultT.unit) { (_, commitId) ⇒
      for {
        parentIds ← * <~ collectParentIds(commitId, kind, Seq.empty[Commit#Id])
        _         ← * <~ validateIdsNotInRelations(commitId, kind, relations, parentIds.toSet)
      } yield {}
    }

  private def validateIdsNotInRelations(entityId: Commit#Id,
                                        entityKind: String,
                                        relations: Content.ContentRelations,
                                        parentIds: Set[Commit#Id]): Either[Failures, Unit] = {
    val totalFailures = relations.foldLeft(Seq.empty[Failure]) {
      case (allFailures, (relKind, relCommits)) ⇒
        val failures = parentIds.intersect(relCommits.toSet).foldLeft(Seq.empty[Failure]) {
          (relFailures, relCommitId) ⇒
            relFailures :+ ImportCycleFound(entityId = entityId,
                                            entityKind = entityKind,
                                            relCommitId = relCommitId,
                                            relKind = relKind)
        }

        allFailures ++ failures
    }

    totalFailures match {
      case head :: tail ⇒ Left(NonEmptyList(head, tail))
      case _            ⇒ Right(Unit)
    }
  }

  private def failIfErrors(errors: Seq[Failure])(implicit ec: EC): DbResultT[Unit] = {
    errors match {
      case head :: tail ⇒ DbResultT.failures(NonEmptyList(head, tail))
      case Nil          ⇒ DbResultT.unit
    }
  }
}
