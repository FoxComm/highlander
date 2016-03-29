package services.orders

import cats.implicits._
import models.order._
import models.traits.Originator
import OrderPayments.scope._
import models.location.Regions
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import payloads.{GiftCardPayment, StoreCreditPayment}
import responses.order.FullOrder
import FullOrder.refreshAndFullOrder
import failures.GiftCardFailures.GiftCardPaymentAlreadyAdded
import failures.{NotFoundFailure400, OpenTransactionsFailure}
import failures.OrderFailures.OrderPaymentNotFoundFailure
import failures.StoreCreditFailures.CustomerHasInsufficientStoreCredit
import responses.TheResponse
import services.{CartValidator, LogActivity, Result}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.aliases._

object OrderPaymentUpdater {

  def addGiftCard(originator: Originator, payload: GiftCardPayment, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[FullOrder.Root]] = (for {
    order  ← * <~ getCartByOriginator(originator, refNum)
    _      ← * <~ order.mustBeCart
    gc     ← * <~ GiftCards.mustFindByCode(payload.code, c ⇒ NotFoundFailure400(GiftCard, c))
    _      ← * <~ gc.mustNotBeCart
    _      ← * <~ gc.mustBeActive
    _      ← * <~ GiftCardAdjustments.lastAuthByGiftCardId(gc.id).one.mustNotFindOr(OpenTransactionsFailure)
    _      ← * <~ OrderPayments.giftCards.filter(_.paymentMethodId === gc.id)
                                        .filter(_.orderId === order.id)
                                        .one.mustNotFindOr(GiftCardPaymentAlreadyAdded(order.refNum, payload.code))
    amount = selectGiftCardAmount(payload, gc)
    _      ← * <~ gc.mustHaveEnoughBalance(amount)
    _      ← * <~ OrderPayments.create(OrderPayment.build(gc).copy(orderId = order.id, amount = amount.some))
    resp   ← * <~ refreshAndFullOrder(order).toXor
    valid  ← * <~ CartValidator(order).validate()
    _      ← * <~ LogActivity.orderPaymentMethodAddedGc(originator, resp, gc, amount)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  private def selectGiftCardAmount(payload: GiftCardPayment, gc: GiftCard): Int = payload.amount match {
    case Some(amount) ⇒ amount
    case _            ⇒ gc.availableBalance
  }

  def addStoreCredit(originator: Originator, payload: StoreCreditPayment, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[FullOrder.Root]] = {
    (for {
      order        ← * <~ getCartByOriginator(originator, refNum)
      _            ← * <~ order.mustBeCart
      storeCredits ← * <~ StoreCredits.findAllActiveByCustomerId(order.customerId).result.toXor
      reqAmount = payload.amount
      available = storeCredits.map(_.availableBalance).sum
      actions      ← * <~ (if (available < reqAmount) {
        DbResult.failure(CustomerHasInsufficientStoreCredit(id = order.customerId, has = available, want = reqAmount))
      } else {
        val delete = OrderPayments.filter(_.orderId === order.id).storeCredits.delete
        val payments = StoreCredit.processFifo(storeCredits.toList, reqAmount).map { case (sc, amount) ⇒
          OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
        }
        delete.flatMap(_ ⇒ OrderPayments.createAll(payments))
      })
      validation   ← * <~ CartValidator(order).validate()
      response     ← * <~ refreshAndFullOrder(order).toXor
      _            ← * <~ LogActivity.orderPaymentMethodAddedSc(originator, response, payload.amount)
    } yield TheResponse.build(response, alerts = validation.alerts, warnings = validation.warnings)).runTxn()
  }

  def addCreditCard(originator: Originator, id: Int, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[FullOrder.Root]] = (for {
    order   ← * <~ getCartByOriginator(originator, refNum)
    _       ← * <~ order.mustBeCart
    cc      ← * <~ CreditCards.mustFindById400(id)
    _       ← * <~ cc.mustBeInWallet
    region  ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
    _       ← * <~ OrderPayments.filter(_.orderId === order.id).creditCards.delete
    _       ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = None))
    valid   ← * <~ CartValidator(order).validate()
    resp    ← * <~ refreshAndFullOrder(order).toXor
    _       ← * <~ LogActivity.orderPaymentMethodAddedCc(originator, resp, cc, region)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def deleteCreditCard(originator: Originator, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[FullOrder.Root]] =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(originator: Originator, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[FullOrder.Root]] =
    deleteCreditCardOrStoreCredit(originator, refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(originator: Originator, refNum: Option[String],
    pmt: PaymentMethod.Type)(implicit ec: EC, db: DB, ac: AC):
    Result[TheResponse[FullOrder.Root]] = (for {

    order ← * <~ getCartByOriginator(originator, refNum)
    _     ← * <~ order.mustBeCart
    valid ← * <~ CartValidator(order).validate()
    resp  ← * <~ OrderPayments
                .filter(_.orderId === order.id)
                .byType(pmt).deleteAll(
                  onSuccess = refreshAndFullOrder(order).toXor,
                  onFailure = DbResult.failure(OrderPaymentNotFoundFailure(pmt)))
    _     ← * <~ LogActivity.orderPaymentMethodDeleted(originator, resp, pmt)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def deleteGiftCard(originator: Originator, code: String, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[FullOrder.Root]] = (for {
    order     ← * <~ getCartByOriginator(originator, refNum)
    _         ← * <~ order.mustBeCart
    giftCard  ← * <~ GiftCards.mustFindByCode(code)
    validated ← * <~ CartValidator(order).validate()
    deleteRes ← * <~ OrderPayments
                      .filter(_.paymentMethodId === giftCard.id)
                      .filter(_.orderId === order.id)
                      .giftCards.deleteAll(
                        onSuccess = refreshAndFullOrder(order).toXor,
                        onFailure = DbResult.failure(OrderPaymentNotFoundFailure(PaymentMethod.GiftCard)))
    _         ← * <~ LogActivity.orderPaymentMethodDeletedGc(originator, deleteRes, giftCard)
  } yield TheResponse.build(deleteRes, alerts = validated.alerts, warnings = validated.warnings)).runTxn()
}
