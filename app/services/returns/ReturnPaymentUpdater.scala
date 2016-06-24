package services.returns

import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.returns._
import ReturnPayments.scope._
import payloads.ReturnPayloads.ReturnPaymentPayload
import responses.ReturnResponse
import responses.ReturnResponse.Root
import services.Result
import services.returns.Helpers._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object ReturnPaymentUpdater {
  def addCreditCard(refNum: String, payload: ReturnPaymentPayload)(implicit ec: EC,
                                                                   db: DB): Result[Root] =
    (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      cc        ← * <~ CreditCards.mustFindById404(payment.paymentMethodId)
      deleteAll ← * <~ deleteCc(rma.id).toXor
      ccRefund ← * <~ ReturnPayments.create(
                    ReturnPayment.build(cc, rma.id, payload.amount, payment.currency))
      updated  ← * <~ Returns.refresh(rma).toXor
      response ← * <~ ReturnResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def addGiftCard(refNum: String, payload: ReturnPaymentPayload)(implicit ec: EC,
                                                                 db: DB): Result[Root] =
    (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      origin    ← * <~ GiftCardRefunds.create(GiftCardRefund(returnId = rma.id))
      gc        ← * <~ GiftCards.create(GiftCard.buildRmaProcess(origin.id, payment.currency))
      pmt ← * <~ ReturnPayments.create(
               ReturnPayment.build(gc, rma.id, payload.amount, payment.currency))
      updated  ← * <~ Returns.refresh(rma).toXor
      response ← * <~ ReturnResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def addStoreCredit(refNum: String, payload: ReturnPaymentPayload)(implicit ec: EC,
                                                                    db: DB): Result[Root] =
    (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      payment   ← * <~ mustFindCcPaymentsByOrderId(rma.orderId)
      origin    ← * <~ StoreCreditRefunds.create(StoreCreditRefund(returnId = rma.id))

      storeCredit = StoreCredit.buildRmaProcess(rma.customerId, origin.id, payment.currency)
      sc ← * <~ StoreCredits.create(storeCredit)
      pmt ← * <~ ReturnPayments.create(
               ReturnPayment.build(sc, rma.id, payload.amount, payment.currency))
      updated  ← * <~ Returns.refresh(rma).toXor
      response ← * <~ ReturnResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def deleteCreditCard(refNum: String)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteCc(rma.id).toXor
      updated   ← * <~ Returns.refresh(rma).toXor
      response  ← * <~ ReturnResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def deleteGiftCard(refNum: String)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      updated   ← * <~ Returns.refresh(rma).toXor
      response  ← * <~ ReturnResponse.fromRma(rma).toXor
    } yield response).runTxn()

  def deleteStoreCredit(refNum: String)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteSc(rma.id).toXor
      updated   ← * <~ Returns.refresh(rma).toXor
      response  ← * <~ ReturnResponse.fromRma(rma).toXor
    } yield response).runTxn()

  private def deleteCc(returnId: Int)(implicit ec: EC) = {
    ReturnPayments.filter(_.returnId === returnId).creditCards.result.flatMap { seq ⇒
      DBIO.sequence(seq.map { pmt ⇒
        ReturnPayments.filter(_.id === pmt.id).delete
      })
    }
  }

  private def deleteGc(returnId: Int)(implicit ec: EC) = {
    val query = ReturnPayments
      .filter(_.returnId === returnId)
      .giftCards
      .join(GiftCards)
      .on(_.paymentMethodId === _.id)
      .join(GiftCardRefunds)
      .on(_._2.originId === _.id)
      .result

    query.flatMap { seq ⇒
      val deleteAll = seq.map {
        case ((pmt, giftCard), gcOrigin) ⇒
          for {
            origin  ← GiftCardRefunds.filter(_.id === gcOrigin.id).delete
            gc      ← GiftCards.filter(_.id === giftCard.id).delete
            payment ← ReturnPayments.filter(_.id === pmt.id).delete
          } yield ()
      }

      DBIO.sequence(deleteAll)
    }
  }

  private def deleteSc(returnId: Int)(implicit ec: EC) = {
    val query = ReturnPayments
      .filter(_.returnId === returnId)
      .storeCredits
      .join(StoreCredits)
      .on(_.paymentMethodId === _.id)
      .join(StoreCreditRefunds)
      .on(_._2.originId === _.id)
      .result

    query.flatMap { seq ⇒
      val deleteAll = seq.map {
        case ((pmt, storeCredit), scOrigin) ⇒
          for {
            origin  ← StoreCreditRefunds.filter(_.id === scOrigin.id).delete
            sc      ← StoreCredits.filter(_.id === storeCredit.id).delete
            payment ← ReturnPayments.filter(_.id === pmt.id).delete
          } yield ()
      }

      DBIO.sequence(deleteAll)
    }
  }
}
