package phoenix.services.carts

import cats.implicits._
import phoenix.failures.GiftCardFailures._
import phoenix.failures.OrderFailures._
import phoenix.failures.StoreCreditFailures._
import core.failures._
import phoenix.failures.OpenTransactionsFailure
import phoenix.models.cord.OrderPayments.scope._
import phoenix.models.cord._
import phoenix.models.location.Regions
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.creditcard._
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit._
import phoenix.models.account.User
import phoenix.payloads.PaymentPayloads._
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.{CartValidator, LogActivity}
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import core.db._
import phoenix.models.payment.PaymentMethod.ApplePay
import phoenix.models.payment.applepay._

object CartPaymentUpdater {

  type TheFullCart = DbResultT[TheResponse[CartResponse]]

  def addGiftCard(originator: User,
                  payload: GiftCardPayment,
                  refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart   ← * <~ getCartByOriginator(originator, refNum)
      result ← * <~ validGiftCardWithAmount(payload)
      (gc, amount) = result
      _ ← * <~ OrderPayments
           .byCartAndGiftCard(cart, gc)
           .mustNotFindOneOr(GiftCardPaymentAlreadyAdded(cart.refNum, payload.code))
      _     ← * <~ OrderPayments.create(OrderPayment.build(gc).copy(cordRef = cart.refNum, amount = amount.some))
      resp  ← * <~ CartResponse.buildRefreshed(cart)
      valid ← * <~ CartValidator(cart).validate()
      _     ← * <~ LogActivity().orderPaymentMethodAddedGc(originator, resp, gc, amount)
    } yield TheResponse.validated(resp, valid)

  def editGiftCard(originator: User,
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
      resp  ← * <~ CartResponse.buildRefreshed(cart)
      valid ← * <~ CartValidator(cart).validate()
      _ ← * <~ LogActivity()
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

  private def selectGiftCardAmount(payload: GiftCardPayment, gc: GiftCard): Long =
    payload.amount match {
      case Some(amount) ⇒ amount
      case _            ⇒ gc.availableBalance
    }

  def addStoreCredit(originator: User,
                     payload: StoreCreditPayment,
                     refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart = {
    def updateSC(has: Long, want: Long, cart: Cart, storeCredits: List[StoreCredit]) =
      if (has < want) {
        DbResultT.failure(CustomerHasInsufficientStoreCredit(id = cart.accountId, has = has, want = want))
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
      storeCredits ← * <~ StoreCredits.findAllActiveByAccountId(cart.accountId).result
      reqAmount = payload.amount
      available = storeCredits.map(_.availableBalance).sum
      -          ← * <~ updateSC(available, reqAmount, cart, storeCredits.toList)
      validation ← * <~ CartValidator(cart).validate()
      response   ← * <~ CartResponse.buildRefreshed(cart)
      _          ← * <~ LogActivity().orderPaymentMethodAddedSc(originator, response, payload.amount)
    } yield TheResponse.validated(response, validation)
  }

  def addCreditCard(originator: User,
                    id: Int,
                    refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart   ← * <~ getCartByOriginator(originator, refNum)
      cc     ← * <~ CreditCards.mustFindById400(id)
      _      ← * <~ cc.mustBelongToAccount(cart.accountId)
      _      ← * <~ cc.mustBeInWallet
      region ← * <~ Regions.findOneById(cc.address.regionId).safeGet
      _      ← * <~ OrderPayments.filter(_.cordRef === cart.refNum).creditCards.delete
      _      ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = None))
      valid  ← * <~ CartValidator(cart).validate()
      resp   ← * <~ CartResponse.buildRefreshed(cart)
      _      ← * <~ LogActivity().orderPaymentMethodAddedCc(originator, resp, cc, region)
    } yield TheResponse.validated(resp, valid)

  def deleteCreditCard(originator: User,
                       refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(
      originator: User,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(
      originator: User,
      refNum: Option[String],
      pmt: PaymentMethod.Type)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart ← * <~ getCartByOriginator(originator, refNum)
      resp ← * <~ OrderPayments
              .filter(_.cordRef === cart.refNum)
              .byType(pmt)
              .deleteAll(onSuccess = CartResponse.buildRefreshed(cart),
                         onFailure = DbResultT.failure(OrderPaymentNotFoundFailure(pmt)))
      updatedCart ← * <~ getCartByOriginator(originator, refNum)
      valid       ← * <~ CartValidator(updatedCart).validate()
      _           ← * <~ LogActivity().orderPaymentMethodDeleted(originator, resp, pmt)
    } yield TheResponse.validated(resp, valid)

  def deleteGiftCard(originator: User,
                     code: String,
                     refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart     ← * <~ getCartByOriginator(originator, refNum)
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      deleteRes ← * <~ OrderPayments
                   .filter(_.paymentMethodId === giftCard.id)
                   .filter(_.cordRef === cart.refNum)
                   .giftCards
                   .deleteAll(onSuccess = CartResponse.buildRefreshed(cart),
                              onFailure =
                                DbResultT.failure(OrderPaymentNotFoundFailure(PaymentMethod.GiftCard)))
      updatedCart ← * <~ getCartByOriginator(originator, refNum)
      validated   ← * <~ CartValidator(updatedCart).validate()
      _           ← * <~ LogActivity().orderPaymentMethodDeletedGc(originator, deleteRes, giftCard)
    } yield TheResponse.validated(deleteRes, validated)

  def addApplePayPayment(
      originator: User,
      payload: CreateApplePayPayment,
      cartRefNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC, ctx: OC): TheFullCart =
    for {
      cart ← * <~ getCartByOriginator(originator, cartRefNum)
      _    ← * <~ OrderPayments.filter(_.cordRef === cart.refNum).applePays.delete

      //    create apple charge
      applePayment ← * <~ ApplePayments.create(
                      ApplePayment(accountId = originator.accountId, stripeTokenId = payload.stripeToken))

      _ ← * <~ OrderPayments.create(
           OrderPayment(cordRef = cart.refNum,
                        amount = None,
                        paymentMethodType = ApplePay,
                        paymentMethodId = applePayment.id)
         )

      valid ← * <~ CartValidator(cart).validate()
      resp  ← * <~ CartResponse.buildRefreshed(cart)
    } yield TheResponse.validated(resp, valid)

}
