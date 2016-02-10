package services

import scala.concurrent.ExecutionContext

import models.{GiftCards, StoreCredits, StoreCreditAdjustments, GiftCardAdjustments, Order, OrderLineItems,
OrderPayment, OrderPayments, OrderShippingAddresses, OrderShippingMethods}
import services.CartFailures._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.{DbResult, lift}

trait CartValidation {
  def validate(isCheckout: Boolean = false): DbResult[CartValidatorResponse]
}

// warnings would be turned into `errors` during checkout but if we're still in cart mode
// then we'll display to end-user as warnings/alerts since they are not "done" with their cart
final case class CartValidatorResponse(
  alerts:   Option[Failures] = None,
  warnings: Option[Failures] = None)

final case class CartValidator(cart: Order)(implicit db: Database, ec: ExecutionContext)
  extends CartValidation {

  def validate(isCheckout: Boolean = false): DbResult[CartValidatorResponse] = {
    val response = CartValidatorResponse()
    (for {
      state ← hasItems(response)
      state ← hasShipAddress(state)
      state ← validShipMethod(state)
      state ← sufficientPayments(state, isCheckout)
    } yield state).flatMap(DbResult.good)
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
            _ ⇒ warning(response, InvalidShippingMethod(cart.refNum)), // FIXME validator warning and actual failure differ
            _ ⇒ response
          )
        }

      case None ⇒
        lift(warning(response, NoShipMethod(cart.refNum)))
    }
  }

  private def sufficientPayments(response: CartValidatorResponse, isCheckout: Boolean): DBIO[CartValidatorResponse] = {

    def cartFunds(payments: Seq[OrderPayment]) = {
      if (isCheckout) {
        val paymentIds = payments.map(_.id)

        val authorizedStoreCreditPayments = StoreCreditAdjustments.authorizedOrderPayments(paymentIds).map(_.debit)
        val authorizedGiftCardPayments = GiftCardAdjustments.authorizedOrderPayments(paymentIds).map(_.debit)

        authorizedStoreCreditPayments.unionAll(authorizedGiftCardPayments)
      } else {
        def forType(typeFilter: OrderPayment ⇒ Boolean) = payments.filter(typeFilter).map(_.paymentMethodId).toSet

        val availableStoreCreditBalance = StoreCredits.findAllByIds(forType(_.isStoreCredit)).map(_.availableBalance)
        val availableGiftCardBalance = GiftCards.findAllByIds(forType(_.isGiftCard)).map(_.availableBalance)

        availableStoreCreditBalance.unionAll(availableGiftCardBalance)
      }
    }

    def availableFunds(grandTotal: Int, payments: Seq[OrderPayment]): DBIO[CartValidatorResponse] = {
      // we'll find out if the CC doesn't auth at checkout but we presume sufficient funds if we have a
      // credit card regardless of GC/SC funds availability
      if (payments.exists(_.isCreditCard)) {
        lift(response)
      } else if (payments.nonEmpty) {
        cartFunds(payments).sum.result.map {
          case Some(funds) if funds >= grandTotal ⇒
            response

          case _ ⇒
            warning(response, InsufficientFunds(cart.refNum))
        }
      } else {
        lift(warning(response, InsufficientFunds(cart.refNum)))
      }
    }

    if (cart.grandTotal > 0) {
      OrderPayments.findAllByOrderId(cart.id).result.flatMap(availableFunds(cart.grandTotal, _))
    } else {
      lift(response)
    }
  }

  private def warning(response: CartValidatorResponse, failure: Failure): CartValidatorResponse =
    response.copy(warnings = response.warnings.fold(Failures(failure))
    (current ⇒ Failures(current.toList :+ failure: _*)))
}
