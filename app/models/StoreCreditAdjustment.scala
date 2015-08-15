package models

import com.pellucid.sealerate
import utils.{ADT, GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

final case class StoreCreditAdjustment(id: Int = 0, storeCreditId: Int, orderPaymentId: Int,
  debit: Int, capture: Boolean, status: StoreCreditAdjustment.Status = StoreCreditAdjustment.Auth)
  extends ModelWithIdParameter

object StoreCreditAdjustment {
  sealed trait Status
  case object Auth extends Status
  case object Canceled extends Status
  case object Capture extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class StoreCreditAdjustments(tag: Tag)
  extends GenericTable.TableWithId[StoreCreditAdjustment](tag, "store_credit_adjustments")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeCreditId = column[Int]("store_credit_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def debit = column[Int]("debit")
  def capture = column[Boolean]("capture")
  def status = column[StoreCreditAdjustment.Status]("status")

  def * = (id, storeCreditId, orderPaymentId,
    debit, capture, status) <> ((StoreCreditAdjustment.apply _).tupled, StoreCreditAdjustment.unapply)

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object StoreCreditAdjustments
  extends TableQueryWithId[StoreCreditAdjustment, StoreCreditAdjustments](
  idLens = GenLens[StoreCreditAdjustment](_.id)
  )(new StoreCreditAdjustments(_)){
}

