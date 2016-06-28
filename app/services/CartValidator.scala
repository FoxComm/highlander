package services

import failures.CartFailures._
import failures.{Failure, Failures}
import models.order._
import models.order.lineitems.OrderLineItems
import models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import models.payment.storecredit.{StoreCreditAdjustments, StoreCredits}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

trait CartValidation {
  def validate(isCheckout: Boolean = false,
               fatalWarnings: Boolean = false): DbResult[CartValidatorResponse]
}

// warnings would be turned into `errors` during checkout but if we're still in cart mode
// then we'll display to end-user as warnings/alerts since they are not "done" with their cart
case class CartValidatorResponse(alerts: Option[Failures] = None,
                                 warnings: Option[Failures] = None) {}

case class CartValidator(cart: Order)(implicit ec: EC) extends CartValidation {

  def validate(isCheckout: Boolean = false,
               fatalWarnings: Boolean = false): DbResult[CartValidatorResponse] = {
    val response = CartValidatorResponse()
    val validationResult = for {
      state ← hasItems(response)
      state ← hasShipAddress(state)
      state ← validShipMethod(state)
      state ← sufficientPayments(state, isCheckout)
    } yield state
    if (fatalWarnings)
      validationResult.flatMap { validatorResponse ⇒
        validatorResponse.warnings.fold(DbResult.good(validatorResponse))(warnings ⇒
              DbResult.failures(warnings))
      } else DbResult.fromDbio(validationResult)
  }

  private def hasItems(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    OrderLineItems.filter(_.orderRef === cart.refNum).length.result.map { numItems ⇒
      if (numItems == 0) warning(response, EmptyCart(cart.refNum)) else response
    }
  }

  private def hasShipAddress(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    OrderShippingAddresses.findByOrderRef(cart.refNum).one.map { shipAddress ⇒
      shipAddress.fold(warning(response, NoShipAddress(cart.refNum))) { _ ⇒
        response
      }
    }
  }

  private def validShipMethod(response: CartValidatorResponse): DBIO[CartValidatorResponse] =
    (for {
      osm ← OrderShippingMethods.findByOrderRef(cart.refNum)
      sm  ← osm.shippingMethod
    } yield (osm, sm)).one.flatMap {
      case Some((osm, sm)) ⇒
        ShippingManager.evaluateShippingMethodForOrder(sm, cart).map {
          _.fold(
              _ ⇒ warning(response, InvalidShippingMethod(cart.refNum)), // FIXME validator warning and actual failure differ
              _ ⇒ response
          )
        }

      case None ⇒
        lift(warning(response, NoShipMethod(cart.refNum)))
    }

  private def sufficientPayments(response: CartValidatorResponse,
                                 isCheckout: Boolean): DBIO[CartValidatorResponse] = {

    def cartFunds(payments: Seq[OrderPayment]) = {
      if (isCheckout) {
        val paymentIds = payments.map(_.id)

        val authorizedStoreCreditPayments =
          StoreCreditAdjustments.authorizedOrderPayments(paymentIds).map(_.debit)
        val authorizedGiftCardPayments =
          GiftCardAdjustments.authorizedOrderPayments(paymentIds).map(_.debit)

        authorizedStoreCreditPayments.unionAll(authorizedGiftCardPayments)
      } else {
        def forType(typeFilter: OrderPayment ⇒ Boolean) =
          payments.filter(typeFilter).map(_.paymentMethodId).toSet

        val availableStoreCreditBalance =
          StoreCredits.findAllByIds(forType(_.isStoreCredit)).map(_.availableBalance)
        val availableGiftCardBalance =
          GiftCards.findAllByIds(forType(_.isGiftCard)).map(_.availableBalance)

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
      OrderPayments
        .findAllByOrderRef(cart.refNum)
        .result
        .flatMap(availableFunds(cart.grandTotal, _))
    } else {
      lift(response)
    }
  }

  private def warning(response: CartValidatorResponse, failure: Failure): CartValidatorResponse =
    response.copy(warnings = response.warnings.fold(Failures(failure))(current ⇒
              Failures(current.toList :+ failure: _*)))
}
