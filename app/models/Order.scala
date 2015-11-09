package models

import java.time.Instant

import cats.data.Validated.valid
import cats.data.{Xor, ValidatedNel}
import services.CartFailures.OrderMustBeCart
import services._

import scala.concurrent.ExecutionContext

import com.pellucid.sealerate
import models.Order.{Cart, Status}
import monocle.Lens
import monocle.macros.GenLens
import services.orders.OrderTotaler
import services.CartFailures.OrderMustBeCart
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, FSM, GenericTable, ModelWithLockParameter, TableQueryWithLock, Validation}
import utils.Slick.implicits._

final case class Order(id: Int = 0, referenceNumber: String = "", customerId: Int,
  status: Status = Cart, locked: Boolean = false, placedAt: Option[Instant] = None,
  remorsePeriodEnd: Option[Instant] = None)
  extends ModelWithLockParameter[Order]
  with FSM[Order.Status, Order]
  with Validation[Order] {

  import Order._

  // TODO: Add order validations
  override def validate: ValidatedNel[Failure, Order] = {
    valid(this)
  }

  def isCart: Boolean = status == Cart

  def refNum: String = referenceNumber

  def stateLens = GenLens[Order](_.status)
  override def updateTo(newModel: Order): Failures Xor Order = super.transitionModel(newModel)
  override def primarySearchKeyLens: Lens[Order, String] = GenLens[Order](_.referenceNumber)

  val fsm: Map[Status, Set[Status]] = Map(
    Cart →
      Set(FraudHold, RemorseHold, Canceled, FulfillmentStarted),
    FraudHold →
      Set(ManualHold, RemorseHold, FulfillmentStarted, Canceled),
    RemorseHold →
      Set(FraudHold, ManualHold, FulfillmentStarted, Canceled),
    ManualHold →
      Set(FraudHold, RemorseHold, FulfillmentStarted, Canceled),
    FulfillmentStarted →
      Set(Shipped, Canceled)
  )

  // If order is not in RemorseHold, remorsePeriodEnd should be None, but extra check wouldn't hurt
  val getRemorsePeriodEnd: Option[Instant] = status match {
    case RemorseHold if !locked ⇒ remorsePeriodEnd
    case _ ⇒ None
  }

  def mustBeCart: Failures Xor Order = if (isCart) Xor.Right(this) else Xor.Left(OrderMustBeCart(this.refNum).single)
}

object Order {
  sealed trait Status

  case object Cart extends Status
  case object Ordered extends Status
  case object FraudHold extends Status
  case object RemorseHold extends Status
  case object ManualHold extends Status
  case object Canceled extends Status
  case object FulfillmentStarted extends Status
  case object Shipped extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn

  def buildCart(customerId: Int): Order = Order(customerId = customerId, status = Order.Cart)

  val orderRefNumRegex = """([a-zA-Z0-9-_]*)""".r
}

class Orders(tag: Tag) extends GenericTable.TableWithLock[Order](tag, "orders")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  // TODO: Find a way to deal with guest checkouts...
  def referenceNumber = column[String]("reference_number") //we should generate this based on certain rules; nullable until then
  def customerId = column[Int]("customer_id")
  def status = column[Order.Status]("status")
  def locked = column[Boolean]("locked")
  def placedAt = column[Option[Instant]]("placed_at")
  def remorsePeriodEnd = column[Option[Instant]]("remorse_period_end")

  def * = (id, referenceNumber, customerId, status, locked, placedAt, remorsePeriodEnd) <>((Order.apply _).tupled, Order.unapply)

  def assignees = OrderAssignments.filter(_.orderId === id).flatMap(_.assignee)
}

object Orders extends TableQueryWithLock[Order, Orders](
  idLens = GenLens[Order](_.id)
  )(new Orders(_)){

  import scope._

  val returningIdAndReferenceNumber = this.returning(map { o ⇒ (o.id, o.referenceNumber) })

  override def primarySearchTerm: String = "referenceNumber"

  override def saveNew(order: Order)(implicit ec: ExecutionContext): DBIO[Order] = for {
     (newId, refNum) ← returningIdAndReferenceNumber += order
  } yield order.copy(id = newId, referenceNumber = refNum)

  def findByCustomer(cust: Customer): QuerySeq =
    findByCustomerId(cust.id)

  def findByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)

  def findCartByRefNum(refNum: String): QuerySeq =
    findByRefNum(refNum).cartOnly

  def findActiveOrderByCustomer(cust: Customer) =
    filter(_.customerId === cust.id).filter(_.status === (Order.Cart: Order.Status))

  object scope {
    implicit class OrdersQuerySeqConversions(q: QuerySeq) {
      def cartOnly: QuerySeq =
        q.filter(_.status === (Order.Cart: Order.Status))
    }
  }

  implicit class OrderQueryWrappers(q: QuerySeq) extends LockableQueryWrappers(q) {
    def mustBeCart(order: Order): Failures Xor Order = order.mustBeCart
  }
}
