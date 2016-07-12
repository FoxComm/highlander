package services.carts

import cats.implicits._
import failures.CartFailures.NoShipMethod
import models.cord._
import models.shipping.{Shipments, ShippingMethods}
import models.traits.Originator
import payloads.UpdateShippingMethod
import responses.TheResponse
import responses.cart.FullCart
import services.{CartValidator, LogActivity, ShippingManager}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartShippingMethodUpdater {

  def updateShippingMethod(originator: Originator,
                           payload: UpdateShippingMethod,
                           refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart           ← * <~ getCartByOriginator(originator, refNum)
      _              ← * <~ cart.mustBeActive
      oldShipMethod  ← * <~ ShippingMethods.forCordRef(cart.refNum).one
      shippingMethod ← * <~ ShippingMethods.mustFindById400(payload.shippingMethodId)
      _              ← * <~ shippingMethod.mustBeActive
      _              ← * <~ ShippingManager.evaluateShippingMethodForCart(shippingMethod, cart)
      _ ← * <~ Shipments
           .filter(_.cordRef === cart.refNum)
           .map(_.orderShippingMethodId)
           .update(None)
      _ ← * <~ OrderShippingMethods.findByOrderRef(cart.refNum).delete
      orderShipMethod ← * <~ OrderShippingMethods.create(
                           OrderShippingMethod(cordRef = cart.refNum,
                                               shippingMethodId = shippingMethod.id,
                                               price = shippingMethod.price))
      _ ← * <~ Shipments
           .filter(_.cordRef === cart.refNum)
           .map(_.orderShippingMethodId)
           .update(orderShipMethod.id.some)
      // update changed totals
      _         ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      order     ← * <~ CartTotaler.saveTotals(cart)
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ FullCart.buildRefreshed(order)
      _         ← * <~ LogActivity.orderShippingMethodUpdated(originator, response, oldShipMethod)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def deleteShippingMethod(originator: Originator, refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart ← * <~ getCartByOriginator(originator, refNum)
      _    ← * <~ cart.mustBeActive
      shipMethod ← * <~ ShippingMethods
                    .forCordRef(cart.refNum)
                    .mustFindOneOr(NoShipMethod(cart.refNum))
      _ ← * <~ OrderShippingMethods.findByOrderRef(cart.refNum).delete
      // update changed totals
      _     ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      resp  ← * <~ FullCart.buildRefreshed(cart)
      _     ← * <~ LogActivity.orderShippingMethodDeleted(originator, resp, shipMethod)
    } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)
}
