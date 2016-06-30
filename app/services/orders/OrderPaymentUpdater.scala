package services.orders

import cats.implicits._
import failures.GiftCardFailures._
import failures.OrderFailures._
import failures.StoreCreditFailures._
import failures._
import models.location.Regions
import models.order.OrderPayments.scope._
import models.order._
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.traits.Originator
import payloads.PaymentPayloads._
import responses.TheResponse
import responses.order.FullOrder
import responses.order.FullOrder.refreshAndFullOrder
import services.{CartValidator, LogActivity}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderPaymentUpdater {

  type TheFullOrder = DbResultT[TheResponse[FullOrder.Root]]

  def addGiftCard(originator: Originator, payload: GiftCardPayment, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): TheFullOrder =
    for {
      order  ← * <~ getCartByOriginator(originator, refNum)
      _      ← * <~ order.mustBeCart
      result ← * <~ validGiftCardWithAmount(payload)
      (gc, amount) = result
      _ ← * <~ OrderPayments
           .byOrderAndGiftCard(order, gc)
           .one
           .mustNotFindOr(GiftCardPaymentAlreadyAdded(order.refNum, payload.code))
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(gc).copy(orderRef = order.refNum, amount = amount.some))
      resp  ← * <~ refreshAndFullOrder(order)
      valid ← * <~ CartValidator(order).validate()
      _     ← * <~ LogActivity.orderPaymentMethodAddedGc(originator, resp, gc, amount)
    } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)

  def editGiftCard(originator: Originator,
                   payload: GiftCardPayment,
                   refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC): TheFullOrder =
    for {
      order  ← * <~ getCartByOriginator(originator, refNum)
      _      ← * <~ order.mustBeCart
      result ← * <~ validGiftCardWithAmount(payload)
      (gc, amount) = result
      orderPayment ← * <~ OrderPayments
                      .byOrderAndGiftCard(order, gc)
                      .mustFindOneOr(GiftCardPaymentNotFound(order.refNum, payload.code))
      _     ← * <~ OrderPayments.update(orderPayment, orderPayment.copy(amount = amount.some))
      resp  ← * <~ refreshAndFullOrder(order)
      valid ← * <~ CartValidator(order).validate()
      _ ← * <~ LogActivity
           .orderPaymentMethodUpdatedGc(originator, resp, gc, orderPayment.amount, amount)
    } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)

  private def validGiftCardWithAmount(payload: GiftCardPayment)(implicit ec: EC, db: DB, ac: AC) =
    for {
      gc ← * <~ GiftCards.mustFindByCode(payload.code, code ⇒ NotFoundFailure400(GiftCard, code))
      _  ← * <~ gc.mustNotBeCart
      _  ← * <~ gc.mustBeActive
      _ ← * <~ GiftCardAdjustments
           .lastAuthByGiftCardId(gc.id)
           .one
           .mustNotFindOr(OpenTransactionsFailure)
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
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC): TheFullOrder = {
    def updateSC(has: Int, want: Int, order: Order, storeCredits: List[StoreCredit]) =
      if (has < want) {
        DbResultT.failure(
            CustomerHasInsufficientStoreCredit(id = order.customerId, has = has, want = want))
      } else {
        def payments = StoreCredit.processFifo(storeCredits, want).map {
          case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderRef = order.refNum, amount = amount.some)
        }

        for {
          _ ← * <~ OrderPayments
               .filter(_.orderRef === order.refNum)
               .storeCredits
               .deleteAll(onSuccess = DbResultT.unit, onFailure = DbResultT.unit)
          _ ← * <~ OrderPayments.createAll(payments)
        } yield {}
      }

    for {
      order        ← * <~ getCartByOriginator(originator, refNum)
      _            ← * <~ order.mustBeCart
      storeCredits ← * <~ StoreCredits.findAllActiveByCustomerId(order.customerId).result.toXor
      reqAmount = payload.amount
      available = storeCredits.map(_.availableBalance).sum
      -          ← * <~ updateSC(available, reqAmount, order, storeCredits.toList)
      validation ← * <~ CartValidator(order).validate()
      response   ← * <~ refreshAndFullOrder(order)
      _          ← * <~ LogActivity.orderPaymentMethodAddedSc(originator, response, payload.amount)
    } yield TheResponse.build(response, alerts = validation.alerts, warnings = validation.warnings)
  }

  def addCreditCard(originator: Originator, id: Int, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): TheFullOrder =
    for {
      order  ← * <~ getCartByOriginator(originator, refNum)
      _      ← * <~ order.mustBeCart
      cc     ← * <~ CreditCards.mustFindById400(id)
      _      ← * <~ cc.mustBeInWallet
      region ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
      _      ← * <~ OrderPayments.filter(_.orderRef === order.refNum).creditCards.delete
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(cc).copy(orderRef = order.refNum, amount = None))
      valid ← * <~ CartValidator(order).validate()
      resp  ← * <~ refreshAndFullOrder(order)
      _     ← * <~ LogActivity.orderPaymentMethodAddedCc(originator, resp, cc, region)
    } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)

  def deleteCreditCard(
      originator: Originator,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC): TheFullOrder =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(
      originator: Originator,
      refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: AC): TheFullOrder =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(
      originator: Originator,
      refNum: Option[String],
      pmt: PaymentMethod.Type)(implicit ec: EC, db: DB, ac: AC): TheFullOrder =
    for {
      order ← * <~ getCartByOriginator(originator, refNum)
      _     ← * <~ order.mustBeCart
      valid ← * <~ CartValidator(order).validate()
      resp ← * <~ OrderPayments
              .filter(_.orderRef === order.refNum)
              .byType(pmt)
              .deleteAll(onSuccess = refreshAndFullOrder(order),
                         onFailure = DbResultT.failure(OrderPaymentNotFoundFailure(pmt)))
      _ ← * <~ LogActivity.orderPaymentMethodDeleted(originator, resp, pmt)
    } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)

  def deleteGiftCard(originator: Originator, code: String, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): TheFullOrder =
    for {
      order     ← * <~ getCartByOriginator(originator, refNum)
      _         ← * <~ order.mustBeCart
      giftCard  ← * <~ GiftCards.mustFindByCode(code)
      validated ← * <~ CartValidator(order).validate()
      deleteRes ← * <~ OrderPayments
                   .filter(_.paymentMethodId === giftCard.id)
                   .filter(_.orderRef === order.refNum)
                   .giftCards
                   .deleteAll(onSuccess = refreshAndFullOrder(order),
                              onFailure = DbResultT.failure(
                                  OrderPaymentNotFoundFailure(PaymentMethod.GiftCard)))
      _ ← * <~ LogActivity.orderPaymentMethodDeletedGc(originator, deleteRes, giftCard)
    } yield TheResponse.build(deleteRes, alerts = validated.alerts, warnings = validated.warnings)
}
