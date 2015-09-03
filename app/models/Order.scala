package models

import cats.data.Validated.valid
import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Checks

import scala.concurrent.{ExecutionContext, Future}

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.pellucid.sealerate
import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import models.Order.{Cart, Status}
import monocle.macros.GenLens
import org.joda.time.DateTime
import services.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.{ADT, FSM, GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import utils.Slick.implicits._

final case class Order(id: Int = 0, referenceNumber: String = "", customerId: Int,
  status: Status = Cart, locked: Boolean = false, placedAt: Option[DateTime] = None,
  remorsePeriodInMinutes: Int = 30)
  extends ModelWithIdParameter
  with FSM[Order.Status, Order] {

  import Order._

  // TODO: Add order validations
  def validateNew: ValidatedNel[Failure, Order] = {
    valid(this)
  }

  // TODO: Add a real collector/builder here that assembles the subTotal
  def subTotal(implicit ec: ExecutionContext, db: Database): Future[Int] = {
    OrderTotaler.subTotalForOrder(this)
  }

  def grandTotal: Future[Int] = {
    Future.successful(27)
  }

  def isNew: Boolean = id == 0

  def refNum: String = referenceNumber

  def stateLens = GenLens[Order](_.status)

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

  implicit val statusColumnType = Status.slickColumn

  def buildCart(customerId: Int): Order = Order(customerId = customerId, status = Order.Cart)
}

class Orders(tag: Tag) extends GenericTable.TableWithId[Order](tag, "orders")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  // TODO: Find a way to deal with guest checkouts...
  def referenceNumber = column[String]("reference_number") //we should generate this based on certain rules; nullable until then
  def customerId = column[Int]("customer_id")
  def status = column[Order.Status]("status")
  def locked = column[Boolean]("locked")
  def placedAt = column[Option[DateTime]]("placed_at")
  def remorsePeriodInMinutes = column[Int]("remorse_period_in_minutes")
  def * = (id, referenceNumber, customerId, status, locked, placedAt, remorsePeriodInMinutes) <>((Order.apply _).tupled, Order.unapply)
}

object Orders extends TableQueryWithId[Order, Orders](
  idLens = GenLens[Order](_.id)
  )(new Orders(_)){

  type QuerySeq = Query[Orders, Order, Seq]

  import scope._

  val returningIdAndReferenceNumber = this.returning(map { o ⇒ (o.id, o.referenceNumber) })

  override def save(order: Order)(implicit ec: ExecutionContext) = {
    if (order.isNew) {
      _create(order)
    } else {
      super.save(order)
    }
  }

  def create(order: Order)(implicit ec: ExecutionContext, db: Database): Future[models.Order] =
    _create(order).run()

  def _create(order: Order)(implicit ec: ExecutionContext): DBIO[models.Order] = for {
     (newId, refNum) <- returningIdAndReferenceNumber += order
  } yield order.copy(id = newId, referenceNumber = refNum)

  def findByCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Future[Seq[Order]] = {
    db.run(_findByCustomer(customer).result)
  }

  def _findByCustomer(cust: Customer): QuerySeq =
    findByCustomerId(cust.id)

  def findByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)

  def findCartByRefNum(refNum: String): QuerySeq =
    findByRefNum(refNum).cartOnly

  def findActiveOrderByCustomer(cust: Customer)(implicit ec: ExecutionContext, db: Database): Future[Option[Order]] =
    db.run(_findActiveOrderByCustomer(cust).one)

  def _findActiveOrderByCustomer(cust: Customer) =
    filter(_.customerId === cust.id).filter(_.status === (Order.Cart: Order.Status))

  // If the user doesn't have an order yet, let's create one.
  def findOrCreateActiveOrderByCustomer(customer: Customer)
                            (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {
    val actions = for {
      numOrders <- _findActiveOrderByCustomer(customer).length.result
      order <- if (numOrders < 1) {
        val freshOrder = Order(customerId = customer.id, status = Order.Cart)
        (returningId += freshOrder).map { id => freshOrder.copy(id = id) }.map(Some(_))
      } else {
        _findActiveOrderByCustomer(customer).one
      }
    } yield order

    db.run(actions.transactionally)
  }

  object scope {
    implicit class OrdersQuerySeqConversions(q: QuerySeq) {
      def cartOnly: QuerySeq =
        q.filter(_.status === (Order.Cart: Order.Status))
    }
  }
}
