package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import models._
import models.Rma.rmaRefNumRegex
import services._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Apis

object RmaRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("rmas" / rmaRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            RmaService.getByRefNum(refNum)
          }
        }
      }
    }
  }
}
