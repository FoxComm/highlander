package services.assignments

import cats.implicits._
import models.Assignment._
import models.{StoreAdmins, StoreAdmin, Assignment, Assignments, NotificationSubscription}
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import responses.{StoreAdminResponse, ResponseItem, BatchMetadataSource, BatchMetadata, AssignmentResponse, TheResponse}
import responses.AssignmentResponse.{build ⇒ buildAssignment, Root ⇒ Root}
import responses.StoreAdminResponse.{build ⇒ buildAdmin}
import responses.BatchMetadata._
import services.Util._
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.{ModelWithIdParameter, TableQueryWithId}
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
  def notifyDimension(): String

  private def notifyReason(): NotificationSubscription.Reason = assignmentType() match {
    case Assignee ⇒ NotificationSubscription.Assigned
    case Watcher  ⇒ NotificationSubscription.Watching
  }

  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[M]
  def fetchSequence(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[M]]

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
    _         ← * <~ LogActivity.assigned(originator, entity, assignedAdmins)
    _         ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id), dimension = notifyDimension(),
      reason = notifyReason(), objectIds = Seq(key.toString))
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
    _         ← * <~ LogActivity.unassigned(originator, entity, admin)
    _         ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId), dimension = notifyDimension(),
      reason = notifyReason(), objectIds = Seq(key.toString))
  } yield response).runTxn()

  def assignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload[K])
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[Seq[M]]] = (for {
    // Validation + assign
    admin          ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities       ← * <~ fetchSequence(payload.entityIds)
    assignments    ← * <~ Assignments.filter(_.referenceType === referenceType())
      .filter(_.assignmentType === assignmentType())
      .filter(_.storeAdminId === payload.storeAdminId).result.toXor
    newAssignedIds = entities.map(_.id).diff(assignments.map(_.referenceId))
    newEntries     = buildSeq(entities.filter(e ⇒ newAssignedIds.contains(e.id)), payload.storeAdminId)
    _              ← * <~ Assignments.createAll(newEntries)
    success        = filterSuccess(entities, newEntries)
    // TODO - LogActivity + notifications generalization
    // ...
    // Batch response builder
    entityName    = entities.headOption.getOrElse(Some)
    batchFailures = diffToBatchErrors(payload.entityIds, newEntries.map(_.referenceId), entityName)
    batchMetadata = BatchMetadata(BatchMetadataSource(entityName, success.map(_.toString), batchFailures))
  } yield TheResponse(entities, errors = flattenErrors(batchFailures), batch = batchMetadata.some)).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: BulkAssignmentPayload[K])
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[Seq[M]]] = (for {
    // Validation + unassign
    admin         ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities      ← * <~ fetchSequence(payload.entityIds)
    _             ← * <~ Assignments.byEntitySeqAndAdmin(assignmentType(), entities, referenceType(), admin).delete
    success       = entities.filter(c ⇒ payload.entityIds.contains(c.id)).map(_.id)
    // TODO - LogActivity + notifications generalization
    // ...
    // Batch response builder
    entityName    = entities.headOption.getOrElse(Some)
    batchFailures = diffToBatchErrors(payload.entityIds, entities.map(_.id), entityName)
    batchMetadata = BatchMetadata(BatchMetadataSource(entityName, success.map(_.toString), batchFailures))
  } yield TheResponse(entities, errors = flattenErrors(batchFailures), batch = batchMetadata.some)).runTxn()

  // Helpers
  private def fetchAssignments(entity: M)(implicit ec: EC, db: DB) = {
    for {
      assignments ← Assignments.byEntity(assignmentType(), entity, referenceType()).result
      admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.storeAdminId))).result
    } yield assignments.zip(admins)
  }

  private def build(entity: M, newAssigneeIds: Seq[Int]): Seq[Assignment] =
    newAssigneeIds.map(adminId ⇒ Assignment(assignmentType = assignmentType(),
      storeAdminId = adminId, referenceType = referenceType(), referenceId = entity.id))

  private def buildSeq(entities: Seq[M], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities) yield Assignment(assignmentType = assignmentType(), storeAdminId = storeAdminId,
      referenceType = referenceType(), referenceId = e.id)

  private def filterSuccess(entities: Seq[M], newEntries: Seq[Assignment]): Seq[M#Id] =
    entities.filter(e ⇒ newEntries.map(_.referenceId).contains(e.id)).map(_.id)
}