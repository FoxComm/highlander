package routes

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import akka.stream.Materializer
import services.Authenticator
import utils.Http._
import utils.aliases._

object AuthRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒
            complete(renderFailure(f))
          }, { token ⇒
            Authenticator.setJwtHeader(token) {
              complete(render(token))
            }
          })
        }
      }
    }
  }

}
