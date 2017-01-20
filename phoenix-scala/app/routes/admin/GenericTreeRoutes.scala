package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.GenericTreePayloads._
import services.tree.TreeManager
import services.Authenticator.AuthData
import utils.aliases.{DB, EC}
import utils.http.CustomDirectives._
import utils.http.Http._

object GenericTreeRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("tree" / Segment / Segment) { (context, name) ⇒
        (get & pathEnd) {
          getOrFailures {
            TreeManager.getFullTree(name, context)
          }
        } ~
        (post & pathEnd & entity(as[NodePayload])) { payload ⇒
          mutateOrFailures {
            TreeManager.updateTree(name, context, payload)
          }
        } ~
        pathPrefix(Segment) { (path) ⇒
          (post & pathEnd & entity(as[NodePayload])) { payload ⇒
            mutateOrFailures {
              TreeManager.updateTree(name, context, payload, Some(path))
            }
          } ~
          (patch & pathEnd & entity(as[NodeValuesPayload])) { payload ⇒
            mutateOrFailures {
              TreeManager.editNode(name, context, path, payload)
            }
          }
        } ~
        (patch & pathEnd & entity(as[MoveNodePayload])) { payload ⇒
          mutateOrFailures {
            TreeManager.moveNode(name, context, payload)
          }
        } ~
        (patch & pathEnd & entity(as[MoveNodePayload])) { payload ⇒
          mutateOrFailures {
            TreeManager.moveNode(name, context, payload)
          }
        }
      }
    }
  }
}
