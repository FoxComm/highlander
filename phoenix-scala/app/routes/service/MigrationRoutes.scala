package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import payloads.CustomerPayloads._
import services.account.AccountCreateContext
import services.migration.CustomerImportService
import utils.http.CustomDirectives._
import utils.aliases._
import utils.http.Http._

object MigrationRoutes {
  def routes(customerCreateContext: AccountCreateContext)(implicit ec: EC, db: DB, es: ES): Route = {

    activityContext() { implicit ac ⇒
      pathPrefix("migration") {
        pathPrefix("customers") {
          (post & path("new") & pathEnd & entity(as[CreateCustomerPayload])) { payload ⇒
            mutateOrFailures {
              CustomerImportService.create(payload = payload, context = customerCreateContext)
            }
          }
        }
      }
    }
  }
}
