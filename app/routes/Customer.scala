package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreCredits
import payloads.{CreateAddressPayload, UpdateLineItemsPayload}
import services.orders.OrderQueries
import services.{AddressManager, Checkout, LineItemUpdater, StoreCreditService}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object Customer {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, customerAuth: AsyncAuthenticator[models.Customer], apis: Apis) = {

      pathPrefix("my") {
        authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer ⇒

          determineProductContext(db, ec) { productContext ⇒ 

            (get & path("cart")) {
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderQueries.findOrCreateCartByCustomer(customer, productContext)
                }
              }
            } ~
            pathPrefix("addresses") {
              (get & sortAndPage) { implicit sortAndPage ⇒
                goodOrFailures {
                  AddressManager.findAllVisibleByCustomer(customer.id)
                }
              } ~
              (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                activityContext(customer) { implicit ac ⇒
                  goodOrFailures {
                    AddressManager.create(payload, customer.id)
                  }
                }
              }
            } ~
            pathPrefix("payment-methods") {
              pathPrefix("store-credits") {
                (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
                  goodOrFailures {
                    StoreCreditService.findAllByCustomer(customer.id)
                  }
                } ~
                (get & path(IntNumber)) { storeCreditId ⇒
                  complete {
                    renderOrNotFound(StoreCredits.findByIdAndCustomerId(storeCreditId, customer.id).run())
                  }
                }
              }
            } ~
            pathPrefix("order") {
              (post & path("checkout")) {
                activityContext(customer) { implicit ac ⇒
                  goodOrFailures {
                    Checkout.fromCustomerCart(customer)
                  }
                }
              } ~
              (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
                activityContext(customer) { implicit ac ⇒
                  goodOrFailures {
                    LineItemUpdater.updateQuantitiesOnCustomersOrder(customer, reqItems)
                  }
                }
              } ~
              (get & pathEnd) {
                activityContext(customer) { implicit ac ⇒
                  goodOrFailures {
                    OrderQueries.findOrCreateCartByCustomer(customer, productContext)
                  }
                }
              }
            } ~
            complete {
              notFoundResponse
            }
          }
        }
      }
  }
}
