import java.util.Date
import com.stripe.model.Token
import com.stripe.model.Charge
import com.stripe.Stripe

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

case class Store(id: Int, name: String, Configuration: StoreConfiguration)

//  This is a super quick placeholder for store configuration.  We will want to blow this out later.
// TODO: Create full configuration data model
case class StoreConfiguration(id: Int, storeId: Int, PaymentGateway: PaymentGateway)

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

sealed trait CreditCardPaymentStatus extends PaymentStatus
case object Auth extends CreditCardPaymentStatus
case object FailedCapture extends CreditCardPaymentStatus
case object CanceledAuth extends CreditCardPaymentStatus
case object ExpiredAuth extends CreditCardPaymentStatus

sealed trait GiftCardPaymentStatus extends PaymentStatus
case object InsufficientBalance extends GiftCardPaymentStatus
case object SuccessfulDebit extends GiftCardPaymentStatus
case object FailedDebit extends GiftCardPaymentStatus

abstract class Payment extends DefaultJsonProtocol {
  def authorize: {}

  def process: Option[String] = {
    println("processing payment")
    if (scala.util.Random.nextInt(2) == 1) {
      None
    } else {
      Some("payment processing failed")
    }
  }
}



abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway
// TODO: Get the API key from somewhere more useful.
case class StripeGateway(paymentToken: String, apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  def validateToken: Boolean = {
    Stripe.apiKey = this.apiKey
    try {
      val retrievedToken = Token.retrieve(this.paymentToken)
      val card = retrievedToken.getCard
      println(card)
    } catch {
      case ire: com.stripe.exception.InvalidRequestException =>
        println(ire)
        return false
    }
    true
  }
}


abstract class PaymentMethod extends DefaultJsonProtocol {
  def validate: Boolean = {
    scala.util.Random.nextInt(2) == 1
  }

  def addToGuestCheckout: Boolean = {
    true
  }

  def addToUserWallet: Boolean = {
    true
  }
}

object PaymentMethods {
  val tokenCardsTable = TableQuery[TokenizedCreditCards]

  // TODO: The right way to do this would be to return all the different payment methods available to the user.
  def findAllByAccount(db: PostgresDriver.backend.DatabaseDef, account: Account): Future[Seq[PaymentMethod]] = {
    db.run(table.filter(_.accountId === account.id).result)
  }

}


// TODO: Figure out how to have the 'status' field on the payment and not the payment method.
case class CreditCard(id: Int, cartId: Int, cardholderName: String, cardNumber: String, cvv: Int, status: CreditCardPaymentStatus, expiration: String, address: Address) extends PaymentMethod
// We should probably store the payment gateway on the card itself.  This way, we can manage a world where a merchant changes processors.
case class TokenizedCreditCard(id: Int, walletId: Int, paymentGateway: PaymentGateway, gatewayTokenId: String, gatewayUserEmail: String) extends PaymentMethod
case class GiftCard(id: Int, cartId: Int, status: GiftCardPaymentStatus, code: String) extends PaymentMethod

// TODO: Decide if we should take some kind of STI approach here!
class TokenizedCreditCards(tag: Tag) extends Table[TokenizedCreditCard](tag, "tokenized_credit_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def wallet_id = column[Int]("wallet_id")
  def payment_gateway = column[String]("payment_gateway")
  def gateway_token_id = column[String]("gateway_token_token_id")
  def gateway_user_email = column[String]("gateway_user_email")
}

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
  def checkout: Order Or List[ErrorMessage] = {
    // Realistically, what we'd do here is actually
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/couponsi
    // 5) Final Auth on the payment
    val order = Order(id = 0, cartId = cart.id, status = New)

    if (scala.util.Random.nextInt(2) == 1) {
      Bad(List("payment re-auth failed"))
    } else {
      Good(order)
    }
  }
}


// We should have accounts.  Users and Shoppers can have accounts.
case class Shopper(id: Int, email: String, password: String, firstName: String, lastName: String) extends Validation {
  override def validator[T] = {
    createValidator[Shopper] { shopper =>
      shopper.firstName is notEmpty
      shopper.lastName is notEmpty
      shopper.email is notEmpty
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

case class LineItemsPayload(skuId: Int, quantity: Int)
case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)
case class TokenizedPaymentMethodPayload(paymentGateway: String, paymentGatewayToken: String) extends Validation {
  override def validator[T] = {
    createValidator[TokenizedPaymentMethodPayload] { tokenizedPaymentMethodPayload =>
      tokenizedPaymentMethodPayload.paymentGateway is notEmpty
      tokenizedPaymentMethodPayload.paymentGatewayToken is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me!
}

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
  implicit val addPaymentMethodRequestFormat = jsonFormat4(PaymentMethodPayload.apply)
  implicit val addTokenizedPaymentMethodRequestFormat = jsonFormat2(TokenizedPaymentMethodPayload.apply)


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
    val counts = (for {
      (skuId, q) <- lineItems.filter(_.cartId === cart.id).groupBy(_.skuId)
    } yield (skuId, q.length))

    /*
      TODO: let's do this transactionally to avoid data racing
            maybe we could also leverage slick for a single for comprehension?
     */
    db.run(counts.result).flatMap { items =>
      items.foreach { case (skuId, current) =>
        val newQuantity = updateQuantities.getOrElse(skuId, 0)

        // we're using absolute values from payload, so if newQuantity is greater than create the N items
        if (newQuantity > current) {
          val delta = newQuantity - current
          db.run(for {
            _ <- lineItems ++= (1 to delta).map { _ => LineItem(0, cart.id, skuId) }.toSeq
          } yield ())
        } else if (newQuantity < current && current - newQuantity > 0) {
          db.run(for {
            _ <- lineItems.filter(_.id in lineItems.filter(_.cartId === cart.id).filter(_.skuId === skuId).
                    sortBy(_.id.asc).take(current - newQuantity).map(_.id)).delete
          } yield ())
        }
      }

      db.run(lineItems.filter(_.cartId === cart.id).result).map { results =>
        Good(results)
      }
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

    def findAccount(id: Option[Int]): Option[Shopper] = {
      id match{
        case None =>
          None
        case Some(id) =>
          Some(new Shopper(id, "donkey@donkey.com", "donkeyPass", "Mister", "Donkey"))
      }
    }

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
            renderOrNotFound(findCart(id))
          }
        } ~
        (post & path(IntNumber / "checkout")) { id =>
          complete {
            renderOrNotFound(findCart(id), (c: Cart) => {
              new Checkout(c).checkout match {
                case Good(order) => HttpResponse(OK, entity = render(order))
                case Bad(errors) => HttpResponse(BadRequest, entity = render(errors))
              }
            })
          }
        } ~
        (post & path(IntNumber / "line-items") & entity(as[Seq[LineItemsPayload]])) { (cartId, reqItems) =>
          complete {
            findCart(cartId).map {
              case None => Future(notFoundResponse)
              case Some(c) =>
                LineItemUpdater(db, c, reqItems).map {
                  case Bad(errors)      => HttpResponse(BadRequest, entity = render(errors))
                  case Good(lineItems)  => HttpResponse(OK, entity = render(lineItems))
                }
            }
          }
        } ~
          (get & path(IntNumber) / "payment-methods") { cartId =>
            complete {
              renderOrNotFound(findCart(id))
            }
          } ~
        (post & path(IntNumber / "payment-methods") & entity(as[PaymentMethodPayload])) { (cartId, reqPayment) =>
          complete {
            findCart(cartId).map {
              //can't add payment methods if the cart doesn't exist
              case None => notFoundResponse
              case Some(c) =>
                HttpResponse(OK, entity = render("HI"))
            }
          }
        } ~
        (post & path(IntNumber / "tokenized-payment-methods") & entity(as[TokenizedPaymentMethodPayload])) { (cartId, reqPayment) =>
          complete {
            findCart(cartId).map {
              case None => notFoundResponse
              case Some(c) =>

                // First, ensure that the token is valid.
                new StripeGateway(reqPayment.paymentGatewayToken).validateToken match {
                  case true =>
                    println("This was a valid stripe payment token.")

                    // Next, check to see if there is a user associated with the checkout.
                    findAccount(c.accountId) match {
                      case None =>
                        HttpResponse(OK, entity = render("Guest checkout!!"))
                      case Some(s) =>

                        HttpResponse(OK, entity = render("Authed Checkout"))
                    }
                  case false =>
                    println("Stripe payment token was invalid")
                    HttpResponse (OK, entity = render ("Stripe payment token was invalid!") )
                }

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
