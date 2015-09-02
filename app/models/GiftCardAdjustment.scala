package models

import com.pellucid.sealerate
import models.GiftCardAdjustment.{Auth, Status}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId}

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

  implicit val statusColumnType = Status.slickColumn
}

class GiftCardAdjustments(tag: Tag)
  extends GenericTable.TableWithId[GiftCardAdjustment](tag, "gift_card_adjustments")
  with RichTable {

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

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.status).update(Canceled)
}
