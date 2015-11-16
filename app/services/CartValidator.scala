package services

import scala.concurrent.{ExecutionContext, Future}

import services.CartFailures._
import models.{OrderShippingMethods, Order, OrderLineItems, OrderShippingAddresses, OrderPayments, OrderPayment,
StoreCredits, GiftCards}
import OrderPayments.scope._
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Slick.lift
import utils.Slick.implicits._

trait CartValidation {
  def validate: Result[CartValidatorResponse]
}

// warnings would be turned into `errors` during checkout but if we're still in cart mode
// then we'll display to end-user as warnings/alerts since they are not "done" with their cart
final case class CartValidatorResponse(
  alerts:   List[Failure] = List.empty[Failure],
  warnings: List[Failure] = List.empty[Failure])

final case class CartValidator(cart: Order)(implicit db: Database, ec: ExecutionContext)
  extends CartValidation {

  def validate: Result[CartValidatorResponse] = {
    val response = CartValidatorResponse()

    if (cart.isCart) {
      (for {
        state ← hasItems(response)
        state ← hasShipAddress(state)
        state ← validShipMethod(state)
        state ← sufficientPayments(state)
      } yield state).run().flatMap(Result.good)
    } else {
      Result.failure(OrderMustBeCart(cart.refNum))
    }
  }

  private def hasItems(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    OrderLineItems.filter(_.orderId === cart.id).length.result.map { numItems ⇒
      if (numItems == 0) warning(response, EmptyCart(cart.refNum)) else response
    }
  }

  private def hasShipAddress(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    OrderShippingAddresses.findByOrderId(cart.id).one.map { shipAddress ⇒
      shipAddress.fold(warning(response, NoShipAddress(cart.refNum))) { _ ⇒ response }
    }
  }

  private def validShipMethod(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    (for {
      osm ← OrderShippingMethods.findByOrderId(cart.id)
      sm  ← osm.shippingMethod
    } yield (osm, sm)).one.flatMap {
      case Some((osm, sm)) ⇒
        ShippingManager.evaluateShippingMethodForOrder(sm, cart).map { res ⇒
          res.fold(
            _ ⇒ warning(response, InvalidShippingMethod(cart.refNum)),
            isValid ⇒ if (isValid) response else warning(response, InvalidShippingMethod(cart.refNum))
          )
        }

      case None ⇒
        lift(warning(response, NoShipMethod(cart.refNum)))
    }
  }

  private def sufficientPayments(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    def availableFunds(grandTotal: Int, payments: Seq[OrderPayment]): DBIO[CartValidatorResponse] = {
      // we'll find out if the CC doesn't auth at checkout but we presume sufficient funds if we have a
      // credit card regardless of GC/SC funds availability
      if (payments.exists(_.isCreditCard)) {
        lift(response)
      } else if (payments.nonEmpty) {
        def ids(f: OrderPayment ⇒ Boolean) = payments.filter(f).map(_.paymentMethodId).toSet

        StoreCredits.findAllByIds(ids(_.isStoreCredit)).map(_.availableBalance)
          .unionAll(GiftCards.findAllByIds(ids(_.isGiftCard)).map(_.availableBalance))
          .sum
          .result.map {
          case Some(funds) if funds >= grandTotal ⇒
            response

          case _ ⇒
            warning(response, InsufficientFunds(cart.refNum))
        }
      } else {
        lift(warning(response, InsufficientFunds(cart.refNum)))
      }
    }

    OrderTotaler.grandTotal(cart).flatMap {
      case Some(grandTotal) if grandTotal > 0 ⇒
        OrderPayments.findAllByOrderId(cart.id).result.flatMap(availableFunds(grandTotal, _))

      case _ ⇒
        lift(response)
    }
  }

  private def warning(response: CartValidatorResponse, failure: Failure): CartValidatorResponse =
    response.copy(warnings = response.warnings :+ failure)
}

