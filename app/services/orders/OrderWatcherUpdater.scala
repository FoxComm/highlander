package services.orders

import models.{Order, OrderWatcher, OrderWatchers, Orders, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, ResponseWithFailuresAndMetadata, TheResponse}
import services.{Failure, Failurez, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object OrderWatcherUpdater {

  def watch(refNum: String, requestedWatcherIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {

    order           ← * <~ Orders.mustFindByRefNum(refNum)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedWatcherIds)).map(_.id).result
    watchers        ← * <~ OrderWatchers.watchersFor(order).result.toXor
    newWatchers     = adminIds.diff(watchers.map(_.id))
      .map(adminId ⇒ OrderWatcher(orderId = order.id, watcherId = adminId))
    _               ← * <~ OrderWatchers.createAll(newWatchers)
    newOrder        ← * <~ Orders.refresh(order).toXor
    fullOrder       ← * <~ FullOrder.fromOrder(newOrder).toXor
    warnings        = Failurez(requestedWatcherIds.diff(adminIds).map(NotFoundFailure404(StoreAdmin, _)): _*)
  } yield TheResponse.build(fullOrder, warnings = warnings)).runT()

  def watchBulk(payload: payloads.BulkWatchers)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders          ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      admin           ← StoreAdmins.findById(payload.watcherId).result
      newWatchers     = for (o ← orders; a ← admin) yield OrderWatcher(orderId = o.id, watcherId = a.id)
      _               ← OrderWatchers.createAll(newWatchers)
      allOrders       ← OrderQueries.findAll.result
      adminNotFound   = adminNotFoundFailure(admin.headOption, payload.watcherId)
      ordersNotFound  = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
    } yield ResponseWithFailuresAndMetadata.fromXor(
      result = allOrders,
      addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  def unwatch(payload: payloads.BulkWatchers)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders          ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      adminId         ← StoreAdmins.findById(payload.watcherId).extract.map(_.id).result
      _               ← OrderWatchers.filter(_.watcherId === payload.watcherId)
        .filter(_.orderId.inSetBind(orders.map(_.id))).delete
      allOrders       ← OrderQueries.findAll.result
      adminNotFound   = adminNotFoundFailure(adminId.headOption, payload.watcherId)
      ordersNotFound  = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
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
