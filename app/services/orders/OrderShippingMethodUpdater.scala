package services.orders

import scala.concurrent.ExecutionContext

import cats.implicits._
import models.{OrderShippingMethod, OrderShippingMethods, Orders, Shipments, ShippingMethod, ShippingMethods, StoreAdmin}
import payloads.UpdateShippingMethod
import responses.{FullOrder, TheResponse}
import services.{LogActivity, CartValidator, NotFoundFailure400, Result, ShippingManager}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import models.activity.ActivityContext

object OrderShippingMethodUpdater {

  def updateShippingMethod(admin: StoreAdmin, payload: UpdateShippingMethod, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order           ← * <~ Orders.mustFindByRefNum(refNum)
    _               ← * <~ order.mustBeCart
    shippingMethod  ← * <~ ShippingMethods.mustFindById(payload.shippingMethodId, i ⇒ NotFoundFailure400(ShippingMethod, i))
    _               ← * <~ shippingMethod.mustBeActive
    _               ← * <~ ShippingManager.evaluateShippingMethodForOrder(shippingMethod, order)
    _               ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(None)
    _               ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
    orderShipMethod ← * <~ OrderShippingMethods.create(OrderShippingMethod(orderId = order.id, shippingMethodId = shippingMethod.id))
    _               ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(orderShipMethod.id.some)
    // update changed totals
    order           ← * <~ OrderTotaler.saveTotals(order)
    validated       ← * <~ CartValidator(order).validate
    response        ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _               ← * <~ LogActivity.orderShippingMethodUpdated(admin, response, orderShipMethod)
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def deleteShippingMethod(admin: StoreAdmin, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustBeCart
    _     ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
    // update changed totals
    order ← * <~ OrderTotaler.saveTotals(order)
    valid ← * <~ CartValidator(order).validate
    resp  ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _     ← * <~ LogActivity.orderShippingMethodDeleted(admin, resp)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runT()
}
