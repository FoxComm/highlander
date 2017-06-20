package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.StoreCreditPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.StoreCreditService
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object StoreCreditRoutes {
  private[admin] def storeCreditRoutes(implicit ec: EC,
                                       db: DB,
                                       ac: AC,
                                       apis: Apis,
                                       auth: AuthData[User]): Route =
    pathPrefix("store-credits") {
      (patch & pathEnd & entity(as[StoreCreditBulkUpdateStateByCsr])) { payload ⇒
        mutateOrFailures {
          StoreCreditService.bulkUpdateStateByCsr(payload, auth.model)
        }
      }
    } ~
      pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
        (get & pathEnd) {
          getOrFailures {
            StoreCreditService.getById(storeCreditId)
          }
        } ~
        (patch & pathEnd & entity(as[StoreCreditUpdateStateByCsr])) { payload ⇒
          mutateOrFailures {
            StoreCreditService.updateStateByCsr(storeCreditId, payload, auth.model)
          }
        }
      }
}
