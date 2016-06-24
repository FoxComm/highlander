package services.orders

import cats.implicits._
import failures.CartFailures.NoShipMethod
import models.order._
import models.shipping.{Shipments, ShippingMethods}
import models.traits.Originator
import payloads.UpdateShippingMethod
import responses.TheResponse
import responses.order.FullOrder
import services.{CartValidator, LogActivity, Result, ShippingManager}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object OrderShippingMethodUpdater {

  def updateShippingMethod(originator: Originator,
                           payload: UpdateShippingMethod,
                           refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] =
    (for {
      order          ← * <~ getCartByOriginator(originator, refNum)
      _              ← * <~ order.mustBeCart
      oldShipMethod  ← * <~ ShippingMethods.forOrder(order).one.toXor
      shippingMethod ← * <~ ShippingMethods.mustFindById400(payload.shippingMethodId)
      _              ← * <~ shippingMethod.mustBeActive
      _              ← * <~ ShippingManager.evaluateShippingMethodForOrder(shippingMethod, order)
      _              ← * <~ Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(None)
      _              ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
      orderShipMethod ← * <~ OrderShippingMethods.create(
                           OrderShippingMethod(orderId = order.id,
                                               shippingMethodId = shippingMethod.id,
                                               price = shippingMethod.price))
      _ ← * <~ Shipments
           .filter(_.orderId === order.id)
           .map(_.orderShippingMethodId)
           .update(orderShipMethod.id.some)
      // update changed totals
      _         ← * <~ OrderPromotionUpdater.readjust(order).recover { case _ ⇒ Unit }
      order     ← * <~ OrderTotaler.saveTotals(order)
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ FullOrder.refreshAndFullOrder(order).toXor
      _         ← * <~ LogActivity.orderShippingMethodUpdated(originator, response, oldShipMethod)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings))
      .runTxn()

  def deleteShippingMethod(originator: Originator, refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] =
    (for {
      order      ← * <~ getCartByOriginator(originator, refNum)
      _          ← * <~ order.mustBeCart
      shipMethod ← * <~ ShippingMethods.forOrder(order).mustFindOneOr(NoShipMethod(order.refNum))
      _          ← * <~ OrderShippingMethods.findByOrderId(order.id).delete
      // update changed totals
      _     ← * <~ OrderPromotionUpdater.readjust(order).recover { case _ ⇒ Unit }
      order ← * <~ OrderTotaler.saveTotals(order)
      valid ← * <~ CartValidator(order).validate()
      resp  ← * <~ FullOrder.refreshAndFullOrder(order).toXor
      _     ← * <~ LogActivity.orderShippingMethodDeleted(originator, resp, shipMethod)
    } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runTxn()
}
