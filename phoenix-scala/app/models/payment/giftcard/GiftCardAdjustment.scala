package models.payment.giftcard

import java.time.Instant

import cats.data.Xor
import failures.Failures
import models.cord.OrderPayment
import models.payment._
import models.payment.InStorePaymentStates._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.FSM

case class GiftCardAdjustment(id: Int = 0,
                              giftCardId: Int,
                              orderPaymentId: Option[Int],
                              storeAdminId: Option[Int] = None,
                              credit: Int,
                              debit: Int,
                              availableBalance: Int,
                              state: State = Auth,
                              createdAt: Instant = Instant.now())
    extends FoxModel[GiftCardAdjustment]
    with InStorePaymentAdjustment[GiftCardAdjustment]
    with FSM[InStorePaymentStates.State, GiftCardAdjustment] {

  def stateLens = lens[GiftCardAdjustment].state
  override def updateTo(newModel: GiftCardAdjustment): Failures Xor GiftCardAdjustment =
    super.transitionModel(newModel)

  def getAmount: Int = if (credit > 0) credit else -debit

  val fsm: Map[State, Set[State]] = Map(
      Auth → Set(Canceled, Capture)
  )
}

object GiftCardAdjustment {

  def build(gc: GiftCard, orderPayment: OrderPayment): GiftCardAdjustment =
    GiftCardAdjustment(giftCardId = gc.id,
                       orderPaymentId = Some(orderPayment.id),
                       credit = 0,
                       debit = 0,
                       availableBalance = gc.availableBalance)
}

class GiftCardAdjustments(tag: Tag)
    extends InStorePaymentAdjustmentTable[GiftCardAdjustment](tag, "gift_card_adjustments") {

  def giftCardId = column[Int]("gift_card_id")
  def credit     = column[Int]("credit")

  def * =
    (id,
     giftCardId,
     orderPaymentId,
     storeAdminId,
     credit,
     debit,
     availableBalance,
     state,
     createdAt) <> ((GiftCardAdjustment.apply _).tupled, GiftCardAdjustment.unapply)
}

object GiftCardAdjustments
    extends InStorePaymentAdjustmentQueries[GiftCardAdjustment, GiftCardAdjustments](
        new GiftCardAdjustments(_))
    with ReturningId[GiftCardAdjustment, GiftCardAdjustments] {

  val returningLens: Lens[GiftCardAdjustment, Int] = lens[GiftCardAdjustment].id

  def filterByGiftCardId(id: Int): QuerySeq = filter(_.giftCardId === id)

  def lastAuthByGiftCardId(id: Int): QuerySeq =
    filterByGiftCardId(id).filter(_.state === (Auth: State)).sortBy(_.createdAt).take(1)

  object scope {
    implicit class GCAQuerySeqAdditions(val query: QuerySeq) extends QuerySeqAdditions
  }
}
