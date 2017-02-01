package services

import failures.CartFailures._
import failures.{Failure, Failures}
import models.cord._
import models.cord.lineitems.CartLineItems
import models.inventory.Sku
import models.objects.{FullObject, ProductSkuLinks}
import models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import models.payment.storecredit.{StoreCreditAdjustments, StoreCredits}
import models.product.{Mvp, Product}
import services.objects.ObjectManager
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

trait CartValidation {
  def validate(isCheckout: Boolean = false,
               fatalWarnings: Boolean = false): DbResultT[CartValidatorResponse]
}

// warnings would be turned into `errors` during checkout but if we're still in cart mode
// then we'll display to end-user as warnings/alerts since they are not "done" with their cart
case class CartValidatorResponse(alerts: Option[Failures] = None,
                                 warnings: Option[Failures] = None) {}

case class CartValidator(cart: Cart)(implicit ec: EC, db: DB) extends CartValidation {

  def validate(isCheckout: Boolean = false,
               fatalWarnings: Boolean = false): DbResultT[CartValidatorResponse] = {
    val validationResult = for {
      state ← hasItems(CartValidatorResponse())
      state ← hasShipAddress(state)
      state ← validShipMethod(state)
      state ← sufficientPayments(state, isCheckout)
      state ← ensureHasActiveItemsOnly(state)
    } yield state
    if (fatalWarnings) {
      validationResult.dbresult.flatMap { validatorResponse ⇒
        validatorResponse.warnings match {
          case Some(warnings) ⇒
            DbResultT.failures(warnings)
          case _ ⇒
            DbResultT.good(validatorResponse)
        }
      }
    } else {
      DbResultT.fromDbio(validationResult)
    }
  }

  //todo: do we need alway have sku or at least sku or gc
  private def hasItems(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    CartLineItems.byCordRef(cart.refNum).length.result.map { numItems ⇒
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

  private def validShipMethod(response: CartValidatorResponse)(
      implicit db: DB): DBIO[CartValidatorResponse] =
    (for {
      osm ← OrderShippingMethods.findByOrderRef(cart.refNum)
      sm  ← osm.shippingMethod
    } yield (osm, sm)).one.flatMap {
      case Some((osm, sm)) ⇒
        ShippingManager
          .evaluateShippingMethodForCart(sm, cart)
          .fold(
              _ ⇒ warning(response, InvalidShippingMethod(cart.refNum)), // FIXME validator warning and actual failure differ
              _ ⇒ response
          )

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
    }

    def availableFunds(grandTotal: Int, payments: Seq[OrderPayment]): DBIO[CartValidatorResponse] = {
      // we'll find out if the CC doesn't auth at checkout but we presume sufficient funds if we have a
      // credit card regardless of GC/SC funds availability
      if (payments.exists(_.isCreditCard)) {
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

  private def ensureHasActiveItemsOnly(
      response: CartValidatorResponse)(implicit ec: EC, db: DB): DBIO[CartValidatorResponse] = {
    val inactiveProductToFailure = (sku: FullObject[Sku], product: FullObject[Product]) ⇒
      LineItemHasInactiveProduct(Mvp.title(product.form, product.shadow), sku.model.code)

    getInactiveLineItemProducts.fold(
        failures ⇒ warning(response, failures.toList: _*),
        items ⇒ warning(response, items.map(inactiveProductToFailure.tupled): _*)
    )
  }

  private def getInactiveLineItemProducts(
      implicit ec: EC): DbResultT[Seq[(FullObject[Sku], FullObject[Product])]] =
    for {
      skuIds ← * <~ CartLineItems.byCordRef(cart.referenceNumber).map(_.skuId).distinct.result
      skusAndProducts ← * <~ ProductSkuLinks.joinLeftAndRight.filter {
                         case (_, sku) ⇒ sku.id inSet skuIds
                       }.result
      (products, skus) = skusAndProducts.unzip

      fullSkus     ← * <~ ObjectManager.getFullObjects[Sku](skus)
      fullProducts ← * <~ ObjectManager.getFullObjects[Product](products)

      invalidItems = fullSkus.zip(fullProducts).filterNot {
        case (sku, product) ⇒
          sku.isActive && product.isActive && sku.model.archivedAt.isEmpty && product.model.archivedAt.isEmpty
      }
    } yield invalidItems

  private def warning(response: CartValidatorResponse, failures: Failure*): CartValidatorResponse =
    if (failures.isEmpty) {
      response
    } else {
      val newWarnings = response.warnings.fold(Failures(failures: _*))(current ⇒
            Failures(current.toList ++ failures: _*))
      response.copy(warnings = newWarnings)
    }
}
