//import akka.http.scaladsl.unmarshalling.Unmarshal
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
import slick.lifted.ProvenShape
import spray.json.{JsValue, JsString, JsonFormat, DefaultJsonProtocol}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import slick.driver.H2Driver.api._
import slick.lifted.Tag

// Validation mixin
trait Validation {
  def validator[T]: Validator[T]
  def validate: Result = { runValidation(this)(validator) }
  def isValid: Boolean = { validate == ValidationSuccess }
}

case class StockLocation(id: Int, name: String)
object StockLocation extends DefaultJsonProtocol {
  implicit val stockLocationFormat = jsonFormat2(StockLocation.apply)
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

case class LineItem(id: Int, skuId: Int)
object LineItem extends DefaultJsonProtocol {
  implicit val lineItemFormat = jsonFormat2(LineItem.apply)
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

sealed trait Destination
case class EmailDestination(email: String) extends Destination
case class ResidenceDestination(address: Address) extends Destination
case class StockLocationDestination(stockLocation: StockLocation) extends Destination

object Destination extends DefaultJsonProtocol {
  implicit object destinationFormat extends JsonFormat[Destination] {
    def write(obj: Destination) = JsString(obj.toString)

    def read(json: JsValue): Destination = json match {
      case _ => throw new Exception("could not parse")
    }
  }
}

object EmailDestination extends DefaultJsonProtocol {
  implicit val emailDestinationFormat = jsonFormat1(EmailDestination.apply)
}
object ResidenceDestination extends DefaultJsonProtocol {
  implicit val residenceDestination = jsonFormat1(ResidenceDestination.apply)
}
object StockLocationDestination extends DefaultJsonProtocol {
  implicit val stockLocationDestinationFormat = jsonFormat1(StockLocationDestination.apply)
}

case class Fulfillment(id: Int, destination: Destination)
object Fulfillment extends DefaultJsonProtocol {
  implicit val fulfillmentFormat = jsonFormat2(Fulfillment.apply)
}

case class BasicCart(id: Int, userId: Int)
object BasicCart extends DefaultJsonProtocol {
  implicit val fulfillmentFormat = jsonFormat2(BasicCart.apply)
}

case class Cart(id: Int, userId: Option[Int] = None, lineItems: Seq[LineItem],
//                payments: Seq[Payment],
                fulfillments: Seq[Fulfillment],
                coupons: Seq[Coupon], adjustments: List[Adjustment]) {
  // TODO: how do we handle adjustment/coupon
  // specifically, promotions are handled at the checkout level, but need to display in the cart
  def addCoupon(coupon: Coupon) = {}

  // carts support guest checkout
  def isGuest = this.userId.isDefined

  def update(request: UpdateCartRequest) = {
    //lineItems = updateCartRequest.lineItems)
  }

  // TODO: service class it?
  def addLineItems(items: Seq[LineItem]): Cart = {
    this.copy(lineItems = this.lineItems ++ items)
    // TODO: execute the fulfillment runner
  }
}

object Cart extends DefaultJsonProtocol {
  implicit val cartFormat = jsonFormat6(Cart.apply)
}

class Carts(tag: Tag) extends Table[BasicCart](tag, "carts") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def * = (id, userId) <> ((BasicCart.apply _).tupled, BasicCart.unapply)
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

object OrderStatus extends DefaultJsonProtocol {
  implicit object orderStatusFormat extends JsonFormat[OrderStatus] {
    def write(obj: OrderStatus) = JsString(obj.toString)

    def read(json: JsValue): OrderStatus = json match {
      case JsString("New") => New
      case JsString("FraudHold") => FraudHold
      case JsString("RemorseHold") => RemorseHold
      case JsString("ManualHold") => ManualHold
      case JsString("Canceled") => Canceled
      case JsString("FulfillmentStarted") => FulfillmentStarted
      case JsString("PartiallyShipped") => PartiallyShipped
      case JsString("Shipped") => Shipped
      case _ => throw new Exception("could not parse")
    }
  }
}

case class Order(id: Int, cartId: Int, status: OrderStatus, lineItems: Seq[LineItem],
//                 payment: Seq[Payment],
                 //deliveries: Seq[ShippingInformation],
                 adjustments: List[Adjustment]) {
//  def masterStatus: OrderStatus = New
}

object Order extends DefaultJsonProtocol {
  implicit val orderFormat = jsonFormat5(Order.apply)
}

case class StockItem(id: Int, productId: Int, stockLocationId: Int, onHold: Int, onHand: Int, allocatedToSales: Int) {
  def available: Int = {
    this.onHand - this.onHold - this.allocatedToSales
  }
}

class Checkout(cart: Cart) {
  def checkout: Either[List[String], Order] = {
    // Realistically, what we'd do here is actually
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/coupons
    val order = Order(id = 0, cartId = cart.id, status = New, lineItems = cart.lineItems,
      //deliveries = cart.deliveries,
      adjustments = cart.adjustments)

    Right(order)
  }
}

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
    val service = new Service()
    service.bind()
  }
}

// Request cases
case class UpdateCartRequest(lineItems: Seq[LineItem])

case class AddLineItemsRequest(skuId: Int, quantity: Int)

// JSON formatters
trait Protocols extends DefaultJsonProtocol {
  implicit val updateCartRequestFormat = jsonFormat1(UpdateCartRequest.apply)
  implicit val addLineItemsRequestFormat = jsonFormat2(AddLineItemsRequest.apply)

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
  implicit def executionContext = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val logger = Logging(system, getClass)

  val routes = {
    val cart = Cart(id = 0, lineItems = Seq[LineItem](), fulfillments = Seq[Fulfillment](),
                    coupons = Seq[Coupon](), adjustments = List[Adjustment]())

    def findCart(id: Int): Cart = {
      // If we are going to punt on user authentication, then we sort of have to stub this piece out.
      // If we do auth, then we should create a cart for a user if one doesn't exist.
      cart.copy(id = id)
    }

    logRequestResult("cart") {
      pathPrefix("v1" / "cart" ) {
        (get & path(IntNumber)) { id =>
          complete {
            findCart(id)
          }
        } ~
        (patch & path(IntNumber) & entity(as[UpdateCartRequest])) { (id, updateCartRequest) =>
          complete {
            val cart = findCart(id)
            cart
            // cart.update(updateCartRequest)
          }
        } ~
          (post & path(IntNumber / "line-items") & entity(as[Seq[AddLineItemsRequest]])) { (cartId, reqItems) =>
            complete {
              val lineItems = reqItems.flatMap { req =>
                (1 to req.quantity).map{ i => LineItem(id = 0, skuId = req.skuId) }
              }

              val cart = findCart(cartId)
              cart.addLineItems(lineItems)
            }
          } ~
        (post & path(IntNumber / "checkout")) { id =>
          complete {
            new Checkout(findCart(id)).checkout
          }
        } ~
          (post & path(IntNumber / "persisted")) { id =>
            complete {
              val carts = TableQuery[Carts]
              val db = Database.forURL("jdbc:h2:mem:hello", driver = "org.h2.Driver")
              db.withSession { implicit session =>
                carts.schema.create

                carts += BasicCart(1,3)
                carts += BasicCart(2,2)
                carts += BasicCart(2,2)

                carts.filter(_.id === 1).delete

                val x = carts.filter(_.id === 2)

                Map("hi" -> "hello")
              }
            }
          }

      }
    }
  }

  // when lineItem is added to cart then execute fulfillment runner, digital item or something else? we'll create
  // fulfillment at that moment

  def bind(): Unit = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}

/*
 TODO(yax): consider/research/experiment w/ the following
  - DB Migrations: https://github.com/flyway/flyway
  - Validations:
    - http://skinny-framework.org/documentation/validator.html (this seems simpler than accord but at what cost)
    - https://github.com/wix/accord
  - ORM: http://skinny-framework.org/documentation/orm.html
  - non-ORM:
    - https://github.com/mauricio/postgresql-async
    - Slick: https://github.com/slick/slick
  - Fixtures/Factories: http://skinny-framework.org/documentation/factory-girl.html
  - JSON: should we replace spray-json w/ json4s?
 */
