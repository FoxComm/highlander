package models.payment.storecredit

import java.time.Instant

import cats.data.Xor
import failures.Failures
import models.payment.InStorePaymentStates._
import models.payment._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.FSM
import utils.db._

case class StoreCreditAdjustment(id: Int = 0,
                                 storeCreditId: Int,
                                 orderPaymentId: Option[Int],
                                 storeAdminId: Option[Int] = None,
                                 debit: Int,
                                 availableBalance: Int,
                                 state: State = Auth,
                                 createdAt: Instant = Instant.now())
    extends FoxModel[StoreCreditAdjustment]
    with InStorePaymentAdjustment[StoreCreditAdjustment]
    with FSM[State, StoreCreditAdjustment] {

  def stateLens = lens[StoreCreditAdjustment].state
  override def updateTo(newModel: StoreCreditAdjustment): Failures Xor StoreCreditAdjustment =
    super.transitionModel(newModel)

  def getAmount: Int = debit

  val fsm: Map[State, Set[State]] = Map(
      Auth → Set(Canceled, Capture)
  )
}

class StoreCreditAdjustments(tag: Tag)
    extends InStorePaymentAdjustmentTable[StoreCreditAdjustment](tag, "store_credit_adjustments") {

  def storeCreditId = column[Int]("store_credit_id")

  def * =
    (id, storeCreditId, orderPaymentId, storeAdminId, debit, availableBalance, state, createdAt) <> ((StoreCreditAdjustment.apply _).tupled, StoreCreditAdjustment.unapply)

  def storeCredit = foreignKey(StoreCredits.tableName, storeCreditId, StoreCredits)(_.id)
}

object StoreCreditAdjustments
    extends InStorePaymentAdjustmentQueries[StoreCreditAdjustment, StoreCreditAdjustments](
        new StoreCreditAdjustments(_)) {

  def filterByStoreCreditId(id: Int): QuerySeq = filter(_.storeCreditId === id)

  def lastAuthByStoreCreditId(id: Int): QuerySeq =
    filterByStoreCreditId(id).filter(_.state === (Auth: State)).sortBy(_.createdAt).take(1)

  private val rootLens = lens[StoreCreditAdjustment]

  val returningLens: Lens[StoreCreditAdjustment, (Int, Int)] = rootLens.id ~ rootLens.availableBalance

  object scope {
    implicit class SCAQuerySeqAdditions(val query: QuerySeq) extends QuerySeqAdditions
  }
}
