package services.orders

import cats.implicits._
import models.{OrderShippingMethod, OrderShippingMethods, Orders, Shipments, ShippingMethod, ShippingMethods}
import payloads.UpdateShippingMethod
import responses.{FullOrder, TheResponse}
import services.{CartValidator, NotFoundFailure400, Result, ShippingManager}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object OrderShippingMethodUpdater {

  def updateShippingMethod(payload: UpdateShippingMethod, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {
    order           ← * <~ Orders.mustFindByRefNum(refNum)
    _               ← * <~ order.mustBeCart
    shippingMethod  ← * <~ ShippingMethods.mustFindById(payload.shippingMethodId, i ⇒ NotFoundFailure400(ShippingMethod, i))
    _               ← * <~ shippingMethod.mustBeActive
    _               ← * <~ ShippingManager.evaluateShippingMethodForOrder(shippingMethod, order)
    _               ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(None)
    _               ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
    orderShipMethod ← * <~ OrderShippingMethods.create(OrderShippingMethod(orderId = order.id, shippingMethodId = shippingMethod.id))
    _               ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(orderShipMethod.id.some)
    validated       ← * <~ CartValidator(order).validate
    response        ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def deleteShippingMethod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustBeCart
    _     ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
    valid ← * <~ CartValidator(order).validate
    resp  ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runT()
}
