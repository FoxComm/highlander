package services.assignments

import akka.http.scaladsl.server.Directives._

import cats.implicits._
import failures.{AlreadyAssignedFailure, AssigneeNotFoundFailure, NotAssignedFailure, NotFoundFailure404}
import failures.Util._
import models.Assignment._
import models.{Assignment, Assignments, NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.AssignmentPayloads._
import responses.{BatchMetadata, BatchMetadataSource, ResponseItem, TheResponse}
import responses.AssignmentResponse.{Root, build ⇒ buildAssignment}
import responses.StoreAdminResponse.{build ⇒ buildAdmin}
import responses.BatchMetadata._
import services._
import slick.driver.PostgresDriver.api._
import utils.http.CustomDirectives._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

trait AssignmentsManager[K, M <: FoxModel[M]] {
  // Assign / unassign
  sealed trait ActionType
  case object Assigning   extends ActionType
  case object Unassigning extends ActionType

  // For batch metadata
  sealed trait GroupingType
  case object Skipped extends GroupingType
  case object Succeed extends GroupingType

  case class EntityTrio(succeed: Seq[M], skipped: Seq[M], notFound: Seq[String])

  // Database helpers
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[M]
  def fetchSequence(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[M]]
  def buildResponse(model: M): ResponseItem

  // Metadata helpers
  val assignmentType: AssignmentType
  val referenceType: ReferenceType
  val notifyDimension: String
  val notifyReason: NotificationSubscription.Reason

  // Use methods below in your endpoints
  def list(key: K)(implicit ec: EC, db: DB, ac: AC): Result[Seq[Root]] =
    (for {
      entity      ← * <~ fetchEntity(key)
      assignments ← * <~ fetchAssignments(entity).toXor
      response = assignments.map((buildAssignment _).tupled)
    } yield response).run()

  def assign(key: K, payload: AssignmentPayload, originator: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[TheResponse[Seq[Root]]] =
    (for {
      // Validation + assign
      entity ← * <~ fetchEntity(key)
      admins ← * <~ StoreAdmins.filter(_.id.inSetBind(payload.assignees)).result
      adminIds = admins.map(_.id)
      assignees ← * <~ Assignments.assigneesFor(assignmentType, entity, referenceType).result.toXor
      newAssigneeIds = adminIds.diff(assignees.map(_.id))
      _ ← * <~ Assignments.createAll(build(entity, newAssigneeIds))
      assignedAdmins = admins.filter(a ⇒ newAssigneeIds.contains(a.id)).map(buildAdmin)
      // Response builder
      assignments ← * <~ fetchAssignments(entity).toXor
      response       = assignments.map((buildAssignment _).tupled)
      notFoundAdmins = diffToFailures(payload.assignees, adminIds, StoreAdmin)
      // Activity log + notifications subscription
      _ ← * <~ subscribe(this, assignedAdmins.map(_.id), Seq(key.toString))
      responseItem = buildResponse(entity)
      _ ← * <~ LogActivity
           .assigned(originator, responseItem, assignedAdmins, assignmentType, referenceType)
    } yield TheResponse.build(response, errors = notFoundAdmins)).runTxn()

  def unassign(key: K, assigneeId: Int, originator: StoreAdmin)(implicit ec: EC,
                                                                db: DB,
                                                                ac: AC): Result[Seq[Root]] =
    (for {
      // Validation + unassign
      entity ← * <~ fetchEntity(key)
      admin  ← * <~ StoreAdmins.mustFindById404(assigneeId)
      querySeq = Assignments.byEntityAndAdmin(assignmentType, entity, referenceType, admin)
      assignment ← * <~ querySeq.mustFindOneOr(AssigneeNotFoundFailure(entity, key, assigneeId))
      _          ← * <~ querySeq.delete
      // Response builder
      assignments ← * <~ fetchAssignments(entity).toXor
      response = assignments.map((buildAssignment _).tupled)
      // Activity log + notifications subscription
      responseItem = buildResponse(entity)
      _ ← * <~ LogActivity
           .unassigned(originator, responseItem, admin, assignmentType, referenceType)
      _ ← * <~ unsubscribe(this, adminIds = Seq(assigneeId), objectIds = Seq(key.toString))
    } yield response).runTxn()

  def assignBulk(originator: StoreAdmin, payload: BulkAssignmentPayload[K])(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[TheResponse[Seq[ResponseItem]]] =
    (for {
      // Validation + assign
      admin       ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
      entities    ← * <~ fetchSequence(payload.entityIds)
      assignments ← * <~ Assignments.byAdmin(assignmentType, referenceType, admin).result.toXor
      newAssignedIds  = entities.map(_.id).diff(assignments.map(_.referenceId))
      succeedEntities = entities.filter(e ⇒ newAssignedIds.contains(e.id))
      newEntries      = buildSeq(succeedEntities, payload.storeAdminId)
      _ ← * <~ Assignments.createAll(newEntries)
      // Response, log activity, notifications subscription
      (successData, theResponse) = buildTheResponse(entities, assignments, payload, Assigning)
      _ ← * <~ subscribe(this, Seq(admin.id), successData)
      _ ← * <~ logBulkAssign(this, originator, admin, successData)
    } yield theResponse).runTxn()

  def unassignBulk(originator: StoreAdmin, payload: BulkAssignmentPayload[K])(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[TheResponse[Seq[ResponseItem]]] =
    (for {
      // Validation + unassign
      admin    ← * <~ StoreAdmins.mustFindById404(payload.storeAdminId)
      entities ← * <~ fetchSequence(payload.entityIds)
      querySeq = Assignments.byEntitySeqAndAdmin(assignmentType, entities, referenceType, admin)
      assignments ← * <~ querySeq.result.toXor
      _           ← * <~ querySeq.delete
      // Response, log activity, notifications subscription
      (successData, theResponse) = buildTheResponse(entities, assignments, payload, Unassigning)
      _ ← * <~ logBulkUnassign(this, originator, admin, successData)
      _ ← * <~ unsubscribe(this, Seq(admin.id), successData)
    } yield theResponse).runTxn()

  private def buildTheResponse(entities: Seq[M],
                               assignments: Seq[Assignment],
                               payload: BulkAssignmentPayload[K],
                               actionType: ActionType) = {

    val result        = entities.map(buildResponse)
    val entityTrio    = groupEntities(entities, assignments, payload, actionType)
    val successData   = getSuccessData(entityTrio)
    val failureData   = getFailureData(entityTrio, payload.storeAdminId, actionType)
    val batchMetadata = BatchMetadata(BatchMetadataSource(referenceType, successData, failureData))

    (successData,
     TheResponse(result, errors = flattenErrors(failureData), batch = batchMetadata.some))
  }

  // Result builders
  private def build(entity: M, newAssigneeIds: Seq[Int]): Seq[Assignment] =
    newAssigneeIds.map(
        adminId ⇒
          Assignment(assignmentType = assignmentType,
                     storeAdminId = adminId,
                     referenceType = referenceType,
                     referenceId = entity.id))

  private def buildSeq(entities: Seq[M], storeAdminId: Int): Seq[Assignment] =
    for (e ← entities)
      yield
        Assignment(assignmentType = assignmentType,
                   storeAdminId = storeAdminId,
                   referenceType = referenceType,
                   referenceId = e.id)

  // Batch metadata builders
  private def getSuccessData(trio: EntityTrio): SuccessData = searchKeys(trio.succeed)

  private def getFailureData(trio: EntityTrio,
                             storeAdminId: Int,
                             actionType: ActionType): FailureData = {

    val notFoundFailures = trio.notFound.map { key ⇒
      (key, NotFoundFailure404(referenceType, key).description)
    }

    val skippedFailures = trio.skipped.map { e ⇒
      val searchKey = e.primarySearchKey
      actionType match {
        case Assigning ⇒
          (searchKey, AlreadyAssignedFailure(referenceType, searchKey, storeAdminId).description)
        case Unassigning ⇒
          (searchKey, NotAssignedFailure(referenceType, searchKey, storeAdminId).description)
      }
    }

    (notFoundFailures ++ skippedFailures).toMap
  }

  private def groupEntities(entities: Seq[M],
                            assignments: Seq[Assignment],
                            payload: BulkAssignmentPayload[K],
                            actionType: ActionType): EntityTrio = {

    val succeed  = groupInner(entities, assignments, actionType, Succeed)
    val skipped  = groupInner(entities, assignments, actionType, Skipped)
    val notFound = payload.entityIds.map(_.toString).diff(searchKeys(entities))

    EntityTrio(succeed, skipped, notFound)
  }

  private def groupInner(entities: Seq[M],
                         assignments: Seq[Assignment],
                         actionType: ActionType,
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
        entities.filterNot(e ⇒ successIds.contains(e.id))
    }
  }

  private def fetchAssignments(entity: M)(implicit ec: EC, db: DB) =
    for {
      assignments ← Assignments.byEntity(assignmentType, entity, referenceType).result
      admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.storeAdminId))).result
    } yield assignments.zip(admins)

  private def searchKeys(entities: Seq[M]): Seq[String] = entities.map(_.primarySearchKey)
  private def searchKey(entity: M)                      = entity.primarySearchKey
}
