package services.rmas

import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.rma._
import RmaPayments.scope._
import models.StoreAdmin
import payloads.RmaPayloads.RmaPaymentPayload
import responses.RmaResponse
import responses.RmaResponse.Root
import services.Result
import services.rmas.Helpers._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object RmaPaymentUpdater {
  def addCreditCard(refNum: String, payload: RmaPaymentPayload)(implicit ec: EC, db: DB): Result[Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      cc        ← * <~ CreditCards.mustFindById404(payment.paymentMethodId)
      deleteAll ← * <~ deleteCc(rma.id).toXor
      ccRefund  ← * <~ RmaPayments.create(RmaPayment.build(cc, rma.id, payload.amount, payment.currency))
      updated   ← * <~ Rmas.refresh(rma).toXor
      response  ← * <~ RmaResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def addGiftCard(refNum: String, payload: RmaPaymentPayload)
    (implicit ec: EC, db: DB): Result[Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      origin    ← * <~ GiftCardRefunds.create(GiftCardRefund(rmaId = rma.id))
      gc        ← * <~ GiftCards.create(GiftCard.buildRmaProcess(origin.id, payment.currency))
      pmt       ← * <~ RmaPayments.create(RmaPayment.build(gc, rma.id, payload.amount, payment.currency))
      updated   ← * <~ Rmas.refresh(rma).toXor
      response  ← * <~ RmaResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def addStoreCredit(refNum: String, payload: RmaPaymentPayload)
    (implicit ec: EC, db: DB): Result[Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      origin    ← * <~ StoreCreditRefunds.create(StoreCreditRefund(rmaId = rma.id))

      storeCredit = StoreCredit.buildRmaProcess(rma.customerId, origin.id, payment.currency)
      sc        ← * <~ StoreCredits.create(storeCredit)
      pmt       ← * <~ RmaPayments.create(RmaPayment.build(sc, rma.id, payload.amount, payment.currency))
      updated   ← * <~ Rmas.refresh(rma).toXor
      response  ← * <~ RmaResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def deleteCreditCard(refNum: String)(implicit ec: EC, db: DB): Result[Root] = (for {
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    deleteAll ← * <~ deleteCc(rma.id).toXor
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(rma).toXor
  } yield response).runTxn()

  def deleteGiftCard(refNum: String)(implicit ec: EC, db: DB): Result[Root] = (for {
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    deleteAll ← * <~ deleteGc(rma.id).toXor
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(rma).toXor
  } yield response).runTxn()

  def deleteStoreCredit(refNum: String)(implicit ec: EC, db: DB): Result[Root] = (for {
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    deleteAll ← * <~ deleteSc(rma.id).toXor
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(rma).toXor
  } yield response).runTxn()

  private def deleteCc(rmaId: Int)(implicit ec: EC) = {
    RmaPayments.filter(_.rmaId === rmaId).creditCards.result.flatMap { seq ⇒
      DBIO.sequence(seq.map { pmt ⇒ RmaPayments.filter(_.id === pmt.id).delete })
    }
  }

  private def deleteGc(rmaId: Int)(implicit ec: EC) = {
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

  private def deleteSc(rmaId: Int)(implicit ec: EC) = {
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
