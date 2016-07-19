package services.carts

import cats.implicits._
import failures.GiftCardFailures._
import failures.OrderFailures._
import failures.StoreCreditFailures._
import failures._
import models.cord.OrderPayments.scope._
import models.cord._
import models.location.Regions
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.traits.Originator
import payloads.PaymentPayloads._
import responses.TheResponse
import responses.cart.FullCart
import services.{CartValidator, LogActivity}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartPaymentUpdater {

  type TheFullCart = DbResultT[TheResponse[FullCart.Root]]

  def addGiftCard(originator: Originator, payload: GiftCardPayment, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): TheFullCart =
    for {
      cart   ← * <~ getCartByOriginator(originator, refNum)
      result ← * <~ validGiftCardWithAmount(payload)
      (gc, amount) = result
      _ ← * <~ OrderPayments
           .byCartAndGiftCard(cart, gc)
           .mustNotFindOneOr(GiftCardPaymentAlreadyAdded(cart.refNum, payload.code))
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(gc).copy(cordRef = cart.refNum, amount = amount.some))
      resp  ← * <~ FullCart.buildRefreshed(cart)
      valid ← * <~ CartValidator(cart).validate()
      _     ← * <~ LogActivity.orderPaymentMethodAddedGc(originator, resp, gc, amount)
    } yield TheResponse.validated(resp, valid)

  def editGiftCard(
      originator: Originator,
      payload: GiftCardPayment,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart   ← * <~ getCartByOriginator(originator, refNum)
      result ← * <~ validGiftCardWithAmount(payload)
      (gc, amount) = result
      orderPayment ← * <~ OrderPayments
                      .byCartAndGiftCard(cart, gc)
                      .mustFindOneOr(GiftCardPaymentNotFound(cart.refNum, payload.code))
      _     ← * <~ OrderPayments.update(orderPayment, orderPayment.copy(amount = amount.some))
      resp  ← * <~ FullCart.buildRefreshed(cart)
      valid ← * <~ CartValidator(cart).validate()
      _ ← * <~ LogActivity
           .orderPaymentMethodUpdatedGc(originator, resp, gc, orderPayment.amount, amount)
    } yield TheResponse.validated(resp, valid)

  private def validGiftCardWithAmount(payload: GiftCardPayment)(implicit ec: EC, db: DB, ac: AC) =
    for {
      gc ← * <~ GiftCards.mustFindByCode(payload.code, code ⇒ NotFoundFailure400(GiftCard, code))
      _  ← * <~ gc.mustNotBeCart
      _  ← * <~ gc.mustBeActive
      _ ← * <~ GiftCardAdjustments
           .lastAuthByGiftCardId(gc.id)
           .mustNotFindOneOr(OpenTransactionsFailure)
      amount = selectGiftCardAmount(payload, gc)
      _ ← * <~ gc.mustHaveEnoughBalance(amount)
    } yield (gc, amount)

  private def selectGiftCardAmount(payload: GiftCardPayment, gc: GiftCard): Int =
    payload.amount match {
      case Some(amount) ⇒ amount
      case _            ⇒ gc.availableBalance
    }

  def addStoreCredit(
      originator: Originator,
      payload: StoreCreditPayment,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart = {
    def updateSC(has: Int, want: Int, cart: Cart, storeCredits: List[StoreCredit]) =
      if (has < want) {
        DbResultT.failure(
            CustomerHasInsufficientStoreCredit(id = cart.customerId, has = has, want = want))
      } else {
        def payments = StoreCredit.processFifo(storeCredits, want).map {
          case (sc, amount) ⇒
            OrderPayment.build(sc).copy(cordRef = cart.refNum, amount = amount.some)
        }

        for {
          _ ← * <~ OrderPayments
               .filter(_.cordRef === cart.refNum)
               .storeCredits
               .deleteAll(onSuccess = DbResultT.unit, onFailure = DbResultT.unit)
          _ ← * <~ OrderPayments.createAll(payments)
        } yield {}
      }

    for {
      cart         ← * <~ getCartByOriginator(originator, refNum)
      storeCredits ← * <~ StoreCredits.findAllActiveByCustomerId(cart.customerId).result
      reqAmount = payload.amount
      available = storeCredits.map(_.availableBalance).sum
      -          ← * <~ updateSC(available, reqAmount, cart, storeCredits.toList)
      validation ← * <~ CartValidator(cart).validate()
      response   ← * <~ FullCart.buildRefreshed(cart)
      _          ← * <~ LogActivity.orderPaymentMethodAddedSc(originator, response, payload.amount)
    } yield TheResponse.validated(response, validation)
  }

  def addCreditCard(originator: Originator, id: Int, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): TheFullCart =
    for {
      cart   ← * <~ getCartByOriginator(originator, refNum)
      cc     ← * <~ CreditCards.mustFindById400(id)
      _      ← * <~ cc.mustBeInWallet
      region ← * <~ Regions.findOneById(cc.regionId).safeGet
      _      ← * <~ OrderPayments.filter(_.cordRef === cart.refNum).creditCards.delete
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = None))
      valid ← * <~ CartValidator(cart).validate()
      resp  ← * <~ FullCart.buildRefreshed(cart)
      _     ← * <~ LogActivity.orderPaymentMethodAddedCc(originator, resp, cc, region)
    } yield TheResponse.validated(resp, valid)

  def deleteCreditCard(
      originator: Originator,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(
      originator: Originator,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(
      originator: Originator,
      refNum: Option[String],
      pmt: PaymentMethod.Type)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart  ← * <~ getCartByOriginator(originator, refNum)
      valid ← * <~ CartValidator(cart).validate()
      resp ← * <~ OrderPayments
              .filter(_.cordRef === cart.refNum)
              .byType(pmt)
              .deleteAll(onSuccess = FullCart.buildRefreshed(cart),
                         onFailure = DbResultT.failure(OrderPaymentNotFoundFailure(pmt)))
      _ ← * <~ LogActivity.orderPaymentMethodDeleted(originator, resp, pmt)
    } yield TheResponse.validated(resp, valid)

  def deleteGiftCard(originator: Originator, code: String, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): TheFullCart =
    for {
      cart      ← * <~ getCartByOriginator(originator, refNum)
      giftCard  ← * <~ GiftCards.mustFindByCode(code)
      validated ← * <~ CartValidator(cart).validate()
      deleteRes ← * <~ OrderPayments
                   .filter(_.paymentMethodId === giftCard.id)
                   .filter(_.cordRef === cart.refNum)
                   .giftCards
                   .deleteAll(onSuccess = FullCart.buildRefreshed(cart),
                              onFailure = DbResultT.failure(
                                  OrderPaymentNotFoundFailure(PaymentMethod.GiftCard)))
      _ ← * <~ LogActivity.orderPaymentMethodDeletedGc(originator, deleteRes, giftCard)
    } yield TheResponse.validated(deleteRes, validated)
}
