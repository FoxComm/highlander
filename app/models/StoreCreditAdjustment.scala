package models

import java.time.Instant

import cats.data.Xor
import com.pellucid.sealerate
import models.StoreCreditAdjustment.{Auth, Status}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import services.Failures
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreCreditAdjustment(id: Int = 0, storeCreditId: Int, orderPaymentId: Option[Int],
  storeAdminId: Option[Int] = None, debit: Int, availableBalance: Int, status: Status = Auth, createdAt: Instant = Instant.now())
  extends ModelWithIdParameter[StoreCreditAdjustment]
  with FSM[StoreCreditAdjustment.Status, StoreCreditAdjustment] {

  import StoreCreditAdjustment._

  def stateLens = GenLens[StoreCreditAdjustment](_.status)
  override def updateTo(newModel: StoreCreditAdjustment): Failures Xor StoreCreditAdjustment = super.transitionModel(newModel)

  val fsm: Map[Status, Set[Status]] = Map(
    Auth → Set(Canceled, Capture)
  )
}

object StoreCreditAdjustment {
  sealed trait Status
  case object Auth extends Status
  case object Canceled extends Status
  case object Capture extends Status
  case object CancellationCapture extends Status

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
  def storeAdminId = column[Option[Int]]("store_admin_id")
  def orderPaymentId = column[Option[Int]]("order_payment_id")
  def debit = column[Int]("debit")
  def availableBalance = column[Int]("available_balance")
  def status = column[StoreCreditAdjustment.Status]("status")
  def createdAt = column[Instant]("created_at")

  def * = (id, storeCreditId, orderPaymentId, storeAdminId, debit, availableBalance,
    status, createdAt) <> ((StoreCreditAdjustment.apply _).tupled, StoreCreditAdjustment.unapply)

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id.?)
  def storeCredit = foreignKey(StoreCredits.tableName, storeCreditId, StoreCredits)(_.id)
}

object StoreCreditAdjustments
  extends TableQueryWithId[StoreCreditAdjustment, StoreCreditAdjustments](
  idLens = GenLens[StoreCreditAdjustment](_.id)
  )(new StoreCreditAdjustments(_)){

  import StoreCreditAdjustment._

  def filterByStoreCreditId(id: Int): QuerySeq = filter(_.storeCreditId === id)

  def lastAuthByStoreCreditId(id: Int): QuerySeq =
    filterByStoreCreditId(id).filter(_.status === (Auth: Status)).sortBy(_.createdAt).take(1)

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.status).update(Canceled)
}
