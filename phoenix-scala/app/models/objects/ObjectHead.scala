package models.objects

import java.time.Instant

import failures.ObjectFailures.{ObjectHeadCannotBeFoundByFormId, ObjectHeadCannotBeFoundForContext}
import slick.lifted.Tag
import utils.aliases.{EC, OC}
import utils.db.ExPostgresDriver.api._
import utils.db._

import com.github.tminglei.slickpg._

trait ObjectHead[M <: ObjectHead[M]] extends FoxModel[M] { self: M ⇒
  def scope: LTree
  def contextId: Int
  def shadowId: Int
  def formId: Int
  def commitId: Int
  def updatedAt: Instant
  def createdAt: Instant
  def archivedAt: Option[Instant]

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): M
}

/**
  * Abstract class to help define an object head object which points to the latest
  * version of some object in the context specified.
  */
abstract class ObjectHeads[M <: ObjectHead[M]](tag: Tag, table: String)
    extends FoxTable[M](tag, table) {

  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope      = column[LTree]("scope")
  def contextId  = column[Int]("context_id")
  def shadowId   = column[Int]("shadow_id")
  def formId     = column[Int]("form_id")
  def commitId   = column[Int]("commit_id")
  def updatedAt  = column[Instant]("updated_at")
  def createdAt  = column[Instant]("created_at")
  def archivedAt = column[Option[Instant]]("archived_at")

  def context = foreignKey(ObjectContexts.tableName, contextId, ObjectContexts)(_.id)
  def shadow  = foreignKey(ObjectShadows.tableName, shadowId, ObjectShadows)(_.id)
  def form    = foreignKey(ObjectForms.tableName, formId, ObjectForms)(_.id)
  def commit  = foreignKey(ObjectCommits.tableName, commitId, ObjectCommits)(_.id)
}

abstract class ObjectHeadsQueries[M <: ObjectHead[M], T <: ObjectHeads[M]](construct: Tag ⇒ T)
    extends FoxTableQuery[M, T](construct) {

  def mustFindByFormId404(formId: ObjectForm#Id)(implicit oc: OC, ec: EC): DbResultT[M] =
    findOneByFormId(formId).mustFindOneOr(
        ObjectHeadCannotBeFoundByFormId(baseTableRow.tableName, formId, oc.name))

  def mustFindByContextAndFormId404(contextId: Int, formId: Int)(implicit ec: EC): DbResultT[M] =
    findOneByContextAndFormId(contextId, formId).mustFindOneOr(
        ObjectHeadCannotBeFoundForContext(baseTableRow.tableName, formId, contextId))

  def findOneByFormId(formId: Int)(implicit oc: OC): QuerySeq =
    filter(_.contextId === oc.id).filter(_.formId === formId)

  def findOneByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)

  def findOneByContextAndShadowId(contextId: Int, shadowId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.shadowId === shadowId)

  def updateHead(fullObject: FullObject[M], commitId: Int)(
      implicit ec: EC): DbResultT[FullObject[M]] =
    update(fullObject.model,
           fullObject.model.withNewShadowAndCommit(fullObject.shadow.id, commitId)).map(updated ⇒
          fullObject.copy(model = updated))
}
