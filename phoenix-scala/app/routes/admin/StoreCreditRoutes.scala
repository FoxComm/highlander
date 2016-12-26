package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.StoreCreditPayloads._
import services.StoreCreditService
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object StoreCreditRoutes {
  private[admin] def storeCreditRoutes(implicit ec: EC,
                                       db: DB,
                                       ac: AC,
                                       auth: AuthData[User],
                                       tr: TracingRequest,
                                       trace: TracingExtensionImpl) = {
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
