import java.util.Date

import org.scalactic.{Bad, Good, ErrorMessage, Or}
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

class States(tag: Tag) extends Table[State](tag, "states") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def abbreviation = column[String]("abbreviation")

  def * = (id, name, abbreviation) <> ((State.apply _).tupled, State.unapply)
}

case class Address(id: Int, accountId: Int, stateId: Int, name: String, street1: String, street2: Option[String],
                   city: String, zip: String) extends Validation {
  override def validator[T] = {
    createValidator[Address] { address =>
      address.name is notEmpty
      address.street1 is notEmpty
      address.city is notEmpty
      address.zip.length is equalTo(5)
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

class Addresses(tag: Tag) extends Table[Address](tag, "addresses") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Int]("account_id")
  def stateId = column[Int]("state_id")
  def name = column[String]("name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")

  def * = (id, accountId, stateId, name, street1, street2, city, zip) <> ((Address.apply _).tupled, Address.unapply)

  def state = foreignKey("addresses_state_id_fk", stateId, TableQuery[States])(_.id)
}

object Addresses {
  val table = TableQuery[Addresses]

  def findAllByAccount(db: PostgresDriver.backend.DatabaseDef, account: User): Future[Seq[Address]] = {
    db.run(table.filter(_.accountId === account.id).result)
  }

  def findById(db: PostgresDriver.backend.DatabaseDef, id: Int): Future[Option[Address]] = {
    db.run(table.filter(_.id === id).result.headOption)
  }

  def createFromPayload(db: PostgresDriver.backend.DatabaseDef,
                        account: User,
                        payload: Seq[CreateAddressPayload])
                       (implicit ec: ExecutionContext): Future[Seq[Address] Or Seq[ErrorMessage]] = {
    // map to Address & validate
    val results = payload.map { a =>
      val address = Address(id = 0, accountId = account.id, stateId = a.stateId, name = a.name,
                            street1 = a.street1, street2 = a.street2, city = a.city, zip = a.zip)
      (address, address.validate)
    }

    val failures = results.filter(_._2 == Failure)

    if (failures.nonEmpty) {
      Future.successful(Bad(failures.map(ErrorMessage(_._2)))
    } else {
      db.run(for {
        _ <- table ++= results.map(_._1)
        addresses <- table.filter(_.accountId === account.id).result
      } yield (addresses))

    }
  }
}

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

object Carts {
  val table = TableQuery[Carts]

  def findById(db: PostgresDriver.backend.DatabaseDef, id: Int): Future[Option[Cart]] = {
    db.run(table.filter(_.id === id).result.headOption)
  }
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
  def checkout: Order Or List[ErrorMessage] = {
    // Realistically, what we'd do here is actually
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/coupons
    val order = Order(id = 0, cartId = cart.id, status = New)

    if (scala.util.Random.nextInt(2) == 1) {
      Bad(List("payment re-auth failed"))
    } else {
      Good(order)
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

class Users(tag: Tag) extends Table[User](tag, "accounts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def hashedPassword = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")

  def * = (id, email, hashedPassword, firstName, lastName) <> ((User.apply _).tupled, User.unapply)
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

case class LineItemsPayload(skuId: Int, quantity: Int)
case class CreateAddressPayload(name: String, stateId: Int, street1: String, street2: Option[String], city: String, zip: String)

// JSON formatters
trait Formats extends DefaultJsonProtocol {
  def adtSerializer[T : Manifest] = () => {
    new CustomSerializer[T](format => ( {
      case _ ⇒ sys.error("Reading not implemented")
    }, {
      case x ⇒ JString(x.toString)
    }))
  }

  implicit val addLineItemsRequestFormat = jsonFormat2(LineItemsPayload.apply)
  implicit val createAddressPayloadFormat = jsonFormat6(CreateAddressPayload.apply)

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

object LineItemUpdater {
  def apply(db: PostgresDriver.backend.DatabaseDef,
            cart: Cart,
            payload: Seq[LineItemsPayload])
           (implicit ec: ExecutionContext): Future[Seq[LineItem] Or List[ErrorMessage]] = {

    // TODO:
    //  validate sku in PIM
    //  execute the fulfillment runner -> creates fulfillments
    //  validate inventory (might be in PIM maybe not)
    //  run hooks to manage promotions

    val lineItems = TableQuery[LineItems]

    // reduce Seq[LineItemsPayload] -> Map(skuId: Int -> absoluteQuantity: Int)
    val updateQuantities = payload.foldLeft(Map[Int, Int]()) { (acc, item) =>
      val quantity = acc.getOrElse(item.skuId, 0)
      acc.updated(item.skuId, quantity + item.quantity)
    }

    // select sku_id, count(1) from line_items where cart_id = $ group by sku_id
    val counts = for {
      (skuId, q) <- lineItems.filter(_.cartId === cart.id).groupBy(_.skuId)
    } yield (skuId, q.length)

    val queries = counts.result.flatMap { (items: Seq[(Int, Int)]) =>
      val existingSkuCounts = items.toMap

      val changes = updateQuantities.map { case (skuId, newQuantity) =>
        val current = existingSkuCounts.getOrElse(skuId, 0)
        // we're using absolute values from payload, so if newQuantity is greater then create N items
        if (newQuantity > current) {
          val delta = newQuantity - current

          lineItems ++= (1 to delta).map { _ => LineItem(0, cart.id, skuId) }.toSeq
        } else if (current - newQuantity > 0) { //otherwise delete N items
          lineItems.filter(_.id in lineItems.filter(_.cartId === cart.id).filter(_.skuId === skuId).
                    sortBy(_.id.asc).take(current - newQuantity).map(_.id)).delete
        } else {
          // do nothing
          DBIO.successful({})
        }
      }.to[Seq]

      DBIO.seq(changes: _*)
    }.flatMap { _ ⇒
      lineItems.filter(_.cartId === cart.id).result
    }

    db.run(queries.transactionally).map(items => Good(items))
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

  val user = User(id = 1, email = "yax@foxcommerce.com", password = "donkey", firstName = "Yax", lastName = "Donkey")

  val routes = {
    val cart = Cart(id = 0, accountId = None)

    val notFoundResponse = HttpResponse(NotFound)

    def renderOrNotFound[T <: AnyRef](resource: Future[Option[T]],
                                      onFound: (T => HttpResponse) = (r: T) => HttpResponse(OK, entity = render(r))) = {
      resource.map {
        case Some(r) => onFound(r)
        case None => notFoundResponse
      }
    }

    logRequestResult("cart") {
      pathPrefix("v1" / "cart" ) {
        (get & path(IntNumber)) { id =>
          complete {
            renderOrNotFound(Carts.findById(db, id))
          }
        } ~
        (post & path(IntNumber / "checkout")) { id =>
          complete {
            renderOrNotFound(Carts.findById(db, id), (c: Cart) => {
              new Checkout(c).checkout match {
                case Good(order) => HttpResponse(OK, entity = render(order))
                case Bad(errors) => HttpResponse(BadRequest, entity = render(errors))
              }
            })
          }
        } ~
        (post & path(IntNumber / "line-items") & entity(as[Seq[LineItemsPayload]])) { (cartId, reqItems) =>
          complete {
            Carts.findById(db, cartId).map {
              case None => Future(notFoundResponse)
              case Some(c) =>
                LineItemUpdater(db, c, reqItems).map {
                  case Bad(errors)      => HttpResponse(BadRequest, entity = render(errors))
                  case Good(lineItems)  => HttpResponse(OK, entity = render(lineItems))
                }
            }
          }
        }
      }
    } ~
    logRequestResult("addresses") {
      pathPrefix("v1" / "addresses" ) {
        get {
          complete {
            Addresses.findAllByAccount(db, user).map { addresses =>
              HttpResponse(OK, entity = render(addresses))
            }
          }
        } ~
        (post & entity(as[Seq[CreateAddressPayload]])) { payload =>
          complete {
            Addresses.createFromPayload(db, user, payload).map {
              case Good(addresses)  => HttpResponse(OK, entity = render(addresses))
              case Bad(errors)      => HttpResponse(BadRequest, entity = render(errors))
            }
          }
        }
      }
    }
  }

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
