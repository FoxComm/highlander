package services.orders

import scala.concurrent.ExecutionContext

import cats.implicits._
import models.order._
import models.shipping.{Shipments, ShippingMethods}
import models.traits.Originator
import payloads.UpdateShippingMethod
import responses.TheResponse
import responses.order.FullOrder
import services.{LogActivity, CartValidator, Result, ShippingManager}
import services.CartFailures.NoShipMethod
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import models.activity.ActivityContext

object OrderShippingMethodUpdater {

  def updateShippingMethod(originator: Originator, payload: UpdateShippingMethod, refNum: Option[String] = None)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order           ← * <~ getCartByOriginator(originator, refNum)
    _               ← * <~ order.mustBeCart
    oldShipMethod   ← * <~ ShippingMethods.forOrder(order).one.toXor
    shippingMethod  ← * <~ ShippingMethods.mustFindById400(payload.shippingMethodId)
    _               ← * <~ shippingMethod.mustBeActive
    _               ← * <~ ShippingManager.evaluateShippingMethodForOrder(shippingMethod, order)
    _               ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(None)
    _               ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
    orderShipMethod ← * <~ OrderShippingMethods.create(OrderShippingMethod(orderId = order.id, shippingMethodId = shippingMethod.id))
    _               ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(orderShipMethod.id.some)
    // update changed totals
    order           ← * <~ OrderTotaler.saveTotals(order)
    validated       ← * <~ CartValidator(order).validate()
    response        ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _               ← * <~ LogActivity.orderShippingMethodUpdated(originator, response, oldShipMethod)
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def deleteShippingMethod(originator: Originator, refNum: Option[String] = None)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order       ← * <~ getCartByOriginator(originator, refNum)
    _           ← * <~ order.mustBeCart
    shipMethod  ← * <~ ShippingMethods.forOrder(order).one.mustFindOr(NoShipMethod(order.refNum))
    _           ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
    // update changed totals
    order       ← * <~ OrderTotaler.saveTotals(order)
    valid       ← * <~ CartValidator(order).validate()
    resp        ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingMethodDeleted(originator, resp, shipMethod)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runTxn()
}
