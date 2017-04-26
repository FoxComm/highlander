package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.StoreCreditPayloads._
import services.Authenticator.AuthData
import services.StoreCreditService
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object StoreCreditRoutes {
  private[admin] def storeCreditRoutes(implicit ec: EC,
                                       db: DB,
                                       ac: AC,
                                       auth: AuthData[User]): Route = {
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
}
