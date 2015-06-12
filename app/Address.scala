import java.util.Date
import java.util.concurrent.TimeoutException
import akka.http.scaladsl.Http.ServerBinding
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

import utils.{RichTable, Validation}
import models._
import payloads._
import responses.FullCart
import services.{LineItemUpdater, PaymentGateway, Checkout, TokenizedPaymentCreator}

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

// JSON formatters
trait Formats extends DefaultJsonProtocol {
  def adtSerializer[T : Manifest] = () => {
    new CustomSerializer[T](format => ( {
      case _ ⇒ sys.error("Reading not implemented")
    }, {
      case x ⇒ JString(x.toString)
    }))
  }

  implicit val addLineItemsRequestFormat = jsonFormat2(UpdateLineItemsPayload.apply)
  implicit val addPaymentMethodRequestFormat = jsonFormat4(PaymentMethodPayload.apply)
  implicit val addTokenizedPaymentMethodRequestFormat = jsonFormat2(TokenizedPaymentMethodPayload.apply)
  implicit val createAddressPayloadFormat = jsonFormat6(CreateAddressPayload.apply)

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

  implicit val system = systemOverride.getOrElse { ActorSystem.create("Cart", config) }
  implicit def executionContext = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  // required for (de)-serialization
  implicit val formats = phoenixFormats

  val logger = Logging(system, getClass)

  implicit val db = dbOverride.getOrElse { Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver") }

  val user = User(id = 1, email = "yax@foxcommerce.com", password = "donkey", firstName = "Yax", lastName = "Donkey")

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

    logRequestResult("carts") {
      pathPrefix("v1" / "carts" ) {
        (get & path(IntNumber)) { id =>
          complete {
            renderOrNotFound(FullCart.findById(id))
          }
        } ~
        (post & path(IntNumber / "checkout")) { id =>
          complete {
            renderOrNotFound(Carts.findById(id), (c: Cart) => {
              new Checkout(c).checkout match {
                case Good(order) => HttpResponse(OK, entity = render(order))
                case Bad(errors) => HttpResponse(BadRequest, entity = render(errors))
              }
            })
          }
        } ~
        (post & path(IntNumber / "line_items") & entity(as[Seq[UpdateLineItemsPayload]])) { (cartId, reqItems) =>
          complete {
            Carts.findById(cartId).map {
              case None => Future(notFoundResponse)
              case Some(c) =>
                LineItemUpdater.updateQuantities(c, reqItems).map {
                  case Bad(errors)      =>
                    HttpResponse(BadRequest, entity = render(errors))
                  case Good(lineItems)  =>
                    HttpResponse(OK, entity = render(FullCart.build(c, lineItems)))
                }
            }
          }
        } ~
        (delete & path(IntNumber / "line_items" / IntNumber)) { (cartId, lineItemId) =>
          complete {
            Carts.findById(cartId).map {
              case None => Future(notFoundResponse)
              case Some(cart) =>
              // TODO(yax): can the account delete this lineItem?
                LineItemUpdater.deleteById(lineItemId, cart.id).map {
                  case Bad(errors) =>
                    HttpResponse(BadRequest, entity = render(errors))
                  case Good(lineItems) =>
                    HttpResponse(OK, entity = render(FullCart.build(cart, lineItems)))
                }
            }
          }
        } ~
          (get & path(IntNumber / "payment-methods")) { cartId =>
            complete {
              renderOrNotFound(Carts.findById(cartId))
            }
          } ~
        (post & path(IntNumber / "payment-methods") & entity(as[PaymentMethodPayload])) { (cartId, reqPayment) =>
          complete {
            Carts.findById(cartId).map {
              //can't add payment methods if the cart doesn't exist
              case None => notFoundResponse
              case Some(c) =>
                HttpResponse(OK, entity = render("HI"))
            }
          }
        } ~
        (post & path(IntNumber / "tokenized-payment-methods") & entity(as[TokenizedPaymentMethodPayload])) { (cartId, reqPayment) =>
          complete {
            Carts.findById(cartId).flatMap {
              case None => Future.successful(notFoundResponse)
              case Some(cart) =>
                // Check to see if there is a user associated with the cart.
                findAccount(cart.accountId) match {
                  case None     =>
                    Future.successful(HttpResponse(OK, entity = render("Guest checkout!!")))

                  case Some(shopper) =>
                    // TODO: good -> render cart, bad -> error
                    TokenizedPaymentCreator.run(cart, shopper, reqPayment.paymentGatewayToken).map { fullCart =>
                      fullCart.fold({ c => HttpResponse(OK, entity = render(c)) },
                                    { e => HttpResponse(BadRequest, entity = render(e)) })
                    }
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
            Addresses.findAllByAccount(user).map { addresses =>
              HttpResponse(OK, entity = render(addresses))
            }
          }
        } ~
        (post & entity(as[Seq[CreateAddressPayload]])) { payload =>
          complete {
            Addresses.createFromPayload(user, payload).map {
              case Good(addresses)  => HttpResponse(OK, entity = render(addresses))
              case Bad(errorMap)      => HttpResponse(BadRequest, entity = render(errorMap))
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
