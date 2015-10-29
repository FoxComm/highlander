package routes.admin

import java.time.Instant

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models._
import payloads._
import services.{LockAwareRmaUpdater, RmaService}

import responses.FullOrder.DisplayLineItem
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse
import responses.RmaResponse._

import slick.driver.PostgresDriver.api._

import utils.Apis
import utils.Http._
import utils.CustomDirectives._

object RmaRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      val adminResponse = Some(StoreAdminResponse.build(admin))
      val genericRmaMock = buildMockRma(id = 1, refNum = "ABC-123", orderId = 1, admin = adminResponse)

      pathPrefix("rmas") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            RmaService.findAll
          }
        } ~
        (get & path("customer" / IntNumber)) { customerId ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              RmaService.findByCustomerId(customerId)
            }
          }
        } ~
        (get & path("order" / Order.orderRefNumRegex)) { refNum ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              RmaService.findByOrderRef(refNum)
            }
          }
        } ~
        (post & entity(as[RmaCreatePayload]) & pathEnd) { payload ⇒
          good {
            genericRmaMock.copy(orderId = payload.orderId)
          }
        }
      } ~
      pathPrefix("rmas" / Rma.rmaRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            RmaService.getByRefNum(refNum)
          }
        } ~
        (patch & entity(as[RmaUpdatePayload]) & pathEnd) { payload ⇒
          good {
            genericRmaMock.copy(orderId = payload.orderId)
          }
        } ~
        (patch & path("status") & entity(as[RmaUpdateStatusPayload]) & pathEnd) { payload ⇒
          good {
            genericRmaMock
          }
        } ~
        (post & path("lock") & pathEnd) {
          goodOrFailures {
            LockAwareRmaUpdater.lock(refNum, admin)
          }
        } ~
        (post & path("unlock") & pathEnd) {
          goodOrFailures {
            LockAwareRmaUpdater.unlock(refNum)
          }
        }
      }
    }
  }
}