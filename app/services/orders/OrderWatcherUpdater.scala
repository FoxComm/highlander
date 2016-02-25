package services.orders

import models.order._
import models.{NotificationSubscription, StoreAdmin, StoreAdmins}
import responses.{TheResponse, BatchMetadata, BatchMetadataSource}
import responses.BatchMetadata.flattenErrors
import payloads.OrderBulkWatchersPayload
import responses.TheResponse
import responses.order.FullOrder
import services.Util._
import services.{NotificationManager, LogActivity, OrderWatcherNotFound, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import models.activity.{Dimension, ActivityContext}
import utils._

object OrderWatcherUpdater {

  def watch(admin: StoreAdmin, refNum: String, requestedWatcherIds: Seq[Int])(implicit db: Database,
    ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order           ← * <~ Orders.mustFindByRefNum(refNum)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedWatcherIds)).map(_.id).result
    watchers        ← * <~ OrderWatchers.watchersFor(order).result.toXor
    newWatchers     = adminIds.diff(watchers.map(_.id)).map(adminId ⇒ OrderWatcher(orderId = order.id, watcherId = adminId))
    _               ← * <~ OrderWatchers.createAll(newWatchers)
    newOrder        ← * <~ Orders.refresh(order).toXor
    fullOrder       ← * <~ FullOrder.fromOrder(newOrder).toXor
    notFoundAdmins  = diffToFailures(requestedWatcherIds, adminIds, StoreAdmin)
    assignedAdmins  = fullOrder.watchers.filter(w ⇒ newWatchers.map(_.watcherId).contains(w.watcher.id)).map(_.watcher)
    _               ← * <~ LogActivity.addedWatchersToOrder(admin, fullOrder, assignedAdmins)
    _               ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id), dimension = Dimension.order,
      reason = NotificationSubscription.Assigned, objectIds = Seq(order.referenceNumber))
  } yield TheResponse.build(fullOrder, errors = notFoundAdmins)).runTxn()

  def unwatch(admin: StoreAdmin, refNum: String, assigneeId: Int)(implicit db: Database, ec: ExecutionContext,
    ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order           ← * <~ Orders.mustFindByRefNum(refNum)
    watcher         ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment      ← * <~ OrderWatchers.byWatcher(watcher).one.mustFindOr(OrderWatcherNotFound(refNum, assigneeId))
    _               ← * <~ OrderWatchers.byWatcher(watcher).delete
    fullOrder       ← * <~ FullOrder.fromOrder(order).toXor
    _               ← * <~ LogActivity.removedWatcherFromOrder(admin, fullOrder, watcher)
    _               ← * <~ NotificationManager.unsubscribe(adminIds = Seq(watcher.id), dimension = Dimension.order,
      reason = NotificationSubscription.Assigned, objectIds = Seq(order.referenceNumber))
  } yield TheResponse.build(fullOrder)).runTxn()

  def watchBulk(admin: StoreAdmin, payload: OrderBulkWatchersPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    orders         ← * <~ Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
    watcher        ← * <~ StoreAdmins.mustFindById400(payload.watcherId)
    newWatchers    = for (order ← orders) yield OrderWatcher(orderId = order.id, watcherId = watcher.id)
    _              ← * <~ OrderWatchers.createAll(newWatchers)
    response       ← * <~ OrderQueries.findAll
    orderRefNums   = orders.filter(o ⇒ newWatchers.map(_.orderId).contains(o.id)).map(_.referenceNumber)
    _              ← * <~ LogActivity.bulkAddedWatcherToOrders(admin, watcher, orderRefNums)
    _              ← * <~ NotificationManager.subscribe(adminIds = Seq(watcher.id), dimension = Dimension.order,
                            reason = NotificationSubscription.Watching, objectIds = orders.map(_.referenceNumber))
    // Prepare batch response
    batchFailures  = diffToBatchErrors(payload.referenceNumbers, orders.map(_.referenceNumber), Order)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Order, orderRefNums, batchFailures))
  } yield response.copy(errors = flattenErrors(batchFailures), batch = Some(batchMetadata))).runTxn()

  def unwatchBulk(admin: StoreAdmin, payload: OrderBulkWatchersPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = (for {

    // TODO: transfer sorting-paging metadata
    orders         ← * <~ Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
    watcher        ← * <~ StoreAdmins.mustFindById400(payload.watcherId)
    _              ← * <~ OrderWatchers.filter(_.watcherId === payload.watcherId).filter(_.orderId.inSetBind(orders.map(_.id))).delete
    response       ← * <~ OrderQueries.findAll
    orderRefNums   = orders.filter(o ⇒ payload.referenceNumbers.contains(o.refNum)).map(_.referenceNumber)
    _              ← * <~ LogActivity.bulkRemovedWatcherFromOrders(admin, watcher, orderRefNums)
    _              ← * <~ NotificationManager.unsubscribe(adminIds = Seq(watcher.id), dimension = Dimension.order,
                            reason = NotificationSubscription.Watching, objectIds = orders.map(_.referenceNumber))
    // Prepare batch response
    batchFailures  = diffToBatchErrors(payload.referenceNumbers, orders.map(_.referenceNumber), Order)
    batchMetadata  = BatchMetadata(BatchMetadataSource(Order, orderRefNums, batchFailures))
  } yield response.copy(errors = flattenErrors(batchFailures), batch = Some(batchMetadata))).runTxn()
}
