package services.orders

import models.{Order, OrderWatcher, OrderWatchers, Orders, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, ResponseWithFailuresAndMetadata, TheResponse}
import services.{LogActivity, Failure, Failurez, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import models.activity.ActivityContext

object OrderWatcherUpdater {

  def watch(admin: StoreAdmin, refNum: String, requestedWatcherIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order           ← * <~ Orders.mustFindByRefNum(refNum)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedWatcherIds)).map(_.id).result
    watchers        ← * <~ OrderWatchers.watchersFor(order).result.toXor
    newWatchers     = adminIds.diff(watchers.map(_.id))
      .map(adminId ⇒ OrderWatcher(orderId = order.id, watcherId = adminId))
    _               ← * <~ OrderWatchers.createAll(newWatchers)
    newOrder        ← * <~ Orders.refresh(order).toXor
    fullOrder       ← * <~ FullOrder.fromOrder(newOrder).toXor
    warnings        = Failurez(requestedWatcherIds.diff(adminIds).map(NotFoundFailure404(StoreAdmin, _)): _*)
    assignedAdmins  = fullOrder.watchers.filter(w ⇒ newWatchers.map(_.watcherId).contains(w.watcher.id)).map(_.watcher)
    _               ← * <~ LogActivity.addedWatchersToOrder(admin, fullOrder, assignedAdmins)
  } yield TheResponse.build(fullOrder, warnings = warnings)).runT()

  def watchBulk(admin: StoreAdmin, payload: payloads.BulkWatchers)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders          ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      watcher         ← StoreAdmins.findById(payload.watcherId).result
      newWatchers     = for (o ← orders; a ← watcher) yield OrderWatcher(orderId = o.id, watcherId = a.id)
      _               ← OrderWatchers.createAll(newWatchers)
      allOrders       ← OrderQueries.findAll.result
      adminNotFound   = adminNotFoundFailure(watcher.headOption, payload.watcherId)
      ordersNotFound  = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
      orderRefNums    = orders.filter(o ⇒ newWatchers.map(_.orderId).contains(o.id)).map(_.referenceNumber)
      _               ← LogActivity.bulkAddedWatcherToOrders(admin, watcher.headOption, payload.watcherId, orderRefNums)
    } yield ResponseWithFailuresAndMetadata.fromXor(
      result = allOrders,
      addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  def unwatch(admin: StoreAdmin, payload: payloads.BulkWatchers)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders          ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      watcher         ← StoreAdmins.findById(payload.watcherId).result
      _               ← OrderWatchers.filter(_.watcherId === payload.watcherId)
        .filter(_.orderId.inSetBind(orders.map(_.id))).delete
      allOrders       ← OrderQueries.findAll.result
      adminNotFound   = adminNotFoundFailure(watcher.map(_.id).headOption, payload.watcherId)
      ordersNotFound  = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
      orderRefNums    = orders.filter(o ⇒ payload.referenceNumbers.contains(o.refNum)).map(_.referenceNumber)
      _               ← LogActivity.bulkRemovedWatcherFromOrders(admin, watcher.headOption, payload.watcherId, orderRefNums)
    } yield ResponseWithFailuresAndMetadata.fromXor(
      result = allOrders,
      addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  private def adminNotFoundFailure(a: Option[_], id: Int) = a match {
    case Some(_) ⇒ Seq.empty[Failure]
    case None ⇒ Seq(NotFoundFailure404(StoreAdmin, id))
  }

  private def ordersNotFoundFailures(requestedRefs: Seq[String], availableRefs: Seq[String]) =
    requestedRefs.diff(availableRefs).map(NotFoundFailure404(Order, _))

}
