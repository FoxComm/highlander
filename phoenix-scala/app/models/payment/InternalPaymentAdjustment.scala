package models.payment

import java.time.Instant

import com.pellucid.sealerate
import models.cord.OrderPayments
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT
import utils.db.ExPostgresDriver.api._
import utils.db._

trait InternalPaymentAdjustment[M <: InternalPaymentAdjustment[M]] extends FoxModel[M] { self: M ⇒
  def id: Int
  def orderPaymentId: Option[Int]
  def storeAdminId: Option[Int]
  def debit: Int
  def availableBalance: Int
  def state: InternalPaymentAdjustment.State
  def createdAt: Instant
}

object InternalPaymentAdjustment {
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

trait InternalPaymentStates {
  def Auth                = InternalPaymentAdjustment.Auth
  def Canceled            = InternalPaymentAdjustment.Canceled
  def Capture             = InternalPaymentAdjustment.Capture
  def CancellationCapture = InternalPaymentAdjustment.CancellationCapture
  def State               = InternalPaymentAdjustment.State
}

abstract class InternalPaymentAdjustments[M <: InternalPaymentAdjustment[M]](tag: Tag,
                                                                             table: String)
    extends FoxTable[M](tag, table) {

  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeAdminId     = column[Option[Int]]("store_admin_id")
  def orderPaymentId   = column[Option[Int]]("order_payment_id")
  def debit            = column[Int]("debit")
  def availableBalance = column[Int]("available_balance")
  def state            = column[InternalPaymentAdjustment.State]("state")
  def createdAt        = column[Instant]("created_at")

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id.?)
}

abstract class InternalPaymentAdjustmentQueries[M <: InternalPaymentAdjustment[M],
    T <: InternalPaymentAdjustments[M]](construct: Tag ⇒ T)
    extends FoxTableQuery[M, T](construct) {

  import InternalPaymentAdjustment._

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.state).update(Canceled)

  def authorizedOrderPayments(orderPaymentIds: Seq[Int]): QuerySeq =
    filter(adj ⇒ adj.orderPaymentId.inSet(orderPaymentIds) && adj.state === (Auth: State))

  def lastPaymentState(orderPaymentId: Int): DBIO[Option[State]] =
    filter(_.orderPaymentId === orderPaymentId).sortBy(_.createdAt.desc).map(_.state).one

}
