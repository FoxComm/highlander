package models.payment.storecredit

import java.time.Instant

import cats.data.Xor
import com.pellucid.sealerate
import failures.Failures
import models.cord.OrderPayments
import models.payment.storecredit.StoreCreditAdjustment._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.db._
import utils.{ADT, FSM}

case class StoreCreditAdjustment(id: Int = 0,
                                 storeCreditId: Int,
                                 orderPaymentId: Option[Int],
                                 storeAdminId: Option[Int] = None,
                                 debit: Int,
                                 availableBalance: Int,
                                 state: State = Auth,
                                 createdAt: Instant = Instant.now())
    extends FoxModel[StoreCreditAdjustment]
    with FSM[StoreCreditAdjustment.State, StoreCreditAdjustment] {

  import StoreCreditAdjustment._

  def stateLens = lens[StoreCreditAdjustment].state
  override def updateTo(newModel: StoreCreditAdjustment): Failures Xor StoreCreditAdjustment =
    super.transitionModel(newModel)

  def getAmount: Int = debit

  val fsm: Map[State, Set[State]] = Map(
      Auth → Set(Canceled, Capture)
  )
}

object StoreCreditAdjustment {
  sealed trait State
  case object Auth                extends State
  case object Canceled            extends State
  case object Capture             extends State
  case object CancellationCapture extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class StoreCreditAdjustments(tag: Tag)
    extends FoxTable[StoreCreditAdjustment](tag, "store_credit_adjustments") {

  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeCreditId    = column[Int]("store_credit_id")
  def storeAdminId     = column[Option[Int]]("store_admin_id")
  def orderPaymentId   = column[Option[Int]]("order_payment_id")
  def debit            = column[Int]("debit")
  def availableBalance = column[Int]("available_balance")
  def state            = column[StoreCreditAdjustment.State]("state")
  def createdAt        = column[Instant]("created_at")

  def * =
    (id, storeCreditId, orderPaymentId, storeAdminId, debit, availableBalance, state, createdAt) <> ((StoreCreditAdjustment.apply _).tupled, StoreCreditAdjustment.unapply)

  def payment     = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id.?)
  def storeCredit = foreignKey(StoreCredits.tableName, storeCreditId, StoreCredits)(_.id)
}

object StoreCreditAdjustments
    extends FoxTableQuery[StoreCreditAdjustment, StoreCreditAdjustments](
        new StoreCreditAdjustments(_))
    with ReturningId[StoreCreditAdjustment, StoreCreditAdjustments] {

  val returningLens: Lens[StoreCreditAdjustment, Int] = lens[StoreCreditAdjustment].id

  import StoreCreditAdjustment._

  def filterByStoreCreditId(id: Int): QuerySeq = filter(_.storeCreditId === id)

  def lastAuthByStoreCreditId(id: Int): QuerySeq =
    filterByStoreCreditId(id).filter(_.state === (Auth: State)).sortBy(_.createdAt).take(1)

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.state).update(Canceled)

  def authorizedOrderPayments(orderPaymentIds: Seq[Int]): QuerySeq =
    filter(adj ⇒ adj.orderPaymentId.inSet(orderPaymentIds) && adj.state === (Auth: State))

  def authorizedOrderPayment(orderPaymentId: Int): QuerySeq =
    filter(adj ⇒ adj.orderPaymentId === orderPaymentId && adj.state === (Auth: State))

  object scope {

    implicit class SCAQuerySeqAdditions(query: QuerySeq) {
      def cancel(): DBIO[Int] = query.map(_.state).update(Canceled)
    }
  }
}
