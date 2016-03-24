package services.assignments

import scala.reflect.ClassTag
import scala.reflect._

import cats.implicits._
import failures.{AlreadyAssignedFailure, AssigneeNotFound, NotAssignedFailure, NotFoundFailure404}
import failures.Util._
import models.Assignment._
import models.{Assignment, Assignments, NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import responses.{BatchMetadata, BatchMetadataSource, ResponseItem, TheResponse}
import responses.AssignmentResponse.{Root, build ⇒ buildAssignment}
import responses.StoreAdminResponse.{build ⇒ buildAdmin}
import responses.BatchMetadata._
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.ModelWithIdParameter
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

trait AssignmentsManager[K, M <: ModelWithIdParameter[M]] {
  // Assign / unassign
  sealed trait ActionType
  case object Assigning extends ActionType
  case object Unassigning extends ActionType

  // For batch metadata
  sealed trait GroupingType
  case object Skipped extends GroupingType
  case object Succeed extends GroupingType

  // Add additional ADT members here if necessary
  private def notifyReason(): NotificationSubscription.Reason = assignmentType() match {
    case Assignee ⇒ NotificationSubscription.Assigned
    case Watcher  ⇒ NotificationSubscription.Watching
  }

  case class EntityTrio(succeed: Seq[M], skipped: Seq[M], notFound: Seq[String])

  // Database helpers
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[M]
  def fetchSequence(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[M]]

  private def fetchAssignments(entity: M)(implicit ec: EC, db: DB) = for {
    assignments ← Assignments.byEntity(assignmentType(), entity, referenceType()).result
    admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.storeAdminId))).result
  } yield assignments.zip(admins)

  // Define this methods in inherit object
  def assignmentType(): AssignmentType
  def referenceType(): ReferenceType
  def notifyDimension(): String
  def buildResponse(model: M): ResponseItem

  // Use this methods wherever you want
  def list(key: K)(implicit ec: EC, db: DB, ac: AC): Result[Seq[Root]] = (for {
    entity      ← * <~ fetchEntity(key)
    assignments ← * <~ fetchAssignments(entity).toXor
    response    = assignments.map((buildAssignment _).tupled)
  } yield response).run()

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
    admin           ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities        ← * <~ fetchSequence(payload.entityIds)
    assignments     ← * <~ Assignments.byAdmin(assignmentType(), referenceType(), admin).result.toXor
    newAssignedIds  = entities.map(_.id).diff(assignments.map(_.referenceId))
    succeedEntities = entities.filter(e ⇒ newAssignedIds.contains(e.id))
    newEntries      = buildSeq(succeedEntities, payload.storeAdminId)
    _               ← * <~ Assignments.createAll(newEntries)
    // Response, log activity, notifications subscription
    (successData, theResponse) = buildTheResponse(entities, assignments, payload, Assigning)
    _               ← * <~ logBulkAssign(originator, admin, successData)
    _               ← * <~ subscribe(Seq(admin.id), successData)
  } yield theResponse).runTxn()

  def unassignBulk(originator: StoreAdmin, payload: BulkAssignmentPayload[K])
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[Seq[ResponseItem]]] = (for {
    // Validation + unassign
    admin           ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
    entities        ← * <~ fetchSequence(payload.entityIds)
    querySeq        = Assignments.byEntitySeqAndAdmin(assignmentType(), entities, referenceType(), admin)
    assignments     ← * <~ querySeq.result.toXor
    _               ← * <~ querySeq.delete
    // Response, log activity, notifications subscription
    (successData, theResponse) = buildTheResponse(entities, assignments, payload, Unassigning)
    _               ← * <~ logBulkUnassign(originator, admin, successData)
    _               ← * <~ unsubscribe(Seq(admin.id), successData)
  } yield theResponse).runTxn()

  private def buildTheResponse(entities: Seq[M], assignments: Seq[Assignment], payload: BulkAssignmentPayload[K],
    actionType: ActionType) = {

    val result        = entities.map(buildResponse)
    val entityTrio    = groupEntities(entities, assignments, payload, actionType)
    val successData   = getSuccessData(entityTrio)
    val failureData   = getFailureData(entityTrio, entities, payload.storeAdminId, actionType)
    val batchMetadata = BatchMetadata(BatchMetadataSource(entities, successData, failureData))

    (successData, TheResponse(result, errors = flattenErrors(failureData), batch = batchMetadata.some))
  }

  // Result builders
  private def build(entity: M, newAssigneeIds: Seq[Int]): Seq[Assignment] =
    newAssigneeIds.map(adminId ⇒ Assignment(assignmentType = assignmentType(),
      storeAdminId = adminId, referenceType = referenceType(), referenceId = entity.id))

  private def buildSeq(entities: Seq[M], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities) yield Assignment(assignmentType = assignmentType(), storeAdminId = storeAdminId,
      referenceType = referenceType(), referenceId = e.id)

  // Batch metadata builders
  private def getSuccessData(trio: EntityTrio): SuccessData = searchKeys(trio.succeed)

  private def getFailureData(trio: EntityTrio, entities: Seq[M], storeAdminId: Int,
    actionType: ActionType): FailureData = {

    val entityExample = entities.head // FIXME

    val notFoundFailures = trio.notFound.map { key ⇒
      (key.toString, NotFoundFailure404(entityExample, key).description)
    }

    val skippedFailures = trio.skipped.map { e ⇒
      val searchKey = e.primarySearchKeyLens.get(e)
      actionType match {
        case Assigning ⇒
          (searchKey, AlreadyAssignedFailure(entityExample, searchKey, storeAdminId).description)
        case Unassigning ⇒
          (searchKey, NotAssignedFailure(entityExample, searchKey, storeAdminId).description)
      }
    }

    (notFoundFailures ++ skippedFailures).toMap
  }

  private def groupEntities(entities: Seq[M], assignments: Seq[Assignment], payload: BulkAssignmentPayload[K],
    actionType: ActionType): EntityTrio = {

    val succeed = groupInner(entities, assignments, actionType, Succeed)
    val skipped = groupInner(entities, assignments, actionType, Skipped)
    val notFound = payload.entityIds.map(_.toString).diff(searchKeys(entities))

    EntityTrio(succeed, skipped, notFound)
  }

  private def groupInner(entities: Seq[M], assignments: Seq[Assignment], actionType: ActionType,
    groupType: GroupingType) = {

    val requestedEntityIds = entities.map(_.id)
    val assignedEntityIds  = assignments.map(_.referenceId)

    (actionType, groupType) match {
      case (Assigning, Succeed) ⇒
        val successIds = requestedEntityIds.diff(assignedEntityIds)
        entities.filter(e ⇒ successIds.contains(e.id))
      case (Assigning, Skipped) ⇒
        val successIds = requestedEntityIds.diff(assignedEntityIds)
        entities.filterNot(e ⇒ successIds.contains(e.id))
      case (Unassigning, Succeed) ⇒
        val successIds = requestedEntityIds.intersect(assignedEntityIds)
        entities.filter(e ⇒ successIds.contains(e.id))
      case (Unassigning, Skipped) ⇒
        val successIds = requestedEntityIds.intersect(assignedEntityIds)
        entities.filter(e ⇒ successIds.contains(e.id))
    }
  }

  private def searchKeys(entities: Seq[M]): Seq[String] = entities.map(searchKey)
  private def searchKey(entity: M) = entity.primarySearchKeyLens.get(entity)

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
}
