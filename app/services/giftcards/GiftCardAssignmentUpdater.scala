package services.giftcards

import models.payment.giftcard._
import models.{NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.GiftCardBulkAssignmentPayload
import responses.{GiftCardResponse, TheResponse}
import responses.GiftCardResponse.Root
import services.Util._
import services.{NotificationManager, LogActivity, GiftCardAssigneeNotFound, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import models.activity.{Dimension, ActivityContext}

object GiftCardAssignmentUpdater {

  def assign(admin: StoreAdmin, code: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[Root]] = (for {

    giftCard        ← * <~ GiftCards.mustFindByCode(code)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    assignees       ← * <~ GiftCardAssignments.assigneesFor(giftCard).result.toXor
    newAssignments  = adminIds.diff(assignees.map(_.id))
      .map(adminId ⇒ GiftCardAssignment(giftCardId = giftCard.id, assigneeId = adminId))
    _               ← * <~ GiftCardAssignments.createAll(newAssignments)
    newGiftCard     ← * <~ GiftCards.refresh(giftCard).toXor
    response        ← * <~ GiftCardResponse.fromGiftCard(giftCard).toXor
    notFoundAdmins  = diffToFailures(requestedAssigneeIds, adminIds, StoreAdmin)
    assignedAdmins  = response.assignees.filter(a ⇒ newAssignments.map(_.assigneeId).contains(a.assignee.id)).map(_.assignee)
    _               ← * <~ LogActivity.assignedToGiftCard(admin, giftCard, assignedAdmins)
    _               ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id),
      dimension = Dimension.giftCard, reason = NotificationSubscription.Assigned, objectIds = Seq(code))
  } yield TheResponse.build(response, errors = notFoundAdmins)).runTxn()

  def unassign(admin: StoreAdmin, code: String, assigneeId: Int)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[Root] = (for {

    giftCard        ← * <~ GiftCards.mustFindByCode(code)
    assignee        ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment      ← * <~ GiftCardAssignments.byAssignee(assignee)
                                              .one.mustFindOr(GiftCardAssigneeNotFound(giftCard.code, assigneeId))
    _               ← * <~ GiftCardAssignments.byAssignee(assignee).delete
    response        ← * <~ GiftCardResponse.fromGiftCard(giftCard).toXor
    _               ← * <~ LogActivity.unassignedFromGiftCard(admin, giftCard, assignee)
    _               ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId),
      dimension = Dimension.giftCard, reason = NotificationSubscription.Assigned, objectIds = Seq(code))
  } yield response).runTxn()

  def assignBulk(admin: StoreAdmin, payload: GiftCardBulkAssignmentPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkGiftCardUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    giftCards       ← * <~ GiftCards.filter(_.code.inSetBind(payload.giftCardCodes)).result.toXor
    assignee        ← * <~ StoreAdmins.mustFindById400(payload.assigneeId)
    newAssignments  = for (gc ← giftCards) yield GiftCardAssignment(giftCardId = gc.id, assigneeId = assignee.id)
    _               ← * <~ GiftCardAssignments.createAll(newAssignments)
    response        ← * <~ GiftCardQueries.findAll
    notFound        = diffToFlatFailures(payload.giftCardCodes, giftCards.map(_.code), GiftCard)
    success         = giftCards.filter(gc ⇒ newAssignments.map(_.giftCardId).contains(gc.id)).map(_.code)
    _               ← * <~ LogActivity.bulkAssignedToGiftCards(admin, assignee, success)
    _               ← * <~ NotificationManager.subscribe(adminIds = Seq(assignee.id), dimension = Dimension.giftCard,
      reason = NotificationSubscription.Assigned, objectIds = giftCards.map(_.code)).value
  } yield response.copy(errors = notFound)).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: GiftCardBulkAssignmentPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkGiftCardUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    giftCards ← * <~ GiftCards.filter(_.code.inSetBind(payload.giftCardCodes)).result
    assignee  ← * <~ StoreAdmins.mustFindById400(payload.assigneeId)
    _         ← * <~ GiftCardAssignments.filter(_.assigneeId === payload.assigneeId)
      .filter(_.giftCardId.inSetBind(giftCards.map(_.id))).delete
    response  ← * <~ GiftCardQueries.findAll
    notFound  = diffToFlatFailures(payload.giftCardCodes, giftCards.map(_.code), GiftCard)
    success   = giftCards.filter(gc ⇒ payload.giftCardCodes.contains(gc.code)).map(_.code)
    _         ← * <~ LogActivity.bulkUnassignedFromGiftCards(admin, assignee, success)
    _         ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assignee.id), dimension = Dimension.giftCard,
      reason = NotificationSubscription.Assigned, objectIds = giftCards.map(_.code)).value
  } yield response.copy(errors = notFound)).runTxn()
}
