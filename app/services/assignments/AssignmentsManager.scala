package services.assignments

import cats.implicits._
import models.Assignment._
import models.{StoreAdmins, StoreAdmin, Assignment, Assignments, NotificationSubscription}
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import responses.{ResponseItem, BatchMetadataSource, BatchMetadata, TheResponse}
import responses.AssignmentResponse.{build ⇒ buildAssignment, Root ⇒ Root}
import responses.StoreAdminResponse.{build ⇒ buildAdmin}
import responses.BatchMetadata._
import services.Util._
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.ModelWithIdParameter
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

trait AssignmentsManager[K, M <: ModelWithIdParameter[M]] {
  // Define this methods in inherit object
  def assignmentType(): AssignmentType
  def referenceType(): ReferenceType
  def notifyDimension(): String
  def buildResponse(model: M): ResponseItem

  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[M]
  def fetchSequence(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[M]]

  // Add additional ADT members here if necessary
  private def notifyReason(): NotificationSubscription.Reason = assignmentType() match {
    case Assignee ⇒ NotificationSubscription.Assigned
    case Watcher  ⇒ NotificationSubscription.Watching
  }

  // Use this methods wherever you want
  def assign(key: K, payload: AssignmentPayload, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[Seq[Root]]] = (for {
    // Validation + assign
    entity         ← * <~ fetchEntity(key)
    admins         ← * <~ StoreAdmins.filter(_.id.inSetBind(payload.assignees)).result
    adminIds       = admins.map(_.id)
    assignees      ← * <~ Assignments.assigneesFor(assignmentType(), entity, referenceType()).result.toXor
    newAssigneeIds = adminIds.diff(assignees.map(_.id))
    _              ← * <~ Assignments.createAll(build(entity, newAssigneeIds))
    assignedAdmins = admins.filter(a ⇒ newAssigneeIds.contains(a.id)).map(buildAdmin)
    // Response builder
    assignments    ← * <~ fetchAssignments(entity).toXor
    response       = assignments.map((buildAssignment _).tupled)
    notFoundAdmins = diffToFailures(payload.assignees, adminIds, StoreAdmin)
    // Activity log + notifications subscription
    _         ← * <~ LogActivity.assigned(originator, entity, assignedAdmins, assignmentType(), referenceType())
    _         ← * <~ subscribe(adminIds = assignedAdmins.map(_.id), objectIds = Seq(key.toString))
  } yield TheResponse.build(response, errors = notFoundAdmins)).runTxn()

  def unassign(key: K, assigneeId: Int, originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Seq[Root]] = (for {
    // Validation + unassign
    entity     ← * <~ fetchEntity(key)
    admin      ← * <~ StoreAdmins.mustFindById404(assigneeId)
    querySeq   = Assignments.byEntityAndAdmin(assignmentType(), entity, referenceType(), admin)
    assignment ← * <~ querySeq.one.mustFindOr(AssigneeNotFound(entity, key, assigneeId))
    _          ← * <~ querySeq.delete
    // Response builder
    assignments    ← * <~ fetchAssignments(entity).toXor
    response       = assignments.map((buildAssignment _).tupled)
    // Activity log + notifications subscription
    _         ← * <~ LogActivity.unassigned(originator, entity, admin, assignmentType(), referenceType())
    _         ← * <~ unsubscribe(adminIds = Seq(assigneeId), objectIds = Seq(key.toString))
  } yield response).runTxn()

  def assignBulk(originator: StoreAdmin, payload: BulkAssignmentPayload[K])
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[Seq[ResponseItem]]] = (for {
    // Validation + assign
    admin          ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities       ← * <~ fetchSequence(payload.entityIds)
    assignments    ← * <~ Assignments.byAdmin(assignmentType(), referenceType(), admin).result.toXor
    newAssignedIds = entities.map(_.id).diff(assignments.map(_.referenceId))
    newEntries     = buildSeq(entities.filter(e ⇒ newAssignedIds.contains(e.id)), payload.storeAdminId)
    _              ← * <~ Assignments.createAll(newEntries)
    success        = filterSuccess(entities, newEntries)
    // LogActivity + notifications
    _              ← * <~ logBulkAssign(originator, admin, success)
    _              ← * <~ subscribe(Seq(admin.id), success)
    // Batch response builder
    result         = entities.map(buildResponse)
    entityName     = entities.headOption.getOrElse(Some)
    batchFailures  = diffToBatchErrors(payload.entityIds, success, entityName)
    batchMetadata  = BatchMetadata(BatchMetadataSource(entityName, success, batchFailures))
  } yield TheResponse(result, errors = flattenErrors(batchFailures), batch = batchMetadata.some)).runTxn()

  def unassignBulk(originator: StoreAdmin, payload: BulkAssignmentPayload[K])
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[Seq[ResponseItem]]] = (for {
    // Validation + unassign
    admin         ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities      ← * <~ fetchSequence(payload.entityIds)
    querySeq      = Assignments.byEntitySeqAndAdmin(assignmentType(), entities, referenceType(), admin)
    assignments   ← * <~ querySeq.result.toXor
    _             ← * <~ querySeq.delete
    success       = filterSuccess(entities, assignments)
    // LogActivity + notifications
    _              ← * <~ logBulkUnassign(originator, admin, success)
    _              ← * <~ unsubscribe(Seq(admin.id), success)
    // Batch response builder
    result        = entities.map(buildResponse)
    entityName    = entities.headOption.getOrElse(Some)
    batchFailures = diffToBatchErrors(payload.entityIds, success, entityName)
    batchMetadata = BatchMetadata(BatchMetadataSource(entityName, success, batchFailures))
  } yield TheResponse(result, errors = flattenErrors(batchFailures), batch = batchMetadata.some)).runTxn()

  // Activities
  private def logBulkAssign(originator: StoreAdmin, admin: StoreAdmin, keys: Seq[String])
    (implicit ec: EC, db: DB, ac: AC) = {
    if (keys.nonEmpty)
      LogActivity.bulkAssigned(originator, admin, keys, assignmentType(), referenceType())
    else
      DbResult.unit
  }

  private def logBulkUnassign(originator: StoreAdmin, admin: StoreAdmin, keys: Seq[String])
    (implicit ec: EC, db: DB, ac: AC) = {
    if (keys.nonEmpty)
      LogActivity.bulkUnassigned(originator, admin, keys, assignmentType(), referenceType())
    else
      DbResult.unit
  }

  // Notifications
  private def subscribe(adminIds: Seq[Int], objectIds: Seq[String])(implicit ec: EC, db: DB) = {
    if (objectIds.nonEmpty)
      NotificationManager.subscribe(adminIds = adminIds, dimension = notifyDimension(), reason = notifyReason(),
        objectIds = objectIds)
    else
      DbResult.unit
  }

  private def unsubscribe(adminIds: Seq[Int], objectIds: Seq[String])(implicit ec: EC, db: DB) =
    if (objectIds.nonEmpty)
      NotificationManager.unsubscribe(adminIds = adminIds, dimension = notifyDimension(), reason = notifyReason(),
        objectIds = objectIds)
    else
      DbResult.unit

  // Helpers
  private def build(entity: M, newAssigneeIds: Seq[Int]): Seq[Assignment] =
    newAssigneeIds.map(adminId ⇒ Assignment(assignmentType = assignmentType(),
      storeAdminId = adminId, referenceType = referenceType(), referenceId = entity.id))

  private def buildSeq(entities: Seq[M], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities) yield Assignment(assignmentType = assignmentType(), storeAdminId = storeAdminId,
      referenceType = referenceType(), referenceId = e.id)

  private def fetchAssignments(entity: M)(implicit ec: EC, db: DB) = for {
    assignments ← Assignments.byEntity(assignmentType(), entity, referenceType()).result
    admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.storeAdminId))).result
  } yield assignments.zip(admins)

  private def filterSuccess(entities: Seq[M], newEntries: Seq[Assignment]): Seq[String] =
    entities.filter(e ⇒ newEntries.map(_.referenceId).contains(e.id)).map(m ⇒ m.primarySearchKeyLens.get(m))
}