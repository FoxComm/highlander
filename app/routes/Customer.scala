package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._

import payloads._
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object Customer {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, customerAuth: AsyncAuthenticator[Customer]) = {
    import Json4sSupport._
    import utils.Http._

    authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer =>
      pathPrefix("my") {
        (get & path("cart")) {
          complete {
            whenOrderFoundAndEditable(customer) { activeOrder ⇒
              FullOrder.fromOrder(activeOrder).run().map(Xor.right)
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
              AddressManager.create(payload, customer.id).map(renderGoodOrFailures)
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
              whenOrderFoundAndEditable(customer) {
                order ⇒ new Checkout(order).checkout
              }
            }
          } ~
          (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
            complete {
              whenOrderFoundAndEditable(customer) { order ⇒
                LineItemUpdater.updateQuantities(order, reqItems).flatMap {
                  case Xor.Right(_) ⇒ FullOrder.fromOrder(order).run().map(Xor.right)
                  case Xor.Left(e)  ⇒ Future.successful(Xor.left(e))
                }
              }
            }
          } ~
          (get & path(PathEnd)) {
            complete {
              whenFound(Orders._findActiveOrderByCustomer(customer).one.run()) { order ⇒
                FullOrder.fromOrder(order).run().map(Xor.right)
              }
            }
          }
        }
      }
    }
  }
}

