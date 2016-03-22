package models.order

import java.time.Instant

import cats.data.Validated.valid
import cats.data.Xor.{left, right}
import cats.data.{ValidatedNel, Xor}
import com.pellucid.sealerate
import failures.CartFailures.OrderMustBeCart
import failures.{Failure, Failures, GeneralFailure}
import models.{currencyColumnTypeMapper, javaTimeSlickMapper}
import models.customer.Customer
import models.order.Order._
import models.traits.Lockable
import monocle.Lens
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Money.Currency
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.table.SearchByRefNum
import utils.{ADT, FSM, GenericTable, ModelWithLockParameter, TableQueryWithLock, Validation}
import utils.aliases._

final case class Order(
  id: Int = 0, referenceNumber: String = "", customerId: Int, productContextId: Int,
  state: State = Cart, isLocked: Boolean = false, placedAt: Option[Instant] = None,
  remorsePeriodEnd: Option[Instant] = None, rmaCount: Int = 0, currency: Currency = Currency.USD,
  subTotal: Int = 0, shippingTotal: Int = 0, adjustmentsTotal: Int = 0, taxesTotal: Int = 0, grandTotal: Int = 0)
  extends ModelWithLockParameter[Order]
  with FSM[Order.State, Order]
  with Lockable[Order]
  with Validation[Order] {

  // TODO: Add order validations
  override def validate: ValidatedNel[Failure, Order] = {
    valid(this)
  }

  def isCart: Boolean = state == Cart

  def refNum: String = referenceNumber

  def stateLens = GenLens[Order](_.state)
  override def updateTo(newModel: Order): Failures Xor Order = super.transitionModel(newModel)
  override def primarySearchKeyLens: Lens[Order, String] = GenLens[Order](_.referenceNumber)

  val fsm: Map[State, Set[State]] = Map(
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
  val getRemorsePeriodEnd: Option[Instant] = state match {
    case RemorseHold if !isLocked ⇒ remorsePeriodEnd
    case _ ⇒ None
  }

  def getShippingState: Option[State] = state match {
    case Cart ⇒ None
    case _    ⇒ Some(state)
  }

  def mustBeCart: Failures Xor Order =
    if (isCart) right(this) else left(OrderMustBeCart(this.refNum).single)

  def mustBeRemorseHold: Failures Xor Order =
    if (state == RemorseHold) right(this) else left(GeneralFailure("Order is not in RemorseHold state").single)
}

object Order {
  sealed trait State

  case object Cart extends State
  case object FraudHold extends State
  case object RemorseHold extends State
  case object ManualHold extends State
  case object Canceled extends State
  case object FulfillmentStarted extends State
  case object Shipped extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  def buildCart(customerId: Int, productContextId: Int): Order = Order(customerId = customerId, productContextId = productContextId, state = Order.Cart)

  val orderRefNumRegex = """([a-zA-Z0-9-_]*)""".r
}

class Orders(tag: Tag) extends GenericTable.TableWithLock[Order](tag, "orders")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  // TODO: Find a way to deal with guest checkouts...
  def referenceNumber = column[String]("reference_number") //we should generate this based on certain rules; nullable until then
  def customerId = column[Int]("customer_id")
  def productContextId = column[Int]("product_context_id")
  def state = column[Order.State]("state")
  def isLocked = column[Boolean]("is_locked")
  def placedAt = column[Option[Instant]]("placed_at")
  def remorsePeriodEnd = column[Option[Instant]]("remorse_period_end")
  def rmaCount = column[Int]("rma_count")
  def currency = column[Currency]("currency")

  def subTotal = column[Int]("sub_total")
  def shippingTotal = column[Int]("shipping_total")
  def adjustmentsTotal = column[Int]("adjustments_total")
  def taxesTotal = column[Int]("taxes_total")
  def grandTotal = column[Int]("grand_total")

  def * = (id, referenceNumber, customerId, productContextId, state, isLocked, placedAt, remorsePeriodEnd,
    rmaCount, currency, subTotal, shippingTotal, adjustmentsTotal,
    taxesTotal, grandTotal) <>((Order.apply _).tupled, Order.unapply)
}

object Orders extends TableQueryWithLock[Order, Orders](
  idLens = GenLens[Order](_.id)
  )(new Orders(_))
  with SearchByRefNum[Order, Orders] {

  import scope._

  val returningIdAndReferenceNumber = this.returning(map { o ⇒ (o.id, o.referenceNumber) })

  def returningAction(ret: (Int, String))(order: Order): Order = ret match {
    case (id, referenceNumber) ⇒ order.copy(id = id, referenceNumber = referenceNumber)
  }

  override def create[R](order: Order, returning: Returning[R], action: R ⇒ Order ⇒ Order)
    (implicit ec: EC): DbResult[Order] = super.create(order, returningIdAndReferenceNumber, returningAction)

  def findByCustomer(cust: Customer): QuerySeq =
    findByCustomerId(cust.id)

  def findByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Order]] =
    filter(_.referenceNumber === refNum).one

  def findOneByRefNumAndCustomer(refNum: String, customer: Customer): QuerySeq =
    filter(_.referenceNumber === refNum).filter(_.customerId === customer.id)

  def findCartByRefNum(refNum: String): QuerySeq =
    findByRefNum(refNum).cartOnly

  def findActiveOrderByCustomer(cust: Customer) =
    filter(_.customerId === cust.id).filter(_.state === (Order.Cart: Order.State))

  object scope {
    implicit class OrdersQuerySeqConversions(q: QuerySeq) {
      def cartOnly: QuerySeq =
        q.filter(_.state === (Order.Cart: Order.State))
    }
  }

}
