package services.rmas

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import cats.implicits._
import Helpers._
import models.RmaPayments.scope._
import models._
import payloads.{RmaPaymentPayload, RmaCcPaymentPayload}
import responses.RmaResponse
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick._
import utils.Slick.implicits._

object RmaPaymentUpdater {
  def addCreditCard(refNum: String, payload: RmaCcPaymentPayload)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      cc        ← * <~ CreditCards.filter(_.id === payload.creditCardId).one
        .mustFindOr(NotFoundFailure404(CreditCard, payload.creditCardId))
      deleteAll ← * <~ deleteCc(rma.id).toXor
      ccRefund  ← * <~ RmaPayments.create(RmaPayment.build(cc, rma.id, payload.amount))
      response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
    } yield response).runT()

  def addGiftCard(admin: StoreAdmin, refNum: String, payload: RmaPaymentPayload)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      origin    ← * <~ GiftCardRefunds.create(GiftCardRefund(rmaId = rma.id))
      gc        ← * <~ GiftCards.create(GiftCard.buildRmaProcess(originId = origin.id, currency = Currency.USD))
      pmt       ← * <~ RmaPayments.create(RmaPayment.build(gc, rma.id, payload.amount))
      response  ← * <~ fullRma(Rmas.findByRefNum(refNum)).toXor
    } yield response).runT()

  def addStoreCredit(admin: StoreAdmin, refNum: String, payload: RmaPaymentPayload)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
      _         ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      deleteAll ← * <~ deleteGc(rma.id).toXor
      origin    ← * <~ StoreCreditRefunds.create(StoreCreditRefund(rmaId = rma.id))
      sc        ← * <~ StoreCredits.create(StoreCredit.buildRmaProcess(customerId = rma.customerId, originId = origin.id,
        currency = Currency.USD))
      pmt       ← * <~ RmaPayments.create(RmaPayment.build(sc, rma.id, payload.amount))
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
