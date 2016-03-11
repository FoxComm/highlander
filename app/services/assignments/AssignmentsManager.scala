package services.assignments

import models.customer.{Customer, Customers}
import models.{StoreAdmins, StoreAdmin, Assignment, Assignments}
import responses.{BatchMetadataSource, BatchMetadata, TheResponse}
import services.Util._
import services._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.ModelWithIdParameter
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

trait AssignmentsManager[K, T <: ModelWithIdParameter[T]] {
  // Define this methods in inherit object
  def assignmentType(): Assignment.AssignmentType
  def referenceType(): Assignment.ReferenceType
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[T]
  def fetchMulti(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[T]]

  final case class AssignmentPayload(assignees: Seq[Int])
  final case class BulkAssignmentPayload(entityIds: Seq[K], assigneeId: Int)

  // Use this methods wherever you want
  def assign(key: K, assigneeIds: Seq[Int], originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {

    entity    ← * <~ fetchEntity(key)
    adminIds  ← * <~ StoreAdmins.filter(_.id.inSetBind(assigneeIds)).map(_.id).result
    assignees ← * <~ Assignments.assigneesFor(entity).result.toXor
    _         ← * <~ Assignments.createAll(buildNew(entity, adminIds, assignees))

    // TODO - TheResponse alternative with embedded assignments
    // ...

    notFoundAdmins  = diffToFailures(assigneeIds, adminIds, StoreAdmin)

    // TODO - LogActivity + notifications generalization
    // ...
  } yield ()).runTxn()

  def unassign(key: K, assigneeId: Int, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {

    entity     ← * <~ fetchEntity(key)
    admin      ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment ← * <~ byEntityAndAdmin(entity, admin).one.mustFindOr(AssigneeNotFound(entity, key, assigneeId))
    _          ← * <~ byEntityAndAdmin(entity, admin).delete

    // TODO - LogActivity + notifications generalization
    // ...
  } yield ()).runTxn()

  def assignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload)
    (implicit ec: EC, db: DB, ac: AC, sortAndPage: SortAndPage): Result[Unit] = (for {

    admin      ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities   ← * <~ fetchMulti(payload.entityIds)
    newEntries = buildNewMulti(entities, payload.storeAdminId)
    _          ← * <~ Assignments.createAll(newEntries)

    // TODO - .findAll generalized
    success   = filterSuccess(entities, newEntries)

    // TODO - LogActivity + notifications generalization
    // ...

    // TODO - Append proper class names
    batchFailures  = diffToBatchErrors(payload.entityIds, entities.map(_.id), Customers)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Customer, success.map(_.toString), batchFailures))
  } yield ()).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload)
    (implicit ec: EC, db: DB, ac: AC, sortAndPage: SortAndPage): Result[Unit] = (for {

    admin    ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities ← * <~ fetchMulti(payload.entityIds)
    _        ← * <~ Assignments.filter(_.storeAdminId === payload.storeAdminId)
      .filter(_.referenceType === referenceType()).filter(_.referenceId.inSetBind(entities.map(_.id))).delete

    // TODO - .findAll generalized
    success   = entities.filter(c ⇒ payload.entityIds.contains(c.id)).map(_.id)

    // TODO - LogActivity + notifications generalization
    // ...

    // Prepare batch response + proper class names
    batchFailures  = diffToBatchErrors(payload.entityIds, entities.map(_.id), Customer)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Customer, success.map(_.toString), batchFailures))
  } yield ()).runTxn()

  // Helpers
  private def byEntityAndAdmin(entity: T, admin: StoreAdmin): Assignments.QuerySeq =
    Assignments.byEntityAndAdmin(entity, referenceType(), admin)

  private def buildNew(entity: T, adminIds: Seq[Int], assignees: Seq[StoreAdmin]): Seq[Assignment] =
    adminIds.diff(assignees.map(_.id)).map(adminId ⇒ Assignment(assignmentType = assignmentType(),
      storeAdminId = adminId, referenceType = referenceType(), referenceId = entity.id))

  private def buildNewMulti(entities: Seq[T], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities) yield Assignment(assignmentType = assignmentType(),
      storeAdminId = storeAdminId, referenceType = referenceType(), referenceId = e.id)

  private def filterSuccess(entities: Seq[T], newEntries: Seq[Assignment]): Seq[T#Id] =
    entities.filter(e ⇒ newEntries.map(_.referenceId).contains(e.id)).map(_.id)
}