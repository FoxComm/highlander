package services.carts

import cats.implicits._
import failures.CartFailures.NoShipMethod
import models.account.User
import models.cord._
import models.shipping.{Shipments, ShippingMethods}
import payloads.UpdateShippingMethod
import responses.TheResponse
import responses.cord.CartResponse
import services.{CartValidator, LogActivity, ShippingManager}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartShippingMethodUpdater {

  def updateShippingMethod(originator: User,
                           payload: UpdateShippingMethod,
                           refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      cart           ← * <~ getCartByOriginator(originator, refNum)
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
      _ ← * <~ CartPromotionUpdater.readjust(cart, failFatally = false).recover {
           case _ ⇒ () /* FIXME: don’t swallow errors @michalrus */
         }
      order     ← * <~ CartTotaler.saveTotals(cart)
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ CartResponse.buildRefreshed(order)
      _         ← * <~ LogActivity.orderShippingMethodUpdated(originator, response, oldShipMethod)
    } yield TheResponse.validated(response, validated)

  def deleteShippingMethod(originator: User, refNum: Option[String] = None)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ getCartByOriginator(originator, refNum)
      shipMethod ← * <~ ShippingMethods
                    .forCordRef(cart.refNum)
                    .mustFindOneOr(NoShipMethod(cart.refNum))
      _ ← * <~ OrderShippingMethods.findByOrderRef(cart.refNum).delete
      // update changed totals
      _ ← * <~ CartPromotionUpdater.readjust(cart, failFatally = false).recover {
           case _ ⇒ () /* FIXME: don’t swallow errors @michalrus */
         }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      resp  ← * <~ CartResponse.buildRefreshed(cart)
      _     ← * <~ LogActivity.orderShippingMethodDeleted(originator, resp, shipMethod)
    } yield TheResponse.validated(resp, valid)
}
