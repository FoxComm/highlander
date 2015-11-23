package services.rmas

import scala.concurrent.ExecutionContext

import Helpers._
import models.OrderPayments.scope._
import models.RmaPayments.scope._
import models._
import payloads.RmaPaymentPayload
import responses.RmaResponse
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

object RmaPaymentUpdater {
  def addCreditCard(refNum: String, payload: RmaPaymentPayload)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      cc        ← * <~ CreditCards.mustFindById(payment.paymentMethodId)
      deleteAll ← * <~ deleteCc(rma.id).toXor
      ccRefund  ← * <~ RmaPayments.create(RmaPayment.build(cc, rma.id, payload.amount, payment.currency))
      response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
    } yield response).runT()

  def addGiftCard(admin: StoreAdmin, refNum: String, payload: RmaPaymentPayload)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      origin    ← * <~ GiftCardRefunds.create(GiftCardRefund(rmaId = rma.id))
      gc        ← * <~ GiftCards.create(GiftCard.buildRmaProcess(origin.id, payment.currency))
      pmt       ← * <~ RmaPayments.create(RmaPayment.build(gc, rma.id, payload.amount, payment.currency))
      response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
    } yield response).runT()

  def addStoreCredit(admin: StoreAdmin, refNum: String, payload: RmaPaymentPayload)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      origin    ← * <~ StoreCreditRefunds.create(StoreCreditRefund(rmaId = rma.id))

      storeCredit = StoreCredit.buildRmaProcess(rma.customerId, origin.id, payment.currency)
      sc        ← * <~ StoreCredits.create(storeCredit)
      pmt       ← * <~ RmaPayments.create(RmaPayment.build(sc, rma.id, payload.amount, payment.currency))
      response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
    } yield response).runT()

  def deleteCreditCard(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    deleteAll ← * <~ deleteCc(rma.id).toXor
    response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
  } yield response).runT()

  def deleteGiftCard(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    deleteAll ← * <~ deleteGc(rma.id).toXor
    response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
  } yield response).runT()

  def deleteStoreCredit(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    deleteAll ← * <~ deleteSc(rma.id).toXor
    response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
  } yield response).runT()

  private def deleteCc(rmaId: Int)(implicit ec: ExecutionContext, db: Database) = {
    RmaPayments.filter(_.rmaId === rmaId).creditCards.result.flatMap { seq ⇒
      DBIO.sequence(seq.map { pmt ⇒ RmaPayments.filter(_.id === pmt.id).delete })
    }
  }

  private def deleteGc(rmaId: Int)(implicit ec: ExecutionContext, db: Database) = {
    val query = RmaPayments.filter(_.rmaId === rmaId).giftCards
      .join(GiftCards).on(_.paymentMethodId === _.id)
      .join(GiftCardRefunds).on(_._2.originId === _.id)
      .result

    query.flatMap { seq ⇒
      val deleteAll = seq.map { case ((pmt, giftCard), gcOrigin) ⇒
        for {
          origin ← GiftCardRefunds.filter(_.id === gcOrigin.id).delete
          gc ← GiftCards.filter(_.id === giftCard.id).delete
          payment ← RmaPayments.filter(_.id === pmt.id).delete
        } yield ()
      }

      DBIO.sequence(deleteAll)
    }
  }

  private def deleteSc(rmaId: Int)(implicit ec: ExecutionContext, db: Database) = {
    val query = RmaPayments.filter(_.rmaId === rmaId).storeCredits
      .join(StoreCredits).on(_.paymentMethodId === _.id)
      .join(StoreCreditRefunds).on(_._2.originId === _.id)
      .result

    query.flatMap { seq ⇒
      val deleteAll = seq.map { case ((pmt, storeCredit), scOrigin) ⇒
        for {
          origin ← StoreCreditRefunds.filter(_.id === scOrigin.id).delete
          sc ← StoreCredits.filter(_.id === storeCredit.id).delete
          payment ← RmaPayments.filter(_.id === pmt.id).delete
        } yield ()
      }

      DBIO.sequence(deleteAll)
    }
  }
}
