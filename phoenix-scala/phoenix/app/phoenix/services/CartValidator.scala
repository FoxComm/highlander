package phoenix.services

import cats.implicits._
import core.db._
import core.failures.{Failure, Failures}
import objectframework.services.ObjectManager
import phoenix.failures.CartFailures._
import phoenix.models.cord._
import phoenix.models.payment.applepay.ApplePayCharges
import phoenix.models.cord.lineitems.CartLineItems
import phoenix.models.inventory.{IlluminatedSku, Skus}
import phoenix.models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import phoenix.models.payment.storecredit.{StoreCreditAdjustments, StoreCredits}
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._
import phoenix.models.location.Addresses

trait CartValidation {
  def validate(isCheckout: Boolean = false, fatalWarnings: Boolean = false): DbResultT[CartValidatorResponse]
}

// warnings would be turned into `errors` during checkout but if we're still in cart mode
// then we'll display to end-user as warnings/alerts since they are not "done" with their cart
case class CartValidatorResponse(alerts: Option[Failures] = None, warnings: Option[Failures] = None) {} // TODO: use real warnings from StateT. What’s with not used alerts? @michalrus

case class CartValidator(cart: Cart)(implicit ec: EC, db: DB, ctx: OC) extends CartValidation {

  def validate(isCheckout: Boolean = false,
               fatalWarnings: Boolean = false): DbResultT[CartValidatorResponse] = {
    val validationResult = for {
      // FIXME: this is the State monad… Use FoxyT. @michalrus
      state ← * <~ hasItems(CartValidatorResponse(), isCheckout)
      state ← * <~ (if (isCheckout) {
                      // Check SKU/Product inactive/archived state only during checkout.
                      // Deactivation/archival don’t happen often enough to justify
                      // this additional overhead for each cart GET request.
                      hasActiveLineItems(state)
                    } else DbResultT.pure(state))
      state ← * <~ hasShipAddress(state)
      state ← * <~ validShipMethod(state)
      state ← * <~ sufficientPayments(state, isCheckout)
    } yield state
    if (fatalWarnings) {
      validationResult.flatMap { validatorResponse ⇒
        validatorResponse.warnings match {
          case Some(warnings) ⇒
            DbResultT.failures(warnings)
          case _ ⇒
            DbResultT.good(validatorResponse)
        }
      }
    } else {
      validationResult
    }
  }

  // TODO: check if SKUs are not archived/deactivated @michalrus
  // TODO: check VariantValue, Variant? — this feels as if duplicating `LineItemUpdater.mustFindProductIdForSku` a bit. @michalrus
  // TODO: check if their Products are not archived/deactivated @michalrus
  private def hasActiveLineItems(response: CartValidatorResponse): DbResultT[CartValidatorResponse] =
    for {
      skus ← * <~ CartLineItems
              .byCordRef(cart.referenceNumber)
              .join(Skus)
              .on(_.skuId === _.id)
              .map(_._2)
              .result
      fullSkus ← ObjectManager.getFullObjects(skus)
      illuminatedSkus = fullSkus.map(IlluminatedSku.illuminate(ctx, _))
      // TODO: use .mustBeActive instead and proper StateT warnings @michalrus
    } yield warnings(response, illuminatedSkus.filterNot(_.isActive).map(_.inactiveError))

  //todo: do we need alway have sku or at least sku or gc
  private def hasItems(response: CartValidatorResponse,
                       isCheckout: Boolean): DbResultT[CartValidatorResponse] =
    for {
      numItems ← * <~ CartLineItems.byCordRef(cart.refNum).length.result
      response ← * <~ (if (numItems == 0) warning(response, EmptyCart(cart.refNum)) else response) // FIXME: use FoxyT @michalrus
    } yield response

  private def hasShipAddress(response: CartValidatorResponse): DBIO[CartValidatorResponse] =
    Addresses.findByCordRef(cart.refNum).one.map { shipAddress ⇒
      shipAddress.fold(warning(response, NoShipAddress(cart.refNum))) { _ ⇒
        response
      }
    }

  private def validShipMethod(response: CartValidatorResponse)(
      implicit db: DB): DbResultT[CartValidatorResponse] =
    for {
      shipping ← * <~ (for {
                  osm ← OrderShippingMethods.findByOrderRef(cart.refNum)
                  sm  ← osm.shippingMethod
                } yield (osm, sm)).one
      validatedResponse ← * <~ (shipping match {
                           case Some((osm, sm)) ⇒
                             ShippingManager
                               .evaluateShippingMethodForCart(sm, cart)
                               .map(_ ⇒ response)
                               .recover {
                                 case _ ⇒ warning(response, InvalidShippingMethod(cart.refNum))
                               } // FIXME validator warning and actual failure differ
                           case None ⇒
                             DbResultT(warning(response, NoShipMethod(cart.refNum)))
                         })
    } yield validatedResponse

  private def sufficientPayments(response: CartValidatorResponse,
                                 isCheckout: Boolean): DBIO[CartValidatorResponse] = {

    def cartFunds(payments: Seq[OrderPayment]): DBIO[Option[Long]] =
      if (isCheckout) {
        val paymentIds = payments.map(_.id)

        val authorizedStoreCreditPayments =
          StoreCreditAdjustments.authorizedOrderPayments(paymentIds).map(_.debit)
        val authorizedGiftCardPayments =
          GiftCardAdjustments.authorizedOrderPayments(paymentIds).map(_.debit)

        authorizedStoreCreditPayments.unionAll(authorizedGiftCardPayments).sum.result
      } else {
        def forType(typeFilter: OrderPayment ⇒ Boolean) =
          payments.filter(typeFilter).map(_.paymentMethodId).toSet

        val availableStoreCredits = for {
          (sc, op) ← StoreCredits
                      .findActive()
                      .filter(_.id.inSet(forType(_.isStoreCredit)))
                      .join(OrderPayments)
                      .on(_.id === _.paymentMethodId) if sc.availableBalance >= op.amount
        } yield op.amount

        val availableGiftCards = for {
          (gc, op) ← GiftCards
                      .findActive()
                      .filter(_.id.inSet(forType(_.isGiftCard)))
                      .join(OrderPayments)
                      .on(_.id === _.paymentMethodId) if gc.availableBalance >= op.amount
        } yield op.amount

        availableStoreCredits.unionAll(availableGiftCards).sum.result
      }

    def availableFunds(grandTotal: Long, payments: Seq[OrderPayment]): DBIO[CartValidatorResponse] =
      // we'll find out if the `ExternalFunds` doesn't auth at checkout but we presume sufficient funds if we have a
      // `ExternalFunds` regardless of GC/SC funds availability
      if (payments.exists(_.isExternalFunds)) {
        lift(response)
      } else if (payments.nonEmpty) {
        cartFunds(payments).map {
          case Some(funds) if funds >= grandTotal ⇒
            response

          case _ ⇒
            warning(response, InsufficientFunds(cart.refNum))
        }
      } else {
        lift(warning(response, InsufficientFunds(cart.refNum)))
      }

    if (cart.grandTotal > 0 || cart.subTotal > 0) {
      OrderPayments
        .findAllByCordRef(cart.refNum)
        .result
        .flatMap(availableFunds(cart.grandTotal, _))
    } else {
      lift(response)
    }
  }

  private def warning(response: CartValidatorResponse, failure: Failure): CartValidatorResponse =
    warnings(response, Seq(failure))

  // FIXME: wat @michalrus
  private def warnings(response: CartValidatorResponse, failures: Seq[Failure]): CartValidatorResponse =
    response.copy(warnings = response.warnings.fold(Failures(failures: _*))(current ⇒
      Failures(current.toList ++ failures: _*)))
}
