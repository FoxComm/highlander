package services.giftcards

import models.payment.giftcard._
import models.{NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.GiftCardBulkWatchersPayload
import responses.{GiftCardResponse, TheResponse}
import responses.GiftCardResponse.Root
import services.Util._
import services.{NotificationManager, LogActivity, GiftCardWatcherNotFound, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import models.activity.{Dimension, ActivityContext}

object GiftCardWatcherUpdater {

  def watch(admin: StoreAdmin, code: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[Root]] = (for {

    giftCard        ← * <~ GiftCards.mustFindByCode(code)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    watchers        ← * <~ GiftCardWatchers.watchersFor(giftCard).result.toXor
    newWatchers  = adminIds.diff(watchers.map(_.id))
      .map(adminId ⇒ GiftCardWatcher(giftCardId = giftCard.id, watcherId = adminId))
    _               ← * <~ GiftCardWatchers.createAll(newWatchers)
    newGiftCard     ← * <~ GiftCards.refresh(giftCard).toXor
    response        ← * <~ GiftCardResponse.fromGiftCard(giftCard).toXor
    notFoundAdmins  = diffToFailures(requestedAssigneeIds, adminIds, StoreAdmin)
    assignedAdmins  = response.assignees.filter(a ⇒ newWatchers.map(_.watcherId).contains(a.assignee.id)).map(_.assignee)
    _               ← * <~ LogActivity.addedWatchersToGiftCard(admin, giftCard, assignedAdmins)
    _               ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id),
      dimension = Dimension.giftCard, reason = NotificationSubscription.Assigned, objectIds = Seq(code))
  } yield TheResponse.build(response, errors = notFoundAdmins)).runTxn()

  def unwatch(admin: StoreAdmin, code: String, assigneeId: Int)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[Root] = (for {

    giftCard        ← * <~ GiftCards.mustFindByCode(code)
    storeAdmin      ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment      ← * <~ GiftCardWatchers.byWatcher(storeAdmin)
                                           .one.mustFindOr(GiftCardWatcherNotFound(giftCard.code, assigneeId))
    _               ← * <~ GiftCardWatchers.byWatcher(storeAdmin).delete
    response        ← * <~ GiftCardResponse.fromGiftCard(giftCard).toXor
    _               ← * <~ LogActivity.removedWatcherFromGiftCard(admin, giftCard, storeAdmin)
    _               ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId),
      dimension = Dimension.giftCard, reason = NotificationSubscription.Assigned, objectIds = Seq(code))
  } yield response).runTxn()

  def watchBulk(admin: StoreAdmin, payload: GiftCardBulkWatchersPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkGiftCardUpdateResponse] = (for {
    
    // TODO: transfer sorting-paging metadata
    giftCards       ← * <~ GiftCards.filter(_.code.inSetBind(payload.giftCardCodes)).result.toXor
    storeAdmin      ← * <~ StoreAdmins.mustFindById400(payload.watcherId)
    newWatchers     = for (gc ← giftCards) yield GiftCardWatcher(giftCardId = gc.id, watcherId = storeAdmin.id)
    _               ← * <~ GiftCardWatchers.createAll(newWatchers)
    response        ← * <~ GiftCardQueries.findAll
    notFound        = diffToFlatFailures(payload.giftCardCodes, giftCards.map(_.code), GiftCard)
    success         = giftCards.filter(gc ⇒ newWatchers.map(_.giftCardId).contains(gc.id)).map(_.code)
    _               ← * <~ LogActivity.bulkAddedWatcherToGiftCards(admin, storeAdmin, success)
    _               ← * <~ NotificationManager.subscribe(adminIds = Seq(storeAdmin.id), dimension = Dimension.giftCard,
      reason = NotificationSubscription.Watching, objectIds = giftCards.map(_.code)).value
  } yield response.copy(errors = notFound)).runTxn()

  def unwatchBulk(admin: StoreAdmin, payload: GiftCardBulkWatchersPayload)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage, ac: ActivityContext): 
    Result[BulkGiftCardUpdateResponse] = (for {
    
    // TODO: transfer sorting-paging metadata
    giftCards   ← * <~ GiftCards.filter(_.code.inSetBind(payload.giftCardCodes)).result
    storeAdmin  ← * <~ StoreAdmins.mustFindById400(payload.watcherId)
    _           ← * <~ GiftCardWatchers.filter(_.watcherId === payload.watcherId)
      .filter(_.giftCardId.inSetBind(giftCards.map(_.id))).delete
    response    ← * <~ GiftCardQueries.findAll
    notFound    = diffToFlatFailures(payload.giftCardCodes, giftCards.map(_.code), GiftCard)
    success     = giftCards.filter(gc ⇒ payload.giftCardCodes.contains(gc.code)).map(_.code)
    _           ← * <~ LogActivity.bulkRemovedWatcherFromGiftCards(admin, storeAdmin, success)
    _           ← * <~ NotificationManager.unsubscribe(adminIds = Seq(storeAdmin.id), dimension = Dimension.giftCard,
      reason = NotificationSubscription.Watching, objectIds = giftCards.map(_.code)).value
  } yield response.copy(errors = notFound)).runTxn()
}
