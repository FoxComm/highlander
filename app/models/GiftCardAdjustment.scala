package models

import com.pellucid.sealerate
import utils.{ADT, GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

import GiftCardAdjustment._

final case class GiftCardAdjustment(id: Int = 0, giftCardId: Int, orderPaymentId: Int,
  credit: Int, debit: Int, capture: Boolean, status: StoreCreditAdjustment.Status = StoreCreditAdjustment.Auth)
  extends ModelWithIdParameter {
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
  def capture = column[Boolean]("capture")
  def status = column[StoreCreditAdjustment.Status]("status")

  def * = (id, giftCardId, orderPaymentId,
    credit, debit, capture, status) <> ((GiftCardAdjustment.apply _).tupled, GiftCardAdjustment.unapply)

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object GiftCardAdjustments extends TableQueryWithId[GiftCardAdjustment, GiftCardAdjustments](
  idLens = GenLens[GiftCardAdjustment](_.id)
  )(new GiftCardAdjustments(_)){
}
