package models.payment.giftcard

import java.time.Instant

import cats.data.Xor
import com.pellucid.sealerate
import failures.Failures
import models.cord.{OrderPayment, OrderPayments}
import models.payment.giftcard.GiftCardAdjustment._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.db._
import utils.{ADT, FSM}

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
    with FSM[GiftCardAdjustment.State, GiftCardAdjustment] {

  import GiftCardAdjustment._

  def stateLens = lens[GiftCardAdjustment].state
  override def updateTo(newModel: GiftCardAdjustment): Failures Xor GiftCardAdjustment =
    super.transitionModel(newModel)

  def getAmount: Int = if (credit > 0) credit else -debit

  val fsm: Map[State, Set[State]] = Map(
      Auth → Set(Canceled, Capture)
  )
}

object GiftCardAdjustment {
  sealed trait State
  case object Auth                extends State
  case object Canceled            extends State
  case object Capture             extends State
  case object CancellationCapture extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  def build(gc: GiftCard, orderPayment: OrderPayment): GiftCardAdjustment =
    GiftCardAdjustment(giftCardId = gc.id,
                       orderPaymentId = Some(orderPayment.id),
                       credit = 0,
                       debit = 0,
                       availableBalance = gc.availableBalance)
}

class GiftCardAdjustments(tag: Tag)
    extends FoxTable[GiftCardAdjustment](tag, "gift_card_adjustments") {

  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId       = column[Int]("gift_card_id")
  def orderPaymentId   = column[Option[Int]]("order_payment_id")
  def storeAdminId     = column[Option[Int]]("store_admin_id")
  def credit           = column[Int]("credit")
  def debit            = column[Int]("debit")
  def availableBalance = column[Int]("available_balance")
  def state            = column[GiftCardAdjustment.State]("state")
  def createdAt        = column[Instant]("created_at")

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

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id.?)
}

object GiftCardAdjustments
    extends FoxTableQuery[GiftCardAdjustment, GiftCardAdjustments](new GiftCardAdjustments(_))
    with ReturningId[GiftCardAdjustment, GiftCardAdjustments] {

  val returningLens: Lens[GiftCardAdjustment, Int] = lens[GiftCardAdjustment].id

  import GiftCardAdjustment._

  def filterByGiftCardId(id: Int): QuerySeq = filter(_.giftCardId === id)

  def lastAuthByGiftCardId(id: Int): QuerySeq =
    filterByGiftCardId(id).filter(_.state === (Auth: State)).sortBy(_.createdAt).take(1)

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.state).update(Canceled)

  def authorizedOrderPayments(orderPaymentIds: Seq[Int]): QuerySeq =
    filter(adj ⇒ adj.orderPaymentId.inSet(orderPaymentIds) && adj.state === (Auth: State))

  def authorizedOrderPayment(orderPaymentId: Int): QuerySeq =
    filter(adj ⇒ adj.orderPaymentId === orderPaymentId && adj.state === (Auth: State))

  object scope {

    implicit class GCAQuerySeqAdditions(query: QuerySeq) {
      def cancel(): DBIO[Int] = query.map(_.state).update(Canceled)
    }
  }
}
