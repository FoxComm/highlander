package models

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


case class Order(id: Int = 0, customerId: Int, status: Order.Status, locked: Int = 0) extends ModelWithIdParameter {
  var lineItems: Seq[OrderLineItem] = Seq.empty


  def payments: Future[Seq[AppliedPayment]] = {
    Orders.collectPaymentMethods(this)
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
  def locked = column[Int]("locked") // 0 for no; 1 for yes
  def * = (id, customerId, status, locked) <> ((Order.apply _).tupled, Order.unapply)
}

object Orders extends TableQueryWithId[Order, Orders](
  idLens = GenLens[Order](_.id)
  )(new Orders(_)){
  val table = TableQuery[Orders]


  // TODO: YAX: Get rid of this and replace with something real.
  def collectPaymentMethods(order: Order): Future[Seq[AppliedPayment]] = {
    val appliedpayment = AppliedPayment(id = 1, orderId = order.id, paymentMethodId = 1, paymentMethodType = "TokenizedCard", appliedAmount = 10000, status = Applied.toString, responseCode = "")
    val appliedpayment2 = appliedpayment.copy(appliedAmount = 2550, paymentMethodId = 2)


    Future.successful(Seq(appliedpayment, appliedpayment2))
  }



  def _create(order: Order)(implicit ec: ExecutionContext, db: Database): DBIOAction[models.Order, NoStream, Effect.Write] = {
   for {
     newId <- Orders.returningId += order
   } yield order.copy(id = newId)
  }

  def findByCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Future[Seq[Order]] = {
    db.run(_findByCustomer(customer).result)
  }

  def _findByCustomer(cust: Customer) = { table.filter(_.customerId === cust.id) }

  def findActiveOrderByCustomer(cust: Customer)(implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {
    // TODO: (AW): we should find a way to ensure that the customer only has one cart.
    db.run(_findActiveOrderByCustomer(cust).result.headOption)
  }

  def _findActiveOrderByCustomer(cust: Customer) = { table.filter(_.customerId === cust.id).filter(_.status === (Order.Cart: Order.Status)) }

  // If the user doesn't have a cart yet, let's create one.
  def findOrCreateActiveOrderByCustomer(customer: Customer)
                            (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {
    val actions = for {
      numCarts <- _findActiveOrderByCustomer(customer).length.result
      cart <- if (numCarts < 1) {
        val freshCart = Order(customerId = customer.id, status = Order.Cart)
        (returningId += freshCart).map { id => freshCart.copy(id = id) }.map(Some(_))
      } else {
        _findActiveOrderByCustomer(customer).result.headOption
      }
    } yield cart

    db.run(actions.transactionally)
  }
}
