package services.orders

import scala.concurrent.ExecutionContext

import models.OrderPayments.scope._
import models.{CreditCard, CreditCards, GiftCard, GiftCards, OrderPayment, OrderPayments, Orders, Regions,
PaymentMethod, StoreAdmin, StoreCredit, StoreCredits}
import payloads.{GiftCardPayment, StoreCreditPayment}
import responses.FullOrder.refreshAndFullOrder
import responses.{FullOrder, TheResponse}
import services.{LogActivity, CartValidator, CustomerHasInsufficientStoreCredit, NotFoundFailure400,
OrderPaymentNotFoundFailure, Result}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._

import models.activity.ActivityContext

object OrderPaymentUpdater {

  def addGiftCard(admin: StoreAdmin, refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustBeCart
    gc    ← * <~ GiftCards.mustFindByCode(payload.code, c ⇒ NotFoundFailure400(GiftCard, c))
    _     ← * <~ gc.mustNotBeCart
    _     ← * <~ gc.mustBeActive
    _     ← * <~ gc.mustHaveEnoughBalance(payload.amount)
    _     ← * <~ OrderPayments.create(OrderPayment.build(gc).copy(orderId = order.id, amount = Some(payload.amount)))
    resp  ← * <~ refreshAndFullOrder(order).toXor
    valid ← * <~ CartValidator(order).validate
    _     ← * <~ LogActivity.orderPaymentMethodAddedGc(admin, resp, gc, payload.amount)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def addStoreCredit(admin: StoreAdmin, refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = {
    (for {
      order        ← * <~ Orders.mustFindByRefNum(refNum)
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
        DbResult.fromDbio(delete >> (OrderPayments ++= payments))
      })
      validation   ← * <~ CartValidator(order).validate
      response     ← * <~ refreshAndFullOrder(order).toXor
      _            ← * <~ LogActivity.orderPaymentMethodAddedSc(admin, response, payload.amount)
    } yield TheResponse.build(response, alerts = validation.alerts, warnings = validation.warnings)).runT()
  }

  def addCreditCard(admin: StoreAdmin, refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order   ← * <~ Orders.mustFindByRefNum(refNum)
    _       ← * <~ order.mustBeCart
    cc      ← * <~ CreditCards.mustFindById(id, i ⇒ NotFoundFailure400(CreditCard, i))
    _       ← * <~ cc.mustBeInWallet
    region  ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
    _       ← * <~ OrderPayments.filter(_.orderId === order.id).creditCards.delete
    _       ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = None))
    valid   ← * <~ CartValidator(order).validate
    resp    ← * <~ refreshAndFullOrder(order).toXor
    _       ← * <~ LogActivity.orderPaymentMethodAddedCc(admin, resp, cc, region)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def deleteCreditCard(admin: StoreAdmin, refNum: String)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] =
    deleteCreditCardOrStoreCredit(admin, refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(admin: StoreAdmin, refNum: String)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] =
    deleteCreditCardOrStoreCredit(admin, refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(admin: StoreAdmin, refNum: String, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustBeCart
    valid ← * <~ CartValidator(order).validate
    resp  ← * <~ OrderPayments
                .filter(_.orderId === order.id)
                .byType(pmt).deleteAll(
                  onSuccess = refreshAndFullOrder(order).toXor,
                  onFailure = DbResult.failure(OrderPaymentNotFoundFailure(pmt)))
    _     ← * <~ LogActivity.orderPaymentMethodDeleted(admin, resp, pmt)
  } yield TheResponse.build(resp, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def deleteGiftCard(admin: StoreAdmin, refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order     ← * <~ Orders.mustFindByRefNum(refNum)
    _         ← * <~ order.mustBeCart
    giftCard  ← * <~ GiftCards.mustFindByCode(code)
    validated ← * <~ CartValidator(order).validate
    deleteRes ← * <~ OrderPayments
                      .filter(_.paymentMethodId === giftCard.id)
                      .filter(_.orderId === order.id)
                      .giftCards.deleteAll(
                        onSuccess = refreshAndFullOrder(order).toXor,
                        onFailure = DbResult.failure(OrderPaymentNotFoundFailure(PaymentMethod.GiftCard)))
    _         ← * <~ LogActivity.orderPaymentMethodDeletedGc(admin, deleteRes, giftCard)
  } yield TheResponse.build(deleteRes, alerts = validated.alerts, warnings = validated.warnings)).runT()
}
