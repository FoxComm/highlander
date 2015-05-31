import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import com.typesafe.config.{ConfigFactory, Config}
import com.wix.accord.{validate => runValidation, Success => ValidationSuccess, Failure => ValidationFailure }
import com.wix.accord._
import dsl.{validator => createValidator}
import dsl._
import java.util.Date
import akka.event.Logging
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

// Validation mixin
trait Validation {
  def validator[T]: Validator[T]
  def validate: Result = { runValidation(this)(validator) }
  def isValid: Boolean = { validate == ValidationSuccess }
}

// TODO: money/currency abstraction. Use joda-money, most likely
case class Money()

case class State(id: Int, name: String, abbreviation: String)

case class City(id: Int, name: String)

case class Address(id: Int, name: String, streetAddresses: List[String], city: City, state: State, zip: String)

case class Adjustment(id: Int)

case class Coupon(id: Int, cartId: Int, code: String, adjustment: List[Adjustment])

case class Promotion(id: Int, cartId: Int, adjustments: List[Adjustment])

case class LineItem(id: Int, skuId: Int, quantity: Int)

sealed trait PaymentStatus
case object Auth extends PaymentStatus
case object FailedCapture extends PaymentStatus
case object CanceledAuth extends PaymentStatus
case object ExpiredAuth extends PaymentStatus

sealed trait GiftCardPaymentStatus extends PaymentStatus
case object InsufficientBalance extends GiftCardPaymentStatus
case object SuccessfulDebit extends GiftCardPaymentStatus
case object FailedDebit extends GiftCardPaymentStatus

abstract class Payment {
  val id: Int
  val cartId: Int
  val status: PaymentStatus

  def validate: Boolean = {
    scala.util.Random.nextInt(2) == 1
  }

  def process: Option[String] = {
    println("processing payment")
    if (scala.util.Random.nextInt(2) == 1) {
      None
    } else {
      Some("payment processing failed")
    }
  }
}

case class CreditCard(id: Int, cartId: Int, status: PaymentStatus, cvv: Int, number: String, expiration: Date, address: Address) extends Payment

case class GiftCard(id: Int, cartId: Int, status: GiftCardPaymentStatus, code: String) extends Payment

case class ShippingInformation(id: Int, cartId: Int, cost: Money)

case class Cart(id: Int, lineItems: Seq[LineItem], payments: Seq[Payment], deliveries: Seq[ShippingInformation],
                coupons: Seq[Coupon], adjustments: List[Adjustment]) {
  // TODO: probably make this a service class
  def checkout: Either[String, Order] = {
    if (payments.nonEmpty && !payments.forall(_.validate)) {
      return Left("Payments invalid!")
    }

    // capture payments and ignore error return values for now
    payments.foreach(_.process)

    // validate deliveries
    // validate inventory
    Right(Order.fromCart(this))
  }

  // TODO: how do we handle adjustment/coupon
  // coupons extends promotions + interaction rules
  def addCoupon(coupon: Coupon) = {}
}

sealed trait OrderStatus
case object New extends OrderStatus
case object FraudHold extends OrderStatus
case object RemorseHold extends OrderStatus
case object ManualHold extends OrderStatus
case object Canceled extends OrderStatus
case object FulfillmentStarted extends OrderStatus
case object PartiallyShipped extends OrderStatus
case object Shipped extends OrderStatus

case class Order(id: Int, cartId: Int, status: OrderStatus, lineItems: Seq[LineItem], payment: Seq[Payment], deliveries: Seq[ShippingInformation], adjustments: List[Adjustment]) {
  // aggregated status (header) of lineItems => single Order status
  def masterStatus: OrderStatus = New
}

object Order {
  def fromCart(cart: Cart): Order = {
    Order(id = 0, cartId = cart.id, status = New, lineItems = cart.lineItems, payment = cart.payments, deliveries = cart.deliveries, adjustments = cart.adjustments)
  }
}

case class StockItem(id: Int, productId: Int, stockLocationId: Int, onHold: Int, onHand: Int, allocatedToSales: Int) {
  def available: Int = {
    this.onHand - this.onHold - this.allocatedToSales
  }
}

case class StockLocation(id: Int, name: String)

case class Store(id: Int, name: String)

case class User(id: Int, email: String, password: String, firstName: String, lastName: String) extends Validation {
  override def validator[T] = {
    createValidator[User] { user =>
      user.firstName is notEmpty
      user.lastName is notEmpty
      user.email is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

case class Archetype(id: Int, name: String) extends Validation {
  override def validator[T] = {
    createValidator[Archetype] { archetype =>
      archetype.name is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

case class Catalog(id: Int, name: String, archetypes: Seq[Archetype], products: Seq[Product]) extends Validation {
  override def validator[T] = {
    createValidator[Catalog] { catalog =>
      catalog.name is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

case class Product(id: Int, name: String, sku: String, archetypes: Seq[Archetype])

case class Asset(id: Int, url: String, rank: Int)

case class Collection(id: Int, name: String, isActive: Boolean) extends Validation {
  override def validator[T] = {
    createValidator[Collection] { collection =>
      collection.name is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

object Main {

  def main(args: Array[String]) = {
    sanityCheck()
    val service = new Service()
    service.bind()
  }

  def sanityCheck() = {
    val payments = List[Payment](GiftCard(1, 1, InsufficientBalance, "123"), GiftCard(2, 1, SuccessfulDebit, "123"))

    val cart = Cart(1, Seq[LineItem](), payments, Seq[ShippingInformation](), Seq[Coupon](), List[Adjustment]())

    println(cart.checkout)

    val invalidUser = User(1, "yax@yax.com", "password", "", "")
    val validUser = User(1, "yax@yax.com", "password", "Yax", "Fuentes")

    List(invalidUser, validUser).foreach { user =>
      user.validate match {
        case ValidationFailure(violations) => printf("%s is invalid: %s\n", user, violations)
        case ValidationSuccess => printf("%s is invalid\n", user)
      }
    }

    val order = Order.fromCart(cart)

    payments.foreach { payment =>
      val status = payment match {
        case GiftCard(_, _, InsufficientBalance, _) => "insufficient!"
        case GiftCard(_, _, SuccessfulDebit, _) => "great success!"
      }

      println(status)
    }
  }
}

// JSON formatters
trait Protocols extends DefaultJsonProtocol {
  implicit val lineItemFormat = jsonFormat3(LineItem.apply)
  implicit val giftCardFormat = jsonFormat4(GiftCard.apply)
  implicit val creditCardFormat = jsonFormat7(CreditCard.apply)
  implicit val shippingInformationFormat = jsonFormat3(ShippingInformation.apply)
  implicit val couponFormat = jsonFormat4(Coupon.apply)
  implicit val adjustmentFormat = jsonFormat1(Adjustment.apply)
}

class Service extends Protocols {
  val conf: String =
    """
      |akka {
      |  loglevel = "DEBUG"
      |  loggers = ["akka.event.Logging$DefaultLogger"]
      |  actor.debug.receive = on
      |}
      |
      |http {
      |  interface = "localhost"
      |  port = 9000
      |}
    """.stripMargin

  val config: Config = ConfigFactory.parseString(conf)

  implicit val system = ActorSystem.create("Cart", config)
  implicit def executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val logger = Logging(system, getClass)

  val routes = {
    logRequestResult("cart") {
        (get & path("")) {
          complete {
            logger.debug("hello")
//            logger.debug(id)
            val cart = Cart(1, Seq[LineItem](), List[Payment](), Seq[ShippingInformation](), Seq[Coupon](), List[Adjustment]())
            cart.asInstanceOf[ToResponseMarshallable]
          }
        }
      }
    }

  def bind(): Unit = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
