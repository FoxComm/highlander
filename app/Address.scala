import java.util.Date
import java.util.concurrent.TimeoutException
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import com.stripe.model.Token
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import com.stripe.model.{Charge => StripeCharge}
import com.stripe.Stripe

import org.scalactic._
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
import akka.http.scaladsl.server.directives._

import utils.{RichTable, Validation}
import models._
import payloads._
import responses.FullCart
import services.{LineItemUpdater, PaymentGateway, Checkout, TokenizedPaymentCreator, Authenticator, CreditCardPaymentCreator}

case class Store(id: Int, name: String, Configuration: StoreConfiguration)

//  This is a super quick placeholder for store configuration.  We will want to blow this out later.
// TODO: Create full configuration data model
case class StoreConfiguration(id: Int, storeId: Int, PaymentGateway: PaymentGateway)

case class StockLocation(id: Int, name: String)

// TODO: money/currency abstraction. Use joda-money, most likely
case class Money(currency: String, amount: Int)

case class Coupon(id: Int, cartId: Int, code: String, adjustment: List[Adjustment])

case class Promotion(id: Int, cartId: Int, adjustments: List[Adjustment])

sealed trait Destination
case class EmailDestination(email: String) extends Destination
case class ResidenceDestination(address: Address) extends Destination
case class StockLocationDestination(stockLocation: StockLocation) extends Destination

case class Fulfillment(id: Int, destination: Destination)

case class Archetype(id: Int, name: String) extends Validation[Archetype] {
  override def validator = createValidator[Archetype] { a =>
    a.name is notEmpty
  }
}

case class Catalog(id: Int, name: String,
                   archetypes: Seq[Archetype], products: Seq[Product]) extends Validation[Catalog] {
  override def validator = createValidator[Catalog] { c =>
    c.name is notEmpty
  }
}

case class Product(id: Int, name: String, sku: String, archetypes: Seq[Archetype])

case class Asset(id: Int, url: String, rank: Int)

case class Collection(id: Int, name: String, isActive: Boolean) extends Validation[Collection] {
  override def validator = createValidator[Collection] { c =>
    c.name is notEmpty
  }
}

object Main extends Formats {
  def main(args: Array[String]): Unit = {
    val service = new Service()
    service.bind()
  }
}

// JSON formatters
trait Formats extends DefaultJsonProtocol {
  implicit val addLineItemsRequestFormat = jsonFormat2(UpdateLineItemsPayload.apply)
  implicit val addPaymentMethodRequestFormat = jsonFormat4(PaymentMethodPayload.apply)
  implicit val creditCardPayloadFormat = jsonFormat5(CreditCardPayload.apply)
  implicit val addTokenizedPaymentMethodRequestFormat = jsonFormat2(TokenizedPaymentMethodPayload.apply)
  implicit val createAddressPayloadFormat = jsonFormat6(CreateAddressPayload.apply)
  implicit val createCustomerPayloadFormat =jsonFormat4(CreateCustomerPayload.apply)

  val phoenixFormats = DefaultFormats + new CustomSerializer[PaymentStatus](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: PaymentStatus ⇒ JString(x.toString) }
    )) + new CustomSerializer[GiftCardPaymentStatus](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: GiftCardPaymentStatus ⇒ JString(x.toString) }
    )) + new CustomSerializer[Order.Status](format => (
    { case _ ⇒ sys.error("Reading not implemented") },
    { case x: Order.Status ⇒ JString(x.toString) }
    ))
}


class Service(
  systemOverride: Option[ActorSystem] = None,
  dbOverride:     Option[slick.driver.PostgresDriver.backend.DatabaseDef] = None
) extends Formats {
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


  implicit val system = systemOverride.getOrElse {
    ActorSystem.create("Cart", config)
  }

  implicit def executionContext = system.dispatcher

  implicit val materializer = ActorFlowMaterializer()

  // required for (de)-serialization
  implicit val formats = phoenixFormats

  val logger = Logging(system, getClass)

  implicit val db = dbOverride.getOrElse {
    Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver")
  }

  def customerAuth: AsyncAuthenticator[Customer] = Authenticator.customer
  def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.storeAdmin

  val notFoundResponse = HttpResponse(NotFound)

  def renderGoodOrBad[G <: AnyRef, B <: AnyRef](goodOrBad: G Or B)
                                               (implicit ec: ExecutionContext,
                                                db: Database): HttpResponse = {
    goodOrBad match {
      case Bad(errors)    =>
        HttpResponse(BadRequest, entity = render(("errors" -> errors)))
      case Good(resource) =>
        HttpResponse(OK, entity = render(resource))
    }
  }

  def whenFound[A, G <: AnyRef, B <: AnyRef](finder: Future[Option[A]])(f: A => Future[G Or B])
                                            (implicit ec: ExecutionContext,
                                             db: Database): Future[HttpResponse] = {
    finder.flatMap { option =>
      option.map(f(_).map(renderGoodOrBad)).
        getOrElse(Future.successful(notFoundResponse))
    }
  }

  val routes = {
    def findCustomer(id: Option[Int]): Future[Option[Customer]] = {
      id.map { id =>
        Future.successful(Some(Customer(id = id, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")))
      }.getOrElse(Future.successful(None))
    }

    def renderOrNotFound[A <: AnyRef](resource: Future[Option[A]],
                                      onFound: (A => HttpResponse) = (r: A) => HttpResponse(OK, entity = render(r))) = {
      resource.map {
        case Some(r) => onFound(r)
        case None => notFoundResponse
      }
    }

    /*
      Admin Authenticated Routes
     */
    logRequestResult("admin-routes") {
      pathPrefix("v1" / "carts") {
        authenticateBasicAsync(realm = "cart and checkout", storeAdminAuth) { user =>
          (get & path(IntNumber)) { cartId =>
            complete {
              renderOrNotFound(FullCart.findById(cartId))
            }
          } ~
            (post & path(IntNumber / "checkout")) { cartId =>
              complete {
                whenFound(Carts.findById(cartId)) { cart => new Checkout(cart).checkout }
              }
            } ~
            (post & path(IntNumber / "line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { (cartId, reqItems) =>
              complete {
                whenFound(Carts.findById(cartId)) { cart => LineItemUpdater.updateQuantities(cart, reqItems) }
              }
            } ~
            (delete & path(IntNumber / "line-items" / IntNumber)) { (cartId, lineItemId) =>
              complete {
                // TODO(yax): can the account delete this lineItem?
                whenFound(Carts.findById(cartId)) { cart => LineItemUpdater.deleteById(lineItemId, cart.id) }
              }
            } ~
            (get & path(IntNumber / "payment-methods")) { cartId =>
              complete {
                renderOrNotFound(Carts.findById(cartId))
              }
            } ~
            (post & path(IntNumber / "payment-methods" / "credit-card") & entity(as[CreditCardPayload])) { (cartId, reqPayment) =>
              complete {
                Carts.findById(cartId).flatMap {
                  case None => Future.successful(notFoundResponse)
                  case Some(cart) =>
                    findCustomer(cart.accountId).flatMap {
                      case None     =>
                        Future.successful(HttpResponse(OK, entity = render(s"Guest checkout!!")))

                      case Some(customer) =>
                        CreditCardPaymentCreator.run(cart, customer, reqPayment).map { fullCart =>
                          fullCart.fold({ c => HttpResponse(OK, entity = render(c)) },
                                        { e => HttpResponse(BadRequest, entity = render("errors" -> e)) })
                        }
                    }
                }
              }
            } ~
            (post & path(IntNumber / "tokenized-payment-methods") & entity(as[TokenizedPaymentMethodPayload])) { (cartId, reqPayment) =>
              complete {
                Carts.findById(cartId).flatMap {
                  case None => Future.successful(notFoundResponse)
                  case Some(cart) =>
                    findCustomer(cart.accountId).flatMap {
                      case None     =>
                        Future.successful(HttpResponse(OK, entity = render(s"Guest checkout!!")))

                      case Some(customer) =>
                        TokenizedPaymentCreator.run(cart, customer, reqPayment.token).map { fullCart =>
                          fullCart.fold({ c => HttpResponse(OK, entity = render(c)) },
                                        { e => HttpResponse(BadRequest, entity = render("errors" -> e)) })
                        }
                    }
                }
              }
            }
        }
      }
    } ~
      /*
        Customer Authenticated Routes
       */
      logRequestResult("customer-routes") {
        pathPrefix("v1" / "my") {
          authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer =>
            pathPrefix("addresses") {
              get {
                complete {
                  Addresses.findAllByCustomer(customer).map { addresses =>
                    HttpResponse(OK, entity = render(addresses))
                  }
                }
              } ~
                (post & entity(as[Seq[CreateAddressPayload]])) { payload =>
                  complete {
                    Addresses.createFromPayload(customer, payload).map(renderGoodOrBad)
                  }
                }
            } ~
              pathPrefix("cart") {
                get {
                  complete {
                    renderOrNotFound(FullCart.findByCustomer(customer))
                  }
                } ~
                  (post & path("checkout")) {
                    complete {
                      whenFound(Carts.findByCustomer(customer)) { cart => new Checkout(cart).checkout }
                    }
                  } ~
                  (post & path("line_items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
                    complete {
                      whenFound(Carts.findByCustomer(customer)) { cart => LineItemUpdater.updateQuantities(cart, reqItems) }
                    }
                  } ~
                  (delete & path("line_items" / IntNumber)) { lineItemId =>
                    complete {
                      whenFound(Carts.findByCustomer(customer)) { cart => LineItemUpdater.deleteById(lineItemId, cart.id) }
                    }
                  }
              }
          }
        }
      } ~
      /*
        Public Routes
       */
      logRequestResult("public-routes") {
        pathPrefix("v1") {
          pathPrefix("registrations") {
            (post & path("new") & entity(as[CreateCustomerPayload])) { regRequest =>
              complete {
                Customers.createFromPayload(regRequest).map(renderGoodOrBad)
              }
            }
          }
        }
      }
  }


  def bind(config: Config = ConfigFactory.parseString(conf)): Future[ServerBinding] = {
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
