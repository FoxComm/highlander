package phoenix.models.payment.storecredit

import java.time.Instant

import core.db._
import core.failures.Failures
import phoenix.models.payment.InStorePaymentStates._
import phoenix.models.payment._
import phoenix.utils.FSM
import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._

case class StoreCreditAdjustment(id: Int = 0,
                                 storeCreditId: Int,
                                 orderPaymentId: Option[Int],
                                 storeAdminId: Option[Int] = None,
                                 debit: Long,
                                 availableBalance: Long,
                                 state: State = Auth,
                                 createdAt: Instant = Instant.now())
    extends FoxModel[StoreCreditAdjustment]
    with InStorePaymentAdjustment[StoreCreditAdjustment]
    with FSM[State, StoreCreditAdjustment] {

  def stateLens = lens[StoreCreditAdjustment].state
  override def updateTo(newModel: StoreCreditAdjustment): Either[Failures, StoreCreditAdjustment] =
    super.transitionModel(newModel)

  def getAmount: Long = debit

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
      new StoreCreditAdjustments(_))
    with ReturningId[StoreCreditAdjustment, StoreCreditAdjustments] {

  val returningLens: Lens[StoreCreditAdjustment, Int] = lens[StoreCreditAdjustment].id

  def filterByStoreCreditId(id: Int): QuerySeq = filter(_.storeCreditId === id)

  def lastAuthByStoreCreditId(id: Int): QuerySeq =
    filterByStoreCreditId(id).filter(_.state === (Auth: State)).sortBy(_.createdAt).take(1)

  object scope {
    implicit class SCAQuerySeqAdditions(val query: QuerySeq) extends QuerySeqAdditions
  }
}
