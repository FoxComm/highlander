package phoenix.routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tminglei.slickpg.LTree
import phoenix.payloads.CustomerPayloads._
import phoenix.services.account.AccountCreateContext
import phoenix.services.migration.CustomerImportService
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object MigrationRoutes {
  def routes(customerCreateContext: AccountCreateContext,
             defaultScope: LTree)(implicit ec: EC, db: DB, apis: Apis): Route =
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
