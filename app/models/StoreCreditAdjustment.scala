package models

import com.pellucid.sealerate
import models.StoreCreditAdjustment.{Auth, Status}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreCreditAdjustment(id: Int = 0, storeCreditId: Int, orderPaymentId: Int,
  debit: Int, status: Status = Auth)
  extends ModelWithIdParameter
  with FSM[StoreCreditAdjustment.Status, StoreCreditAdjustment] {

  import StoreCreditAdjustment._

  def stateLens = GenLens[StoreCreditAdjustment](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    Auth → Set(Canceled, Capture)
  )
}

object StoreCreditAdjustment {
  sealed trait Status
  case object Auth extends Status
  case object Canceled extends Status
  case object Capture extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
}

class StoreCreditAdjustments(tag: Tag)
  extends GenericTable.TableWithId[StoreCreditAdjustment](tag, "store_credit_adjustments")
   {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeCreditId = column[Int]("store_credit_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def debit = column[Int]("debit")
  def status = column[StoreCreditAdjustment.Status]("status")

  def * = (id, storeCreditId, orderPaymentId,
    debit, status) <> ((StoreCreditAdjustment.apply _).tupled, StoreCreditAdjustment.unapply)

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object StoreCreditAdjustments
  extends TableQueryWithId[StoreCreditAdjustment, StoreCreditAdjustments](
  idLens = GenLens[StoreCreditAdjustment](_.id)
  )(new StoreCreditAdjustments(_)){

  import StoreCreditAdjustment._

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.status).update(Canceled)
}

