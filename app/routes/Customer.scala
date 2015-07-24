package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.scalactic._
import payloads._
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object Customer {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, customerAuth: AsyncAuthenticator[Customer]) = {
    import Json4sSupport._
    import utils.Http._

    authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer =>
      pathPrefix("my") {
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
              Addresses.findAllByCustomer(customer).map(render(_))
            }
          } ~
            (post & entity(as[CreateAddressPayload])) { payload =>
              complete {
                AddressManager.createOne(customer.id, payload).map(renderGoodOrBad)
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
  }
}

