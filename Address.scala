import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpResponse
import com.typesafe.config.{ConfigFactory, Config}
import com.wix.accord.{validate => runValidation, Success => ValidationSuccess, Failure => ValidationFailure }
import com.wix.accord._
import dsl.{validator => createValidator}
import dsl._
import java.util.Date
import akka.event.Logging
import spray.json.{JsValue, JsString, JsonFormat, DefaultJsonProtocol}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

// Validation mixin
trait Validation {
  def validator[T]: Validator[T]
  def validate: Result = { runValidation(this)(validator) }
  def isValid: Boolean = { validate == ValidationSuccess }
}

// TODO: money/currency abstraction. Use joda-money, most likely
case class Money(currency: String, amount: Int)
object Money extends DefaultJsonProtocol {
  implicit val moneyFormat = jsonFormat2(Money.apply)
}

case class State(id: Int, name: String, abbreviation: String)
object State extends DefaultJsonProtocol {
  implicit val stateFormat = jsonFormat3(State.apply)
}

case class City(id: Int, name: String)
object City extends DefaultJsonProtocol {
  implicit val cityFormat = jsonFormat2(City.apply)
}

case class Address(id: Int, name: String, streetAddresses: List[String], city: City, state: State, zip: String)
object Address extends DefaultJsonProtocol {
  implicit val addressFormat = jsonFormat6(Address.apply)
}

case class Adjustment(id: Int)
object Adjustment extends DefaultJsonProtocol {
  implicit val adjustmentFormat = jsonFormat1(Adjustment.apply)
}

case class Coupon(id: Int, cartId: Int, code: String, adjustment: List[Adjustment])
object Coupon extends DefaultJsonProtocol {
  implicit val couponFormat = jsonFormat4(Coupon.apply)
}

case class Promotion(id: Int, cartId: Int, adjustments: List[Adjustment])
object Promotion extends DefaultJsonProtocol {
  implicit val promotionFormat = jsonFormat3(Promotion.apply)
}

case class LineItem(id: Int, skuId: Int, quantity: Int)
object LineItem extends DefaultJsonProtocol {
  implicit val lineItemFormat = jsonFormat3(LineItem.apply)
}

sealed trait PaymentStatus
case object Auth extends PaymentStatus
case object FailedCapture extends PaymentStatus
case object CanceledAuth extends PaymentStatus
case object ExpiredAuth extends PaymentStatus

sealed trait GiftCardPaymentStatus extends PaymentStatus
case object InsufficientBalance extends GiftCardPaymentStatus
case object SuccessfulDebit extends GiftCardPaymentStatus
case object FailedDebit extends GiftCardPaymentStatus

object GiftCardPaymentStatus extends DefaultJsonProtocol {
  implicit object giftCardPaymentStatus extends JsonFormat[GiftCardPaymentStatus] {
    def write(obj: GiftCardPaymentStatus) = JsString(obj.toString)

    def read(json: JsValue): GiftCardPaymentStatus = json match {
      case JsString("InsufficientBalance") => InsufficientBalance
      case JsString("SuccessfulDebit")  => SuccessfulDebit
      case JsString("FailedDebit")  => FailedDebit
      case _ => throw new Exception("could not parse")
    }
  }
}

abstract class Payment extends DefaultJsonProtocol {
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

  implicit object paymentFormat extends JsonFormat[Payment] {
    def write(obj: Payment) = JsString(obj.toString)

    def read(json: JsValue): Payment = json match {
      case _ => throw new Exception("could not parse")
    }
  }
}

object PaymentStatus extends DefaultJsonProtocol {
  implicit object paymentStatusFormat extends JsonFormat[PaymentStatus] {
    def write(obj: PaymentStatus) = JsString(obj.toString)

    def read(json: JsValue): PaymentStatus = json match {
      case JsString("Auth") => Auth
      case JsString("FailedCapture") => FailedCapture
      case JsString("CanceledAuth") => CanceledAuth
      case JsString("ExpiredAuth") => ExpiredAuth
      case _ => throw new Exception("could not parse")
    }
  }
}

case class CreditCard(id: Int, cartId: Int, status: PaymentStatus, cvv: Int, number: String, expiration: String, address: Address) extends Payment
object CreditCard extends DefaultJsonProtocol {
  implicit val creditCardFormat = jsonFormat7(CreditCard.apply)
}

case class GiftCard(id: Int, cartId: Int, status: GiftCardPaymentStatus, code: String) extends Payment
object GiftCard extends DefaultJsonProtocol {
  implicit val giftCardFormat = jsonFormat4(GiftCard.apply)
}

case class ShippingInformation(id: Int, cartId: Int, cost: Money)
object ShippingInformation extends DefaultJsonProtocol {
  implicit val shippingInformationFormat = jsonFormat3(ShippingInformation.apply)
}

case class Cart(id: Int, lineItems: Seq[LineItem],
//                payments: Seq[Payment],
                deliveries: Seq[ShippingInformation],
                coupons: Seq[Coupon], adjustments: List[Adjustment]) {
  // TODO: probably make this a service class
//  def checkout: Either[String, Order] = {
//    if (payments.nonEmpty && !payments.forall(_.validate)) {
//      return Left("Payments invalid!")
//    }
//
////     capture payments and ignore error return values for now
//    payments.foreach(_.process)
//
////     validate deliveries
////     validate inventory
//    Right(Order.fromCart(this))
//  }

  // TODO: how do we handle adjustment/coupon
  // coupons extends promotions + interaction rules
  def addCoupon(coupon: Coupon) = {}
}

object Cart extends DefaultJsonProtocol {
  implicit val cartFormat = jsonFormat5(Cart.apply)
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
//  def fromCart(cart: Cart): Order = {
//    Order(id = 0, cartId = cart.id, status = New, lineItems = cart.lineItems, payment = cart.payments, deliveries = cart.deliveries, adjustments = cart.adjustments)
//  }
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

  def main(args: Array[String]): Unit = {
    val cart = Cart(1, Seq[LineItem](), Seq[ShippingInformation](), Seq[Coupon](), List[Adjustment]())
    println(cart.toJson)
//    sanityCheck()
    val service = new Service()
    service.bind()
  }

  def sanityCheck() = {
    val payments = List[Payment](GiftCard(1, 1, InsufficientBalance, "123"), GiftCard(2, 1, SuccessfulDebit, "123"))

    val cart = Cart(1, Seq[LineItem](), Seq[ShippingInformation](), Seq[Coupon](), List[Adjustment]())

//    println(cart.checkout)

    val invalidUser = User(1, "yax@yax.com", "password", "", "")
    val validUser = User(1, "yax@yax.com", "password", "Yax", "Fuentes")

    List(invalidUser, validUser).foreach { user =>
      user.validate match {
        case ValidationFailure(violations) => printf("%s is invalid: %s\n", user, violations)
        case ValidationSuccess => printf("%s is invalid\n", user)
      }
    }

//    val order = Order.fromCart(cart)

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
  implicit object DateJsonFormat extends JsonFormat[Date] {

    override def write(obj: Date) = JsString("")

    override def read(json: JsValue) : Date = json match {
      case JsString(s) => new Date()
      case _ => throw new Exception("Error info you want here ...")
    }
  }
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
//  implicit def executor = system.dispatcher
  implicit def executionContext = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val logger = Logging(system, getClass)

  val routes = {
    logRequestResult("cart") {
        (get & path("v1" / "cart" / IntNumber)) { id =>
          complete {
            val cart = Cart(id, Seq[LineItem](), Seq[ShippingInformation](), Seq[Coupon](), List[Adjustment]())
            cart
          }
        }
      }
    }

  def bind(): Unit = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
