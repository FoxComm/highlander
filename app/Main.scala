import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson
import org.json4s.jackson.Serialization.{write ⇒ render}
import org.scalactic._
import payloads._
import responses.{FullOrder, PublicSku}
import services._
import slick.driver.PostgresDriver.api._
import utils.RunOnDbIO

object Main {
  def main(args: Array[String]): Unit = {
    val service = new Service()
    service.bind()
  }
}

class Service(
  systemOverride: Option[ActorSystem] = None,
  dbOverride:     Option[slick.driver.PostgresDriver.backend.DatabaseDef] = None
) {

  import Json4sSupport._
  import utils.JsonFormatters._

  implicit val serialization = jackson.Serialization
  implicit val formats = phoenixFormats

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
    ActorSystem.create("Orders", config)
  }

  implicit def executionContext = system.dispatcher

  implicit val materializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  implicit val db = dbOverride.getOrElse(Database.forConfig("db.development"))

  def customerAuth: AsyncAuthenticator[Customer] = Authenticator.customer
  def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.storeAdmin

  val notFoundResponse = HttpResponse(StatusCodes.NotFound)

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
    def findCustomer(id: Int): Future[Option[Customer]] = {
        Future.successful(Some(Customer(id = id, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")))
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
      authenticateBasicAsync(realm = "admin", storeAdminAuth) { user =>
        pathPrefix("v1" / "users" / IntNumber) { customerId =>
          pathPrefix("addresses") {
            get {
              complete {
                Addresses.findAllByCustomerId(customerId).map { addresses =>
                  HttpResponse(OK, entity = render(addresses))
                }
              }
            } ~
            (post & entity(as[Seq[CreateAddressPayload]])) { payload =>
              complete {
                whenFound(findCustomer(customerId)) { customer =>
                  Addresses.createFromPayload(customer, payload)
                }
              }
            }
          } ~
          pathPrefix("payment-methods") {
            (get & path("gift-cards")) {
              complete {
                renderOrNotFound(GiftCards.findAllByCustomerId(customerId).map(Some(_)))
              }
            }
          }
        } ~
        pathPrefix("v1" / "orders") {
          (get & path(IntNumber)) { orderId =>
            complete {
              renderOrNotFound(FullOrder.findById(orderId))
            }
          } ~
            (patch & path(IntNumber) & entity(as[UpdateOrderPayload]) ) { (orderId, payload) =>
              complete {
                whenFound(Orders.findById(orderId).run()) { order =>
                  OrderUpdater.updateStatus(order, payload).flatMap {
                    case Good(o) ⇒
                      FullOrder.fromOrder(o).map {
                        case Some(r)  ⇒ Good(r)
                        case None     ⇒ Bad(List("order not found"))
                      }

                    case Bad(e) ⇒
                      Future.successful(Bad(e))
                  }
                }
              }
            } ~
            (post & path(IntNumber / "checkout")) { orderId =>
              complete {
                whenFound(Orders.findById(orderId).run()) { order => new Checkout(order).checkout }
              }
            } ~
            (post & path(IntNumber / "line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { (orderId, reqItems) =>
              complete {
                whenFound(Orders.findById(orderId).run()) { order =>
                  LineItemUpdater.updateQuantities(order, reqItems).flatMap {
                    case Good(_) ⇒
                      FullOrder.fromOrder(order).map {
                        case Some(r) ⇒ Good(r)
                        case None ⇒ Bad(List("order not found"))
                      }

                    case Bad(e) ⇒
                      Future.successful(Bad(e))
                  }
                }
              }
            } ~
            (get & path(IntNumber / "payment-methods")) { orderId =>
              complete {
                renderOrNotFound(Orders.findById(orderId).run())
              }
            } ~
            (post & path(IntNumber / "payment-methods" / "credit-card") & entity(as[CreditCardPayload])) { (orderId, reqPayment) =>
              complete {
                Orders.findById(orderId).run().flatMap {
                  case None => Future.successful(notFoundResponse)
                  case Some(order) =>
                    findCustomer(order.customerId).flatMap {
                      case None     =>
                        Future.successful(HttpResponse(OK, entity = render(s"Guest checkout!!")))

                      case Some(customer) =>
                        CreditCardPaymentCreator.run(order, customer, reqPayment).map { fullOrder =>
                          fullOrder.fold({ c => HttpResponse(OK, entity = render(c)) },
                                        { e => HttpResponse(BadRequest, entity = render("errors" -> e.flatMap(_.description))) })
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
            (get & path("cart")) {
              complete {
                whenFound(Orders.findActiveOrderByCustomer(customer)) { activeOrder =>
                  FullOrder.fromOrder(activeOrder).map(Good(_))
                }
              }
            } ~
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
              pathPrefix("order") {
                (post & path("checkout")) {
                  complete {
                    whenFound(Orders.findActiveOrderByCustomer(customer)) { order => new Checkout(order).checkout }
                  }
                } ~
                (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
                  complete {
                    whenFound(Orders.findActiveOrderByCustomer(customer)) { order =>
                      LineItemUpdater.updateQuantities(order, reqItems).flatMap {
                        case Good(_) ⇒
                          FullOrder.fromOrder(order).map {
                            case Some(r) ⇒ Good(r)
                            case None ⇒ Bad(List("order not found"))
                          }

                        case Bad(e) ⇒
                          Future.successful(Bad(e))
                      }
                    }
                  }
                } ~
                (get & path("shipping-methods")) {
                  complete {
                    whenFound(Orders.findActiveOrderByCustomer(customer)) { order =>
                      ShippingMethodsBuilder.fullShippingMethodsForOrder(order).map { x =>
                        // we'll need to handle Bad
                        Good(x)
                      }
                    }
                  }
                } ~
                (post & path("shipping-methods" / IntNumber)) { shipMethodId =>
                  complete {
                    whenFound(Orders.findActiveOrderByCustomer(customer)) { order =>
                      ShippingMethodsBuilder.addShippingMethodToOrder(shipMethodId, order).flatMap {
                        case Good(o) ⇒
                          FullOrder.fromOrder(o).map {
                            case Some(r) ⇒ Good(r)
                            case None ⇒ Bad(List("order not found"))
                          }

                        case Bad(e) ⇒
                          Future.successful(Bad(e))
                      }
                    }
                  }
                } ~
                  (get & path(PathEnd)) {
                    complete {
                      renderOrNotFound(FullOrder.findByCustomer(customer))
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
          } ~
          pathPrefix("skus") {
            (get & path(IntNumber)) { skuId =>
              complete {
                renderOrNotFound(PublicSku.findById(skuId))
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
