package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tminglei.slickpg.LTree
import payloads.CustomerPayloads._
import services.account.AccountCreateContext
import services.migration.CustomerImportService
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object MigrationRoutes {
  def routes(customerCreateContext: AccountCreateContext,
             defaultScope: LTree)(implicit ec: EC, db: DB, apis: Apis): Route = {

    activityContext(defaultScope) { implicit ac ⇒
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
