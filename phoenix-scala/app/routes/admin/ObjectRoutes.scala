package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.ObjectSchemaPayloads._
import services.Authenticator.AuthData
import utils.aliases._
import services.objects.ObjectSchemasManager
import utils.http.CustomDirectives._
import utils.http.Http._

object ObjectRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("objects" / "schemas") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (post & pathEnd & entity(as[CreateSchemaPayload])) { payload ⇒
              mutateOrFailures {
                ObjectSchemasManager.createSchema(payload)
              }
            } ~
            pathPrefix(Segment) { kind ⇒
              (get & pathEnd) {
                getOrFailures {
                  ObjectSchemasManager.getSchemasForKind(kind)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateSchemaPayload])) { payload ⇒
                mutateOrFailures {
                  ObjectSchemasManager.updateSchema(kind, payload)
                }
              }
            }
          }
        }
      }
    }
  }
}
