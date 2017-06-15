package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.GenericTreePayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.tree.TreeManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object GenericTreeRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
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
