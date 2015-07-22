import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, StatusCode}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.scalactic._
import payloads._
import responses.{FullOrder, PublicSku, AdminNotes}
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
    (implicit ec: ExecutionContext, db: Database): HttpResponse = {
    goodOrBad match {
      case Bad(errors)    => render("errors" → errors, BadRequest)
      case Good(resource) => render(resource)
    }
  }

  def whenFound[A, G <: AnyRef, B <: AnyRef](finder: Future[Option[A]])(f: A => Future[G Or B])
    (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {
    finder.flatMap { option =>
      option.map(f(_).map(renderGoodOrBad)).
        getOrElse(Future.successful(notFoundResponse))
    }
  }

  def renderOrNotFound[A <: AnyRef](resource: Future[Option[A]],
    onFound: (A => HttpResponse) = (r: A) => render(r)) = {
    resource.map {
      case Some(r) => onFound(r)
      case None => notFoundResponse
    }
  }

  def render[A <: AnyRef](resource: A, statusCode: StatusCode = OK) =
    HttpResponse(statusCode, entity = json(resource))

  val routes = {
    def findCustomer(id: Int): Future[Option[Customer]] = {
        Future.successful(Some(Customer(id = id, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")))
    }

    /*
      Admin Authenticated Routes
     */
    logRequestResult("admin-routes") {
      authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin =>
        pathPrefix("v1" / "gift-cards") {
          (get & path(IntNumber) & pathEnd) { giftCardId ⇒
            complete {
              renderOrNotFound(GiftCards.findById(giftCardId).run())
            }
          }
        } ~
        pathPrefix("v1" / "users" / IntNumber) { customerId ⇒
          (get & pathEnd) {
            complete {
              renderOrNotFound(Customers.findById(customerId))
            }
          } ~
          (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
            complete {
              CustomerManager.toggleDisabled(customerId, payload.disabled, admin).map(renderGoodOrBad)
            }
          } ~
          pathPrefix("addresses") {
            get {
              complete {
                Addresses.findAllByCustomerId(customerId).map { addresses =>
                  HttpResponse(OK, entity = json(addresses))
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
            pathPrefix("credit-cards") {
              (get & pathEnd) {
                complete { render(CreditCards.findAllByCustomerId(customerId)) }
              } ~
              (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultCreditCard])) { (cardId, payload) ⇒
                complete {
                  val result = CustomerManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
                  result.map(renderGoodOrBad)
                }
              }
            } ~
            pathPrefix("store-credits") {
              (get & pathEnd) {
                complete {
                  renderOrNotFound(StoreCredits.findAllByCustomerId(customerId).map(Some(_)))
                }
              } ~
              (get & path(IntNumber)) { storeCreditId ⇒
                complete {
                  renderOrNotFound(StoreCredits.findById(storeCreditId).run())
                }
              } ~
//              (post & entity(as[CreateStoreCredit])) { payload ⇒
//                complete {
//                  Future.successful(HttpResponse(OK))
//                }
//              } ~
              (post & path(IntNumber / "convert")) { storeCreditId ⇒
                complete {
                  whenFound(StoreCredits.findById(storeCreditId).run()) { sc ⇒
                    CustomerCreditConverter.toGiftCard(sc, customerId)
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("v1" / "orders" / IntNumber) { orderId ⇒
          (get & pathEnd) {
            complete {
              renderOrNotFound(FullOrder.findById(orderId))
            }
          } ~
          (patch & entity(as[UpdateOrderPayload]) ) { payload =>
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
          (post & path("checkout")) {
            complete {
              whenFound(Orders.findById(orderId).run()) { order => new Checkout(order).checkout }
            }
          } ~
          (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
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
          pathPrefix("payment-methods") {
            (get & pathEnd) {
              complete {
                renderOrNotFound(Orders.findById(orderId).run())
              }
            } ~
            (post & path("credit-card") & entity(as[CreditCardPayload])) { reqPayment =>
              complete {
                Orders.findById(orderId).run().flatMap {
                  case None => Future.successful(notFoundResponse)
                  case Some(order) =>
                    findCustomer(order.customerId).flatMap {
                      case None =>
                        Future.successful(HttpResponse(OK, entity = json(s"Guest checkout!!")))

                      case Some(customer) =>
                        CreditCardPaymentCreator.run(order, customer, reqPayment).map { fullOrder =>
                          fullOrder.fold({ c => HttpResponse(OK, entity = json(c)) }, { e => HttpResponse(BadRequest, entity = json("errors" -> e.flatMap(_.description))) })

                        }
                    }
                }
              }
            }
          } ~
          pathPrefix("notes") {
            (get & pathEnd) {
              complete {
                whenFound(Orders.findById(orderId).run()) { order ⇒ AdminNotes.forOrder(order) }
              }
            } ~
            (post & entity(as[payloads.CreateNote])) { payload ⇒
              complete {
                whenFound(Orders.findById(orderId).run()) { order ⇒
                  services.NoteManager.createOrderNote(order, admin, payload)
                }
              }
            } ~
            (patch & path(IntNumber) & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
              complete {
                whenFound(Orders.findById(orderId).run()) { order ⇒
                  services.NoteManager.updateNote(noteId, admin, payload)
                }
              }
            } ~
            (delete & path(IntNumber)) { noteId ⇒
              complete { notFoundResponse }
            }
//            (patch & entity(as[payloads.UpdateNote])) { payload ⇒
//              complete {
//                whenFound(Orders.findById(orderId).run()) { order ⇒
//                  NoteCreator.createOrderNote(order, admin, payload)
//                }
//              }
//            }
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
                    HttpResponse(OK, entity = json(addresses))
                  }
                }
              } ~
              (post & entity(as[Seq[CreateAddressPayload]])) { payload =>
                complete {
                  Addresses.createFromPayload(customer, payload).map(renderGoodOrBad)
                }
              }
            } ~
            pathPrefix("payment-methods") {
              pathPrefix("store-credits") {
                (get & pathEnd) {
                  complete {
                    renderOrNotFound(StoreCredits.findAllByCustomerId(customer.id).map(Some(_)))
                  }
                } ~
                (get & path(IntNumber)) { storeCreditId ⇒
                  complete {
                    renderOrNotFound(StoreCredits.findByIdAndCustomerId(storeCreditId, customer.id))
                  }
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
            (post & path("new") & entity(as[payloads.CreateCustomer])) { regRequest =>
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
