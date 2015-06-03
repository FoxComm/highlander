import java.util.Date

import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpResponse
import com.typesafe.config.{ConfigFactory, Config}
import com.wix.accord.{validate => runValidation, Success => ValidationSuccess, Failure => ValidationFailure }
import com.wix.accord._
import dsl.{validator => createValidator}
import dsl._
import akka.event.Logging
import slick.lifted.ProvenShape
import spray.json.{JsValue, JsString, JsonFormat, DefaultJsonProtocol}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import slick.lifted.Tag

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import org.json4s.JsonAST.JString
import org.json4s.{CustomSerializer, DefaultFormats}
import org.json4s.jackson.Serialization.{write => render}
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

// Validation mixin
trait Validation {
  def validator[T]: Validator[T]
  def validate: Result = { runValidation(this)(validator) }
  def isValid: Boolean = { validate == ValidationSuccess }
}

case class StockLocation(id: Int, name: String)

// TODO: money/currency abstraction. Use joda-money, most likely
case class Money(currency: String, amount: Int)

case class State(id: Int, name: String, abbreviation: String)

case class City(id: Int, name: String)

case class Address(id: Int, name: String, streetAddresses: List[String], city: City, state: State, zip: String)

case class Adjustment(id: Int)

case class Coupon(id: Int, cartId: Int, code: String, adjustment: List[Adjustment])

case class Promotion(id: Int, cartId: Int, adjustments: List[Adjustment])

case class LineItem(id: Int, cartId: Int, skuId: Int)

sealed trait PaymentStatus
case object Auth extends PaymentStatus
case object FailedCapture extends PaymentStatus
case object CanceledAuth extends PaymentStatus
case object ExpiredAuth extends PaymentStatus

sealed trait GiftCardPaymentStatus extends PaymentStatus
case object InsufficientBalance extends GiftCardPaymentStatus
case object SuccessfulDebit extends GiftCardPaymentStatus
case object FailedDebit extends GiftCardPaymentStatus

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
}

case class CreditCard(id: Int, cartId: Int, status: PaymentStatus, cvv: Int, number: String, expiration: String, address: Address) extends Payment

case class GiftCard(id: Int, cartId: Int, status: GiftCardPaymentStatus, code: String) extends Payment

sealed trait Destination
case class EmailDestination(email: String) extends Destination
case class ResidenceDestination(address: Address) extends Destination
case class StockLocationDestination(stockLocation: StockLocation) extends Destination

case class Fulfillment(id: Int, destination: Destination)

case class Cart(id: Int, accountId: Option[Int] = None) {

  val lineItems: Seq[LineItem] = Seq.empty
  val payments: Seq[Payment] = Seq.empty
  val fulfillments: Seq[Fulfillment] = Seq.empty

//  def coupons: Seq[Coupon] = Seq.empty
//  def adjustments: Seq[Adjustment] = Seq.empty

  // TODO: how do we handle adjustment/coupon
  // specifically, promotions are handled at the checkout level, but need to display in the cart
  def addCoupon(coupon: Coupon) = {}

  // carts support guest checkout
  def isGuest = this.accountId.isDefined

  // TODO: service class it?
}

trait RichTable {
  implicit val JavaUtilDateMapper =
    MappedColumnType .base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))
}

class Carts(tag: Tag) extends Table[Cart](tag, "carts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Option[Int]]("account_id")
  def * = (id, accountId) <> ((Cart.apply _).tupled, Cart.unapply)
}

class LineItems(tag: Tag) extends Table[LineItem](tag, "line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cartId = column[Int]("cart_id")
  def skuId = column[Int]("sku_id")
  def * = (id, cartId, skuId) <> ((LineItem.apply _).tupled, LineItem.unapply)
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

case class Order(id: Int, cartId: Int, status: OrderStatus) {
  var lineItems: Seq[LineItem] = Seq.empty
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
    val order = Order(id = 0, cartId = cart.id, status = New)

    if (scala.util.Random.nextInt(2) == 1) {
      Left(List("payment re-auth failed"))
    } else {
      Right(order)
    }
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

object Main extends Formats {

  def main(args: Array[String]): Unit = {
    val service = new Service()
    service.bind()
  }
}

case class AddLineItemsRequest(skuId: Int, quantity: Int)

// JSON formatters
trait Formats extends DefaultJsonProtocol {
  def adtSerializer[T : Manifest] = () => {
    new CustomSerializer[T](format => ( {
      case _ ⇒ sys.error("Reading not implemented")
    }, {
      case x ⇒ JString(x.toString)
    }))
  }

  implicit val addLineItemsRequestFormat = jsonFormat2(AddLineItemsRequest.apply)

  val phoenixFormats = DefaultFormats + new CustomSerializer[PaymentStatus](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: PaymentStatus ⇒ JString(x.toString) }
    )) + new CustomSerializer[GiftCardPaymentStatus](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: GiftCardPaymentStatus ⇒ JString(x.toString) }
    )) + new CustomSerializer[OrderStatus](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: OrderStatus ⇒ JString(x.toString) }
    ))
}

object LineItemsCreator {
  def apply(db: PostgresDriver.backend.DatabaseDef,
            cart: Cart,
            lineItems: Seq[LineItem])
           (implicit ec: ExecutionContext): Future[Either[List[String], Seq[LineItem]]] = {
    // TODO:
    //  validate sku in PIM
    //  execute the fulfillment runner -> creates fulfillments
    //  validate inventory (might be in PIM maybe not)
    val lineItemsTable = TableQuery[LineItems]

    val actions = (for {
      _ <- lineItemsTable ++= lineItems
      items ← lineItemsTable.filter(_.cartId === cart.id).result
    } yield items).transactionally

    db.run(actions).map { items =>
      Right(items)
    }
  }
}

class Service extends Formats {
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

  // required for (de)-serialization
  implicit val formats = phoenixFormats

  val logger = Logging(system, getClass)

  val db = Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver")

  val carts = TableQuery[Carts]

  val routes = {
    val cart = Cart(id = 0, accountId = None)

    def findCart(id: Int): Future[Option[Cart]] = {
      // If we are going to punt on user authentication, then we sort of have to stub this piece out.
      // If we do auth, then we should create a cart for a user if one doesn't exist.
      db.run(carts.filter(_.id === id).result.headOption)
    }

    val notFoundResponse = HttpResponse(NotFound)

    def renderOrNotFound[T <: AnyRef](resource: Future[Option[T]],
                                      onFound: (T => HttpResponse) = (r: T) => HttpResponse(OK, entity = render(r))) = {
      resource.map { resource =>
        resource match {
          case Some(r) => onFound(r)
          case None => notFoundResponse
        }
      }
    }

    logRequestResult("cart") {
      pathPrefix("v1" / "cart" ) {
        (get & path(IntNumber)) { id =>
          complete {
            renderOrNotFound(findCart(id))
          }
        } ~
          (post & path(IntNumber / "checkout")) { id =>
            complete {
              renderOrNotFound(findCart(id), (c: Cart) => {
                new Checkout(c).checkout match {
                  case Left(errors) => HttpResponse(BadRequest, entity = render(errors))
                  case Right(order) => HttpResponse(OK, entity = render(order))
                }
              })
            }
          } ~
          (post & path(IntNumber / "line-items") & entity(as[Seq[AddLineItemsRequest]])) { (cartId, reqItems) =>
            complete {
              findCart(cartId).map { cart =>
                cart match {
                  case None => Future(notFoundResponse)
                  case Some(c) =>
                    val lineItems = reqItems.flatMap { req =>
                      (1 to req.quantity).map { i => LineItem(id = 0, cartId = cartId, skuId = req.skuId) }
                    }

                    LineItemsCreator(db, c, lineItems).map { result =>
                      result match {
                        case Left(errors) => HttpResponse(BadRequest, entity = render(errors))
                        case Right(lineItems) => HttpResponse(OK, entity = render(lineItems))
                      }
                    }
                }
              }
            }
          } ~
          (post & path(IntNumber / "persisted")) { id =>
            complete {
              val carts = TableQuery[Carts]
              val actions = (for {
                _ ← carts += Cart(1, Some(3))
                _ ← carts += Cart(2, None)
                _ ← carts += Cart(3, Some(2))
                _ ← carts.filter(_.id === 1).delete
                l ← carts.length.result
              } yield l).transactionally

              /** If we are not using transactionally here then we need to run and await the schema creation first.
                * For performance reasons slick does not guarantee that actions are executed in order by default.
                *
                * Use withPinnedSession if you want to share the same session but don’t want a transaction. */

              /** Slick 3 now returns a Future. Spray-routing also accepts a Future to complete a route, so we’re fine. */
              db.run(actions).map { length ⇒

                /** Seems that spray-json can’t work with a Map[String, Any], so need to call toString here. */
                render(Map("hi" → "hello", "length" → length.toString))
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
