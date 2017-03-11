package services.assignments

import cats.implicits._
import failures.AssignmentFailures._
import failures.NotFoundFailure404
import failures.Util._
import models.Assignment._
import models._
import models.account._
import payloads.AssignmentPayloads._
import responses.AssignmentResponse.{Root, build ⇒ buildAssignment}
import responses.BatchMetadata._
import responses.UserResponse.{build ⇒ buildUser}
import responses._
import services._
import slick.driver.PostgresDriver.api._
import utils.FoxConfig.config
import utils.aliases._
import utils.db._

trait AssignmentsManager[K, M <: FoxModel[M]] {
  val defaultContextId: Int = config.app.defaultContextId

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
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResultT[M]
  def fetchSequence(keys: Seq[K])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[M]]
  def buildResponse(model: M): ResponseItem

  // Metadata helpers
  val assignmentType: AssignmentType
  val referenceType: ReferenceType
  val notifyDimension: String
  val notifyReason: NotificationSubscription.Reason

  // Use methods below in your endpoints
  def list(key: K)(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Root]] =
    for {
      entity      ← * <~ fetchEntity(key)
      assignments ← * <~ fetchAssignments(entity)
      response = assignments.map((buildAssignment _).tupled)
    } yield response

  def assign(key: K, payload: AssignmentPayload, originator: User)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[TheResponse[Seq[Root]]] =
    for {
      // Validation + assign
      entity ← * <~ fetchEntity(key)
      admins ← * <~ Users.filter(_.accountId.inSetBind(payload.assignees)).result
      adminIds = admins.map(_.id)
      assignees ← * <~ Assignments.assigneesFor(assignmentType, entity, referenceType).result
      newAssigneeIds = adminIds.diff(assignees.map(_.id))
      _ ← * <~ Assignments.createAll(build(entity, newAssigneeIds))
      assignedAdmins = admins.filter(a ⇒ newAssigneeIds.contains(a.id)).map(buildUser)
      // Response builder
      assignments ← * <~ fetchAssignments(entity)
      response       = assignments.map((buildAssignment _).tupled)
      notFoundAdmins = diffToFailures(payload.assignees, adminIds, User)
      // Activity log + notifications subscription
      _ ← * <~ subscribe(this, assignedAdmins.map(_.id), Seq(key.toString))
      responseItem = buildResponse(entity)
      _ ← * <~ LogActivity
           .assigned(originator, responseItem, assignedAdmins, assignmentType, referenceType)
    } yield TheResponse.build(response, errors = notFoundAdmins)

  def unassign(key: K, assigneeId: Int, originator: User)(implicit ec: EC,
                                                          db: DB,
                                                          ac: AC): DbResultT[Seq[Root]] =
    for {
      // Validation + unassign
      entity ← * <~ fetchEntity(key)
      admin  ← * <~ Users.mustFindByAccountId(assigneeId)
      querySeq = Assignments.byEntityAndAdmin(assignmentType, entity, referenceType, admin)
      assignment ← * <~ querySeq.mustFindOneOr(AssigneeNotFoundFailure(entity, key, assigneeId))
      _          ← * <~ querySeq.delete
      // Response builder
      assignments ← * <~ fetchAssignments(entity)
      response = assignments.map((buildAssignment _).tupled)
      // Activity log + notifications subscription
      responseItem = buildResponse(entity)
      _ ← * <~ LogActivity
           .unassigned(originator, responseItem, admin, assignmentType, referenceType)
      _ ← * <~ unsubscribe(this, adminIds = Seq(assigneeId), objectIds = Seq(key.toString))
    } yield response

  def assignBulk(originator: User, payload: BulkAssignmentPayload[K])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[TheResponse[Seq[ResponseItem]]] =
    for {
      // Validation + assign
      admin       ← * <~ Users.mustFindByAccountId(payload.storeAdminId)
      entities    ← * <~ fetchSequence(payload.entityIds)
      assignments ← * <~ Assignments.byAdmin(assignmentType, referenceType, admin).result
      newAssignedIds  = entities.map(_.id).diff(assignments.map(_.referenceId))
      succeedEntities = entities.filter(e ⇒ newAssignedIds.contains(e.id))
      newEntries      = buildSeq(succeedEntities, payload.storeAdminId)
      _ ← * <~ Assignments.createAll(newEntries)
      // Response, log activity, notifications subscription
      (successData, theResponse) = buildTheResponse(entities, assignments, payload, Assigning)
      _ ← * <~ subscribe(this, Seq(admin.accountId), successData)
      _ ← * <~ logBulkAssign(this, originator, admin, successData)
    } yield theResponse

  def unassignBulk(originator: User, payload: BulkAssignmentPayload[K])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[TheResponse[Seq[ResponseItem]]] =
    for {
      // Validation + unassign
      admin    ← * <~ Users.mustFindByAccountId(payload.storeAdminId)
      entities ← * <~ fetchSequence(payload.entityIds)
      querySeq = Assignments.byEntitySeqAndAdmin(assignmentType, entities, referenceType, admin)
      assignments ← * <~ querySeq.result
      _           ← * <~ querySeq.delete
      // Response, log activity, notifications subscription
      (successData, theResponse) = buildTheResponse(entities, assignments, payload, Unassigning)
      _ ← * <~ logBulkUnassign(this, originator, admin, successData)
      _ ← * <~ unsubscribe(this, Seq(admin.accountId), successData)
    } yield theResponse

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

  // DbResultT builders
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
      admins      ← Users.filter(_.accountId.inSetBind(assignments.map(_.storeAdminId))).result
    } yield assignments.zip(admins)

  private def searchKeys(entities: Seq[M]): Seq[String] = entities.map(_.primarySearchKey)
  private def searchKey(entity: M)                      = entity.primarySearchKey
}
