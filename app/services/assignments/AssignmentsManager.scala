package services.assignments

import models.Assignment._
import models.{StoreAdmins, StoreAdmin, Assignment, Assignments}
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import responses.{ResponseItem, BatchMetadataSource, BatchMetadata, TheResponse}
import services.Util._
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.{TableQueryWithId, ModelWithIdParameter}
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

trait AssignmentsManager[K, M <: ModelWithIdParameter[M]] {
  // Define this methods in inherit object
  //def modelInstance(): ModelWithIdParameter[M]
  //def tableInstance(): TableQueryWithId[M, T]
  //def responseBuilder(): M ⇒ ResponseItem
  def assignmentType(): AssignmentType
  def referenceType(): ReferenceType

  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[M]
  //def fetchMulti(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[M]]

  // Use this methods wherever you want
  def assign(key: K, payload: AssignmentPayload, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {

    entity    ← * <~ fetchEntity(key)
    adminIds  ← * <~ StoreAdmins.filter(_.id.inSetBind(payload.assignees)).map(_.id).result
    assignees ← * <~ Assignments.assigneesFor(assignmentType(), entity, referenceType()).result.toXor
    _         ← * <~ Assignments.createAll(buildNew(entity, adminIds, assignees))

    // TODO - TheResponse alternative with embedded assignments
    // ...

    notFoundAdmins  = diffToFailures(payload.assignees, adminIds, StoreAdmin)

    // TODO - LogActivity + notifications generalization
    // ...
  } yield ()).runTxn()

  def unassign(key: K, assigneeId: Int, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {

    entity     ← * <~ fetchEntity(key)
    admin      ← * <~ StoreAdmins.mustFindById404(assigneeId)
    querySeq   = Assignments.byEntityAndAdmin(assignmentType(), entity, referenceType(), admin)
    assignment ← * <~ querySeq.one.mustFindOr(AssigneeNotFound(entity, key, assigneeId))
    _          ← * <~ querySeq.delete

    // TODO - LogActivity + notifications generalization
    // ...
  } yield ()).runTxn()

  /*
  def assignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload)
    (implicit ec: EC, db: DB, ac: AC, sortAndPage: SortAndPage): Result[Unit] = (for {

    admin      ← * <~ StoreAdmins.mustFindById404(payload.assigneeId)
    entities   ← * <~ fetchMulti(payload.entityIds)
    newEntries = buildNewMulti(entities, payload.assigneeId)
    _          ← * <~ Assignments.createAll(newEntries)

    // TODO - .findAll generalized
    success   = filterSuccess(entities, newEntries)

    // TODO - LogActivity + notifications generalization
    // ...

    // TODO - Append proper class names
    batchFailures  = diffToBatchErrors(payload.entityIds, entities.map(_.id), modelInstance())
    batchMetadata  = BatchMetadata(BatchMetadataSource(modelInstance(), success.map(_.toString), batchFailures))
  } yield ()).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload)
    (implicit ec: EC, db: DB, ac: AC, sortAndPage: SortAndPage): Result[Unit] = (for {

    admin    ← * <~ StoreAdmins.mustFindById404(payload.assigneeId)
    entities ← * <~ fetchMulti(payload.entityIds)
    _        ← * <~ Assignments.filter(_.storeAdminId === payload.assigneeId)
      .filter(_.referenceType === referenceType()).filter(_.referenceId.inSetBind(entities.map(_.id))).delete

    // TODO - .findAll generalized
    success   = entities.filter(c ⇒ payload.entityIds.contains(c.id)).map(_.id)

    // TODO - LogActivity + notifications generalization
    // ...

    // Prepare batch response + proper class names
    batchFailures  = diffToBatchErrors(payload.entityIds, entities.map(_.id), modelInstance())
    batchMetadata  = BatchMetadata(BatchMetadataSource(modelInstance(), success.map(_.toString), batchFailures))
  } yield ()).runTxn()
  */

  // Helpers
  private def buildNew(entity: M, adminIds: Seq[Int], assignees: Seq[StoreAdmin]): Seq[Assignment] =
    adminIds.diff(assignees.map(_.id)).map(adminId ⇒ Assignment(assignmentType = assignmentType(), storeAdminId = adminId,
      referenceType = referenceType(), referenceId = entity.id))

  private def buildNewMulti(entities: Seq[M], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities) yield Assignment(assignmentType = assignmentType(), storeAdminId = storeAdminId,
      referenceType = referenceType(), referenceId = e.id)

  private def filterSuccess(entities: Seq[M], newEntries: Seq[Assignment]): Seq[M#Id] =
    entities.filter(e ⇒ newEntries.map(_.referenceId).contains(e.id)).map(_.id)
}