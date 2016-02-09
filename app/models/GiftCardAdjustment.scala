package models

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.data.Xor
import com.pellucid.sealerate
import models.GiftCardAdjustment.{Auth, State}
import models.Notes._
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import services.Failures
import slick.lifted.ColumnOrdered
import utils.CustomDirectives.SortAndPage
import utils.{CustomDirectives, ADT, FSM, GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.Slick.implicits._

final case class GiftCardAdjustment(id: Int = 0, giftCardId: Int, orderPaymentId: Option[Int],
  storeAdminId: Option[Int] = None, credit: Int, debit: Int, availableBalance: Int, state: State = Auth, 
  createdAt: Instant = Instant.now())
  extends ModelWithIdParameter[GiftCardAdjustment]
  with FSM[GiftCardAdjustment.State, GiftCardAdjustment] {

  import GiftCardAdjustment._

  def stateLens = GenLens[GiftCardAdjustment](_.state)
  override def updateTo(newModel: GiftCardAdjustment): Failures Xor GiftCardAdjustment = super.transitionModel(newModel)

  def getAmount: Int = if (credit > 0) credit else -debit

  val fsm: Map[State, Set[State]] = Map(
    Auth → Set(Canceled, Capture)
  )
}

object GiftCardAdjustment {
  sealed trait State
  case object Auth extends State
  case object Canceled extends State
  case object Capture extends State
  case object CancellationCapture extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  def build(gc: GiftCard, orderPayment: OrderPayment): GiftCardAdjustment =
    GiftCardAdjustment(giftCardId = gc.id, orderPaymentId = Some(orderPayment.id), credit = 0, debit = 0,
      availableBalance = gc.availableBalance)
}

class GiftCardAdjustments(tag: Tag)
  extends GenericTable.TableWithId[GiftCardAdjustment](tag, "gift_card_adjustments")
   {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")
  def orderPaymentId = column[Option[Int]]("order_payment_id")
  def storeAdminId = column[Option[Int]]("store_admin_id")
  def credit = column[Int]("credit")
  def debit = column[Int]("debit")
  def availableBalance = column[Int]("available_balance")
  def state = column[GiftCardAdjustment.State]("state")
  def createdAt = column[Instant]("created_at")

  def * = (id, giftCardId, orderPaymentId, storeAdminId, credit, debit, availableBalance,
    state, createdAt) <> ((GiftCardAdjustment.apply _).tupled, GiftCardAdjustment.unapply)

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id.?)
}

object GiftCardAdjustments extends TableQueryWithId[GiftCardAdjustment, GiftCardAdjustments](
  idLens = GenLens[GiftCardAdjustment](_.id)
  )(new GiftCardAdjustments(_)){

  import GiftCardAdjustment._

  def matchSortColumn(s: CustomDirectives.Sort, adj: GiftCardAdjustments): ColumnOrdered[_] = {
    s.sortColumn match {
      case "id"               ⇒ if (s.asc) adj.id.asc               else adj.id.desc
      case "giftCardId"       ⇒ if (s.asc) adj.giftCardId.asc       else adj.giftCardId.desc
      case "orderPaymentId"   ⇒ if (s.asc) adj.orderPaymentId.asc   else adj.orderPaymentId.desc
      case "storeAdminId"     ⇒ if (s.asc) adj.storeAdminId.asc     else adj.storeAdminId.desc
      case "credit"           ⇒ if (s.asc) adj.credit.asc           else adj.credit.desc
      case "debit"            ⇒ if (s.asc) adj.debit.asc            else adj.debit.desc
      case "availableBalance" ⇒ if (s.asc) adj.availableBalance.asc else adj.availableBalance.desc
      case "state"            ⇒ if (s.asc) adj.state.asc            else adj.state.desc
      case "createdAt"        ⇒ if (s.asc) adj.createdAt.asc        else adj.createdAt.desc
      case other              ⇒ invalidSortColumn(other)
    }
  }

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    query.withMetadata.sortAndPageIfNeeded { (s, adj) ⇒ matchSortColumn(s, adj) }

  def filterByGiftCardId(id: Int): QuerySeq = filter(_.giftCardId === id)

  def lastAuthByGiftCardId(id: Int): QuerySeq =
    filterByGiftCardId(id).filter(_.state === (Auth: State)).sortBy(_.createdAt).take(1)

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.state).update(Canceled)
}
