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
      _ ← * <~ payload.validate
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

  case class ContentComponents(head: Head, commit: Commit, form: Form, shadow: Shadow)
  private def mustFindLatest(id: Int, viewId: Int, kind: String)(implicit ec: EC) =
    for {
      contentTuple ← * <~ ContentQueries
                      .filterLatestById(id, viewId, kind)
                      .mustFindOneOr(ObjectNotFound(kind, id, viewId))
    } yield (ContentComponents.apply _).tupled(contentTuple)

  def update(id: Int, viewId: Int, payload: UpdateContentPayload, kind: String)(
      implicit ec: EC,
      fmt: Formats): DbResultT[Content] =
    for {
      _        ← * <~ payload.validate
      existing ← * <~ mustFindLatest(id, viewId, kind)
      _        ← * <~ validateNoCycles(existing.commit.id, kind, payload.relations)
      newAttrs ← * <~ ContentUtils.encodeContentAttributesForUpdate(existing.form.attributes,
                                                                    existing.shadow.attributes,
                                                                    payload.attributes)
      (newFormAttrs, newShadowAttrs) = newAttrs
      existingRelations              = ContentUtils.buildRelations(existing.shadow.relations)
      relations                      = ContentUtils.updateRelations(existingRelations, payload.relations)
      _ ← * <~ validateRelations(relations)

      updatedForm ← * <~ Forms.update(existing.form,
                                      existing.form.copy(attributes = newFormAttrs, updatedAt = Instant.now))
      newShadow ← * <~ Shadows.create(Shadow.build(updatedForm.id, newShadowAttrs, relations))
      newCommit ← * <~ Commits.create(Commit(formId = existing.form.id, shadowId = existing.shadow.id))
      updatedHead ← * <~ Heads.update(existing.head,
                                      existing.head.copy(commitId = newCommit.id, updatedAt = Instant.now))
      updated ← * <~ Content.build(updatedHead, newCommit, updatedForm, newShadow)

      _ ← * <~ updateParents(existing.commit.id, newCommit.id, existing.head.kind)
    } yield updated

  private def updateParents(oldCommitId: Commit#Id, newCommitId: Commit#Id, kind: String)(
      implicit ec: EC,
      fmt: Formats): DbResultT[Seq[Int]] = {
    for {
      parentIds ← * <~ ContentQueries.filterParentIds(oldCommitId, kind)
      newParentIds ← * <~ parentIds.map {
                      case (parentId, parentKind) ⇒
                        updateParent(parentId, parentKind, oldCommitId, newCommitId, kind)
                    }
    } yield newParentIds
  }

  private def updateParent(parentId: Commit#Id,
                           parentKind: String,
                           oldChildId: Commit#Id,
                           newChildId: Commit#Id,
                           childKind: String)(implicit ec: EC, fmt: Formats) =
    for {
      parentTuple ← * <~ ContentQueries
                     .filterByCommit(parentId, parentKind)
                     .mustFindOneOr(ObjectNotFoundAtCommit(parentKind, parentId))

      (commit, form, shadow) = parentTuple
      oldRelations           = ContentUtils.buildRelations(shadow.relations)
      newRelations           = updateRelations(oldRelations, childKind, oldChildId, newChildId)

      newShadow ← * <~ Shadows.create(Shadow.build(form.id, shadow.attributes, newRelations))
      newCommit ← * <~ Commits.create(Commit(formId = form.id, shadowId = newShadow.id))
      oldHead ← * <~ Heads
                 .filter(h ⇒ h.commitId === commit.id && h.archivedAt.isEmpty)
                 .result
                 .headOption

      _ ← * <~ maybeUpdateHead(oldHead, newCommit.id)
      _ ← * <~ updateParents(commit.id, newCommit.id, parentKind)
    } yield newCommit.id

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

  /**
    * validateNoCycles analyzes the proposed changes to a content object's
    * relations map and validates that no cycles get created in the content
    * object's network. There should be more efficient algorithms to do this,
    * but for now, this should work.
    */
  private def validateNoCycles(id: Commit#Id,
                               kind: String,
                               newRelations: Option[Content.ContentRelations])(implicit ec: EC) = {
    newRelations match {
      case Some(relations) ⇒
        for {
          parentIds ← * <~ collectParentIds(id, kind, Seq.empty[Commit#Id])
          _         ← * <~ validateIdsNotInRelations(id, kind, relations, parentIds.toSet)
        } yield {}
      case None ⇒
        DbResultT.pure(Unit)
    }
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

  private def updateRelations(relations: Content.ContentRelations,
                              kind: String,
                              oldRelation: Int,
                              newRelation: Int): Content.ContentRelations = {
    relations.get(kind).foldLeft(relations) { (acc, commits) ⇒
      val updated = commits.foldLeft(Seq.empty[Commit#Id]) { (acc, commit) ⇒
        if (commit == oldRelation) acc :+ newRelation
        else acc :+ oldRelation
      }

      acc + (kind → updated)
    }
  }

  type FullContentRelations = Map[String, Seq[Content]]
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

  private def validateRelations(relations: Content.ContentRelations)(implicit ec: EC): Seq[DbResultT[Unit]] =
    relations.foldLeft(Seq.empty[DbResultT[Unit]]) {
      case (acc, (kind, expectedIds)) ⇒
        acc :+ (for {
          actualIds ← * <~ ContentQueries.filterCommitIds(kind, expectedIds).result
          _         ← * <~ validateAllCommits(kind, expectedIds, actualIds)
        } yield {})
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
      case Nil          ⇒ DbResultT.pure(Unit)
    }
}
