package models

import services.OrderTotaler
import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


case class Order(id: Int = 0, customerId: Int, status: Order.Status = Order.Cart, locked: Boolean = false)
  extends ModelWithIdParameter
  with Validation[Order] {

  override def validator = createValidator[Order] { order => }

  // TODO: Add a real collector/builder here that assembles the subTotal
  def subTotal(implicit ec: ExecutionContext, db: Database): Future[Option[Int]] = {
    OrderTotaler.grandTotalForOrder(this)
  }

  def grandTotal: Future[Option[Int]] = {
    Future(Some(27))
  }
}

object Order {
  sealed trait Status
  case object Cart extends Status
  case object Ordered extends Status
  case object FraudHold extends Status //this only applies at the order_header level
  case object RemorseHold extends Status //this only applies at the order_header level
  case object ManualHold extends Status //this only applies at the order_header level
  case object Canceled extends Status
  case object FulfillmentStarted extends Status
  case object PartiallyShipped extends Status
  case object Shipped extends Status

  implicit val StatusColumnType = MappedColumnType.base[Status, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "cart" => Cart
    case "ordered" => Ordered
    case "fraudhold" => FraudHold
    case "remorsehold" => RemorseHold
    case "manualhold" => ManualHold
    case "canceled" => Canceled
    case "fulfillmen_started" => FulfillmentStarted
    case "partiallyshipped" => PartiallyShipped
    case "shipped" => Shipped
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  })

}

class Orders(tag: Tag) extends GenericTable.TableWithId[Order](tag, "orders") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  // TODO: Find a way to deal with guest checkouts...
  def customerId = column[Int]("customer_id")
  def status = column[Order.Status]("status")
  def locked = column[Boolean]("locked")
  def * = (id, customerId, status, locked) <> ((Order.apply _).tupled, Order.unapply)
}

object Orders extends TableQueryWithId[Order, Orders](
  idLens = GenLens[Order](_.id)
  )(new Orders(_)){

  def _create(order: Order)(implicit ec: ExecutionContext, db: Database): DBIOAction[models.Order, NoStream, Effect.Write] = {
   for {
     newId <- Orders.returningId += order
   } yield order.copy(id = newId)
  }

  def findByCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Future[Seq[Order]] = {
    db.run(_findByCustomer(customer).result)
  }

  def _findByCustomer(cust: Customer) = { filter(_.customerId === cust.id) }

  def findActiveOrderByCustomer(cust: Customer)(implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {
    // TODO: (AW): we should find a way to ensure that the customer only has one order with a cart status.
    db.run(_findActiveOrderByCustomer(cust).result.headOption)
  }

  def _findActiveOrderByCustomer(cust: Customer) = { filter(_.customerId === cust.id).filter(_.status === (Order.Cart: Order.Status)) }

  // If the user doesn't have an order yet, let's create one.
  def findOrCreateActiveOrderByCustomer(customer: Customer)
                            (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {
    val actions = for {
      numOrders <- _findActiveOrderByCustomer(customer).length.result
      order <- if (numOrders < 1) {
        val freshOrder = Order(customerId = customer.id, status = Order.Cart)
        (returningId += freshOrder).map { id => freshOrder.copy(id = id) }.map(Some(_))
      } else {
        _findActiveOrderByCustomer(customer).result.headOption
      }
    } yield order

    db.run(actions.transactionally)
  }
}
