package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.{Orders, StoreCredits}
import payloads.{CreateAddressPayload, UpdateLineItemsPayload}
import services.{AddressManager, LineItemUpdater, Result, StoreCreditService}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}

import scala.concurrent.ExecutionContext

object Customer {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, customerAuth: AsyncAuthenticator[models.Customer]) = {

    authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer ⇒
      pathPrefix("my") {
        (get & path("cart")) {
          goodOrFailures {
            val finder = Orders.findActiveOrderByCustomer(customer)
            finder.selectOne { _ ⇒ DbResult.fromDbio(fullOrder(finder)) }
          }
        } ~
        pathPrefix("addresses") {
          (get & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              AddressManager.findAllVisibleByCustomer(customer.id)
            }
          } ~
          (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            complete {
              AddressManager.create(payload, customer.id).map(renderGoodOrFailures)
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
            nothingOrFailures {
              Result.unit // FIXME Stubbed until checkout is updated
            }
          } ~
          (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
            goodOrFailures {
              LineItemUpdater.updateQuantitiesOnCustomersOrder(customer, reqItems)
            }
          } ~
          (get & pathEnd) {
            goodOrFailures {
              val finder = Orders.findActiveOrderByCustomer(customer)
              finder.selectOne { order ⇒ DbResult.fromDbio(fullOrder(finder)) }
            }
          }
        }
      }
    }
  }
}

