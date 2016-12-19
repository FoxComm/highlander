package models.payment

import java.time.Instant

import com.pellucid.sealerate
import models.cord.OrderPayments
import models.payment.PaymentStates._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT
import utils.db.ExPostgresDriver.api._
import utils.db._

trait PaymentAdjustment[M <: PaymentAdjustment[M]] extends FoxModel[M] { self: M ⇒
  def id: Int
  def orderPaymentId: Option[Int]
  def storeAdminId: Option[Int]
  def debit: Int
  def availableBalance: Int
  def state: PaymentStates.State
  def createdAt: Instant
}

object PaymentStates {
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

abstract class PaymentAdjustmentTable[M <: PaymentAdjustment[M]](tag: Tag, table: String)
    extends FoxTable[M](tag, table) {

  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeAdminId     = column[Option[Int]]("store_admin_id")
  def orderPaymentId   = column[Option[Int]]("order_payment_id")
  def debit            = column[Int]("debit")
  def availableBalance = column[Int]("available_balance")
  def state            = column[State]("state")
  def createdAt        = column[Instant]("created_at")

  def payment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id.?)
}

abstract class PaymentAdjustmentQueries[M <: PaymentAdjustment[M],
    T <: PaymentAdjustmentTable[M]](construct: Tag ⇒ T)
    extends FoxTableQuery[M, T](construct) {

  def cancel(id: Int): DBIO[Int] = filter(_.id === id).map(_.state).update(Canceled)

  def authorizedOrderPayments(orderPaymentIds: Seq[Int]): QuerySeq =
    filter(adj ⇒ adj.orderPaymentId.inSet(orderPaymentIds) && adj.state === (Auth: State))

  def lastPaymentState(orderPaymentId: Int): DBIO[Option[State]] =
    filter(_.orderPaymentId === orderPaymentId).sortBy(_.createdAt.desc).map(_.state).one

}
