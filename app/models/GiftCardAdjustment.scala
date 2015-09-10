package models

import scala.concurrent.{Future, ExecutionContext}

import com.pellucid.sealerate
import models.GiftCardAdjustment.{Auth, Status}
import models.Notes._
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.Slick.implicits._

final case class GiftCardAdjustment(id: Int = 0, giftCardId: Int, orderPaymentId: Int,
  credit: Int, debit: Int, status: Status = Auth)
  extends ModelWithIdParameter
  with FSM[GiftCardAdjustment.Status, GiftCardAdjustment] {

  import GiftCardAdjustment._

  def stateLens = GenLens[GiftCardAdjustment](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    Auth → Set(Canceled, Capture)
  )
}

object GiftCardAdjustment {
  sealed trait Status
  case object Auth extends Status
  case object Canceled extends Status
  case object Capture extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn

  def build(gc: GiftCard, orderPayment: OrderPayment): GiftCardAdjustment = {
    GiftCardAdjustment(giftCardId = gc.id, orderPaymentId = orderPayment.id, credit = 0, debit = 0)
  }
}

class GiftCardAdjustments(tag: Tag)
  extends GenericTable.TableWithId[GiftCardAdjustment](tag, "gift_card_adjustments")
   {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def credit = column[Int]("credit")
  def debit = column[Int]("debit")
  def status = column[GiftCardAdjustment.Status]("status")

  def * = (id, giftCardId, orderPaymentId,
    credit, debit, status) <> ((GiftCardAdjustment.apply _).tupled, GiftCardAdjustment.unapply)

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object GiftCardAdjustments extends TableQueryWithId[GiftCardAdjustment, GiftCardAdjustments](
  idLens = GenLens[GiftCardAdjustment](_.id)
  )(new GiftCardAdjustments(_)){

  import GiftCardAdjustment._

  def filterByGiftCardId(id: Int)
    (implicit ec: ExecutionContext, db:Database): QuerySeq =
    _filterByGiftCardId(id)

  def _filterByGiftCardId(id: Int): QuerySeq =
    filter(_.giftCardId === id)

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.status).update(Canceled)
}
