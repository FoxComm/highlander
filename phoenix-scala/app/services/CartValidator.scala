package services

import java.time.Instant

import cats._
import cats.implicits._
import failures.CartFailures._
import failures.{ArchiveFailures, Failure, Failures}
import models.cord._
import models.traits.IlluminatedModel
import models.cord.lineitems.CartLineItems
import models.inventory.{Sku, Skus}
import models.payment.giftcard.{GiftCardAdjustments, GiftCards}
import models.payment.storecredit.{StoreCreditAdjustments, StoreCredits}
import services.objects.ObjectManager
import slick.dbio.Effect
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import slick.lifted.QueryBase
import slick.profile.FixedSqlAction
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._

trait CartValidation {
  def validate(isCheckout: Boolean = false,
               fatalWarnings: Boolean = false): DbResultT[CartValidatorResponse]
}

// warnings would be turned into `errors` during checkout but if we're still in cart mode
// then we'll display to end-user as warnings/alerts since they are not "done" with their cart
case class CartValidatorResponse(
    alerts: Option[Failures] = None,
    warnings: Option[Failures] = None) {} // TODO: use real warnings from StateT. What’s with not used alerts? @michalrus

object CartValidator {}

case class CartValidator(cart: Cart)(implicit ec: EC, db: DB) extends CartValidation {

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
  private def hasActiveLineItems(
      response: CartValidatorResponse): DbResultT[CartValidatorResponse] = {
    for {
      skus ← * <~ CartLineItems
              .byCordRef(cart.referenceNumber)
              .join(Skus)
              .on(_.skuId === _.id)
              .map(_._2)
              .result
      inactiveSkus ← * <~ skus.toList.filterA { sku ⇒
                      // FIXME: have mercy… ;( @michalrus
                      for {
                        full ← ObjectManager.getFullObject(DbResultT.pure(sku))
                        im = new IlluminatedModel[Unit] {
                          override def archivedAt: Option[Instant] = full.model.archivedAt

                          override def attributes: Json =
                            IlluminateAlgorithm.projectAttributes(full.form.attributes,
                                                                  full.shadow.attributes)

                          override protected def inactiveError: Failure = null
                        }
                      } yield im.mustBeActive.isLeft
                    }
    } yield
      if (inactiveSkus.isEmpty) response
      else
        warnings(
            response,
            inactiveSkus.map(sku ⇒
                  ArchiveFailures.LinkArchivedSkuFailure(cart, cart.referenceNumber, sku.code)))
  }

  //todo: do we need alway have sku or at least sku or gc
  private def hasItems(response: CartValidatorResponse,
                       isCheckout: Boolean): DbResultT[CartValidatorResponse] =
    for {
      numItems ← * <~ CartLineItems.byCordRef(cart.refNum).length.result
      response ← * <~ (if (numItems == 0) warning(response, EmptyCart(cart.refNum)) else response) // FIXME: use FoxyT @michalrus
    } yield response

  private def hasShipAddress(response: CartValidatorResponse): DBIO[CartValidatorResponse] = {
    OrderShippingAddresses.findByOrderRef(cart.refNum).one.map { shipAddress ⇒
      shipAddress.fold(warning(response, NoShipAddress(cart.refNum))) { _ ⇒
        response
      }
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

    def cartFunds(payments: Seq[OrderPayment]): DBIO[Option[Int]] = {
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

  private def warning(response: CartValidatorResponse, failure: Failure): CartValidatorResponse =
    warnings(response, Seq(failure))

  // FIXME: wat @michalrus
  private def warnings(response: CartValidatorResponse,
                       failures: Seq[Failure]): CartValidatorResponse =
    response.copy(warnings = response.warnings.fold(Failures(failures: _*))(current ⇒
              Failures(current.toList ++ failures: _*)))
}
