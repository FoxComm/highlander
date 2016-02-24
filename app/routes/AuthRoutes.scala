package routes

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import akka.stream.Materializer
import slick.driver.PostgresDriver.api._

import models.auth.Session.setTokenSession
import services.Authenticator

import utils.Http._

object AuthRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, mat: Materializer) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒
            complete(renderFailure(f))
          }, { token ⇒
            setTokenSession(token) {
              complete(render(token))
            }
          })
        }
      }
    }
  }

}
