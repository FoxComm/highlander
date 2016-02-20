package routes

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MalformedRequestContentRejection, UnsupportedRequestContentTypeRejection,
RequestEntityExpectedRejection, Directive1, StandardRoute, ValidationRejection}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import org.json4s.Extraction
import akka.stream.Materializer
import slick.driver.PostgresDriver.api._

import models.auth.Session.{setAdminSession, setCustomerSession, requireAdminAuth, requireCustomerAuth}
import models.auth.{AdminToken, CustomerToken}
import services.{Result, Authenticator}
import utils.CustomDirectives._

import utils.Http._

object AuthRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, mat: Materializer) = {
    
    (post & path("login") & entity(as[payloads.LoginPayload])) { login ⇒
      onSuccess(Authenticator.adminLogin(login)) { result ⇒
        result.fold({ f ⇒
            complete(renderFailure(f))
          }, { token ⇒
          setAdminSession(token) {
            complete(render(token))
          }
        })
      }
    } ~
    requireAdminAuth { session =>
      path("jwt") {
        goodOrFailures {
          Result.good(session.name)
        }
      }
    }
  }

}
