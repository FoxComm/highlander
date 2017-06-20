package phoenix.services.carts

import cats.implicits._
import phoenix.failures.CartFailures.NoShipMethod
import phoenix.models.account.User
import phoenix.models.cord._
import phoenix.models.shipping.{Shipments, ShippingMethods}
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.{CartValidator, LogActivity, ShippingManager}
import slick.jdbc.PostgresProfile.api._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import core.db._

object CartShippingMethodUpdater {

  def updateShippingMethod(originator: User, shippingMethodId: Int, refNum: Option[String] = None)(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      cart           ← * <~ getCartByOriginator(originator, refNum)
      oldShipMethod  ← * <~ ShippingMethods.forCordRef(cart.refNum).one
      shippingMethod ← * <~ ShippingMethods.mustFindById400(shippingMethodId)
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
      _         ← * <~ LogActivity().orderShippingMethodUpdated(originator, response, oldShipMethod)
    } yield TheResponse.validated(response, validated)

  def deleteShippingMethod(originator: User, refNum: Option[String] = None)(
      implicit ec: EC,
      apis: Apis,
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
      _     ← * <~ LogActivity().orderShippingMethodDeleted(originator, resp, shipMethod)
    } yield TheResponse.validated(resp, valid)
}
