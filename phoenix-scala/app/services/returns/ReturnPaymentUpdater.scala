package services.returns

import models.account.Scope
import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.returns.ReturnPayments.scope._
import models.returns._
import payloads.ReturnPayloads.ReturnPaymentPayload
import responses.ReturnResponse
import responses.ReturnResponse.Root
import services.returns.Helpers._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnPaymentUpdater {
  def addCreditCard(refNum: String, payload: ReturnPaymentPayload)(implicit ec: EC,
                                                                   db: DB): DbResultT[Root] =
    for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      payment   ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
      cc        ← * <~ CreditCards.mustFindById404(payment.paymentMethodId)
      deleteAll ← * <~ deleteCc(rma.id)
      ccRefund ← * <~ ReturnPayments.create(
        ReturnPayment.build(cc, rma.id, payload.amount, payment.currency))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  def addGiftCard(refNum: String, payload: ReturnPaymentPayload)(implicit ec: EC,
                                                                 db: DB,
                                                                 au: AU): DbResultT[Root] =
    for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id)
      payment   ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
      origin    ← * <~ GiftCardRefunds.create(GiftCardRefund(returnId = rma.id))
      gc        ← * <~ GiftCards.create(GiftCard.buildRmaProcess(origin.id, payment.currency))
      pmt ← * <~ ReturnPayments.create(
        ReturnPayment.build(gc, rma.id, payload.amount, payment.currency))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  def addStoreCredit(refNum: String, payload: ReturnPaymentPayload)(implicit ec: EC,
                                                                    db: DB,
                                                                    au: AU): DbResultT[Root] =
    for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id)
      payment   ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
      origin    ← * <~ StoreCreditRefunds.create(StoreCreditRefund(returnId = rma.id))
      sc ← * <~ StoreCredits.create(
        StoreCredit(accountId = rma.accountId,
                    scope = Scope.current,
                    originId = origin.id,
                    originType = StoreCredit.RmaProcess,
                    currency = payment.currency,
                    originalBalance = 0))
      pmt ← * <~ ReturnPayments.create(
        ReturnPayment.build(sc, rma.id, payload.amount, payment.currency))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  def deleteCreditCard(refNum: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteCc(rma.id)
      updated   ← * <~ Returns.refresh(rma)
      response  ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  def deleteGiftCard(refNum: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id)
      updated   ← * <~ Returns.refresh(rma)
      response  ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  def deleteStoreCredit(refNum: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    (for {
      rma       ← * <~ mustFindPendingReturnByRefNum(refNum)
      deleteAll ← * <~ deleteSc(rma.id)
      updated   ← * <~ Returns.refresh(rma)
      response  ← * <~ ReturnResponse.fromRma(rma)
    } yield response)

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
