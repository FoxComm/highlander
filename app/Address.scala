import java.util.Date
import java.util.concurrent.TimeoutException
import com.stripe.model.Token
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import com.stripe.model.{Charge => StripeCharge}
import com.stripe.Stripe

import org.scalactic.{Bad, Good, ErrorMessage, Or}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpResponse
import com.typesafe.config.{ConfigFactory, Config}
import com.wix.accord._
import com.wix.accord.{validate => runValidation, Success => ValidationSuccess, Failure => ValidationFailure}
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
import org.json4s.jackson.JsonMethods._
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}
import collection.JavaConversions.mapAsJavaMap

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
case object Applied extends CreditCardPaymentStatus
case object Auth extends CreditCardPaymentStatus
case object FailedCapture extends CreditCardPaymentStatus
case object CanceledAuth extends CreditCardPaymentStatus
case object ExpiredAuth extends CreditCardPaymentStatus

sealed trait GiftCardPaymentStatus extends PaymentStatus
case object InsufficientBalance extends GiftCardPaymentStatus
case object SuccessfulDebit extends GiftCardPaymentStatus
case object FailedDebit extends GiftCardPaymentStatus

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway
// TODO: Get the API key from somewhere more useful.
case class StripeGateway(paymentToken: String, apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  def getTokenizedCard: Try[TokenizedCreditCard] = {
    println("Inside getTokenizedCard")
    Stripe.apiKey = this.apiKey
    try {
      val retrievedToken = Token.retrieve(this.paymentToken)
      println(retrievedToken.getCard)
      val stripeCard = retrievedToken.getCard
      val mergedCard = new TokenizedCreditCard(paymentGateway = "stripe",
        gatewayTokenId = this.paymentToken,
        lastFourDigits = stripeCard.getLast4,
        expirationMonth = stripeCard.getExpMonth,
        expirationYear = stripeCard.getExpYear,
        brand = stripeCard.getBrand
      )
      Success(mergedCard)
    } catch {
      case t: com.stripe.exception.InvalidRequestException =>
        Failure(t)
    }
  }

  def authorizeAmount(tokenizedCard: TokenizedCreditCard, amount: Int): String Or List[ErrorMessage] = {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
                                             "source" -> tokenizedCard.gatewayTokenId, "capture" -> capture)
    val reqOpts = StripeRequestOptions.builder().setApiKey(this.apiKey).build()

    try {
      val charge = StripeCharge.create(mapAsJavaMap(chargeMap), reqOpts)
      /*
       TODO: https://stripe.com/docs/api#create_charge
       Since we're using tokenized, we presumably pass verification process, but might want to handle here
       */
      Good(charge.getId)
    } catch {
      case t: com.stripe.exception.StripeException =>
        Bad(List(t.getMessage))
    }
  }
}

abstract class PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage]
}

object PaymentMethods {
  import scala.concurrent.ExecutionContext.Implicits.global

  // ONLY implmenting tokenized payment methods right now.
  // Next up will be full credit cards
  val tokenCardsTable = TableQuery[TokenizedCreditCards]

  // TODO: The right way to do this would be to return all the different payment methods available to the user.
  def findAllByAccount(db: PostgresDriver.backend.DatabaseDef, account: Shopper): Future[Seq[TokenizedCreditCard]] = {
    db.run(tokenCardsTable.filter(_.accountId === account.id).result)
  }

  // TODO: Figure out our standard 'return' objects for all inserts and lookups
  def addPaymentTokenToAccount(db: PostgresDriver.backend.DatabaseDef, paymentToken: String, account: Shopper) : Future[TokenizedCreditCard] = {
    // First, let's get this token from stripe.
    // TODO: Let's handle a bad response from stripe and bubble up to the user
    val gateWay = StripeGateway(paymentToken = paymentToken)
    gateWay.getTokenizedCard match {
      case Success(card) =>
        val cardToSave = card.copy(accountId = account.id)
        /** Can be used like 'tokenCardsTable', but returns the newly inserted ID */
        val newlyInsertedId = tokenCardsTable.returning(tokenCardsTable.map(_.id))

        val insertAction = (newlyInsertedId += cardToSave).map { newId: Int ⇒
          /** Transform the result of the database query after it is run from Int -> TokenizedCreditCard */
          cardToSave.copy(id = newId)
        }

        db.run(insertAction)
      case Failure(t) => Future.failed(t)
    }
  }

  // TODO: Make polymorphic for real.
  def findById(db: PostgresDriver.backend.DatabaseDef, id: Int): Future[Option[TokenizedCreditCard]] = {
    db.run(tokenCardsTable.filter(_.id === id).result.headOption)
  }
}


// TODO: Figure out how to have the 'status' field on the payment and not the payment method.
case class CreditCard(id: Int, cartId: Int, cardholderName: String, cardNumber: String, cvv: Int, status: CreditCardPaymentStatus, expiration: String, address: Address) extends PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage] = {
    Good("authenticated")
  }
}
// We should probably store the payment gateway on the card itself.  This way, we can manage a world where a merchant changes processors.
case class TokenizedCreditCard(id: Int = 0, accountId: Int = 0, paymentGateway: String, gatewayTokenId: String, lastFourDigits: String, expirationMonth: Int, expirationYear: Int, brand: String) extends PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage] = {
    this.paymentGateway.toLowerCase match {
      case "stripe" =>
        StripeGateway(paymentToken = this.gatewayTokenId).authorizeAmount(this, amount.toInt) match {
          case Good(chargeId) => Good(chargeId)
          case Bad(errorList) => Bad(errorList)
        }
      case gateway => Bad(List(s"Could Not Recognize Payment Gateway $gateway"))
    }
  }

}
case class GiftCard(id: Int, cartId: Int, status: GiftCardPaymentStatus, code: String) extends PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage] = {
    Good("authenticated")
  }
}

// TODO: Decide if we should take some kind of STI approach here!
class TokenizedCreditCards(tag: Tag) extends Table[TokenizedCreditCard](tag, "tokenized_credit_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Int]("account_id")
  def paymentGateway = column[String]("payment_gateway")
  def gatewayTokenId = column[String]("gateway_token_id")
  def lastFourDigits = column[String]("last_four_digits")
  def expirationMonth = column[Int]("expiration_month")
  def expirationYear = column[Int]("expiration_year")
  def brand = column[String]("brand")
  def * = (id, accountId, paymentGateway, gatewayTokenId, lastFourDigits, expirationMonth, expirationYear, brand) <> ((TokenizedCreditCard.apply _).tupled, TokenizedCreditCard.unapply)
}
//
//object TokenizedCreditCards {
//
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  def findById(db: PostgresDriver.backend.DatabaseDef = defaultdb)
//}

case class AppliedPayment(id: Int = 0,
                           cartId: Int,
                           paymentMethodId: Int,
                           paymentMethodType: String,
                           appliedAmount: Float,
                           status: String,
                           responseCode: String)

class AppliedPayments(tag: Tag) extends Table[AppliedPayment](tag, "applied_payments") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cartId = column[Int]("cart_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[String]("payment_method_type")
  def appliedAmount = column[Float]("applied_amount")
  def status = column[String]("status")
  def responseCode = column[String]("response_code")
  def * = (id, cartId, paymentMethodId, paymentMethodType, appliedAmount, status, responseCode) <> ((AppliedPayment.apply _).tupled, AppliedPayment.unapply )
}

sealed trait Destination
case class EmailDestination(email: String) extends Destination
case class ResidenceDestination(address: Address) extends Destination
case class StockLocationDestination(stockLocation: StockLocation) extends Destination

case class Fulfillment(id: Int, destination: Destination)

case class Cart(id: Int, accountId: Option[Int] = None) {
  val db = Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver")

  val lineItems: Seq[LineItem] = Seq.empty
  //val payments: Seq[AppliedPayment] = Seq.empty
  val fulfillments: Seq[Fulfillment] = Seq.empty

//  def coupons: Seq[Coupon] = Seq.empty
//  def adjustments: Seq[Adjustment] = Seq.empty

  // TODO: how do we handle adjustment/coupon
  // specifically, promotions are handled at the checkout level, but need to display in the cart
  def addCoupon(coupon: Coupon) = {}

  // carts support guest checkout
  def isGuest = this.accountId.isDefined

  // TODO: service class it?

  def payments: Future[Seq[AppliedPayment]] = {
    Carts.findPaymentMethods(db, this)
  }

  def subTotal: Int = {
    10000 //in cents?
  }

  def grandTotal: Int = {
    12550
  }
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
  val cartsTable = TableQuery[Carts]
  val tokenCardsTable = TableQuery[TokenizedCreditCards]
  val appliedPaymentsTable = TableQuery[AppliedPayments]

  // What do we return here?  I still don't have a clear STI approach in mind.  So maybe just tokenized cards for now.
  // Ideally, we would return a generic list of payment methods of all types (eg. giftcards, creditcards, store-credit)
  def findPaymentMethods(db: PostgresDriver.backend.DatabaseDef, cart: Cart): Future[Seq[AppliedPayment]] = {
    val appliedpayment = AppliedPayment(id = 1, cartId = cart.id, paymentMethodId = 1, paymentMethodType = "TokenizedCard", appliedAmount = 10000, status = Applied.toString, responseCode = "")
    val appliedpayment2 = appliedpayment.copy(appliedAmount = 2550, paymentMethodId = 2)

    // The whole of the above is to have one passing token and one failing token.  So paymentMethod with ID 1 should be real.
    // PaymentMethod with ID 2 should be fake.

    Future.successful(Seq(appliedpayment, appliedpayment2))

    //val appliedIds = appliedPaymentsTable.returning(appliedPaymentsTable.map(_.paymentMethodId))

    // I tried a monadic join here and failed.
//    val filteredPayments = for {
//      ap <- appliedPaymentsTable if ap.cartId === cartId
//      tc <- tokenCardsTable if tc.id === ap.paymentMethodId
//    } yield (tc.id, tc.accountId, tc.paymentGateway, tc.gatewayTokenId, tc.lastFourDigits, tc.expirationMonth, tc.expirationYear, tc.brand)
//    db.run(filteredPayments.head)

    // TODO: Yax or Ferdinand: Help me filter all the TokenizedCards through the mapping table of applied_payments that belong to this cart.
  }

  def addPaymentMethod(db: PostgresDriver.backend.DatabaseDef, cartId: Int, paymentMethod: PaymentMethod): Boolean = {
    true
  }

  def findById(db: PostgresDriver.backend.DatabaseDef, id: Int): Future[Option[Cart]] = {
    db.run(cartsTable.filter(_.id === id).result.headOption)
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
  import scala.concurrent.ExecutionContext.Implicits.global
  // I don't really want to pass this around until I know we should!  - AW
  val db = Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver")

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

  def verifyInventory: List[ErrorMessage] = {
    // TODO: Call the inventory service and verify that inventory exists for all items in cart
    List.empty
  }

  def authenticatePayments: List[ErrorMessage] = {
    // Really, this should authenticate all payments, at their specified 'applied amount.'
    val paymentMethods = cart.payments.flatMap { payments =>

      payments.map { pmt =>
        PaymentMethods.findById(db, pmt.paymentMethodId).flatMap  {
            case Some(c) =>
              val paymentAmount = pmt.appliedAmount
              c.authenticate(paymentAmount) match {
                case Bad(errors) => List(new ErrorMessage)
                case Good(successStr) =>
                  println("Happy as  motherfucker!")
                  List.empty
              }
            case None => List(new ErrorMessage)
          }
      }
    }
  }

  def validateAddresses: List[ErrorMessage] = {
    List.empty
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

////////////////////
// PAYLOADS
////////////////////
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

  val carts = TableQuery[Carts]

  val routes = {
    val cart = Cart(id = 0, accountId = None)


    def findAccount(id: Option[Int]): Option[Shopper] = id.flatMap { id =>
      Some(Shopper(id = id, email = "donkey@donkey.com", password = "donkeyPass",
                   firstName = "Mister", lastName = "Donkey"))
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
        } ~
          (get & path(IntNumber / "payment-methods")) { cartId =>
            complete {
              renderOrNotFound(Carts.findById(db, cartId))
            }
          } ~
        (post & path(IntNumber / "payment-methods") & entity(as[PaymentMethodPayload])) { (cartId, reqPayment) =>
          complete {
            Carts.findById(db, cartId).map {
              //can't add payment methods if the cart doesn't exist
              case None => notFoundResponse
              case Some(c) =>
                HttpResponse(OK, entity = render("HI"))
            }
          }
        } ~
        (post & path(IntNumber / "tokenized-payment-methods") & entity(as[TokenizedPaymentMethodPayload])) { (cartId, reqPayment) =>
          complete {
            Carts.findById(db, cartId).flatMap {
              case None => Future.successful(notFoundResponse)
              case Some(c) =>
                // Check to see if there is a user associated with the checkout.
                findAccount(c.accountId) match {
                  case None     =>
                    Future.successful(HttpResponse(OK, entity = render("Guest checkout!!")))

                  case Some(s)  =>
                    // Persist the payment token to the user's account
                    PaymentMethods.addPaymentTokenToAccount(db, reqPayment.paymentGatewayToken, s).map { x =>
                      HttpResponse(OK, entity = render(x))
                    }
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
