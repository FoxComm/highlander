package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.GenericTreePayloads._
import services.tree.TreeManager
import utils.aliases.{DB, EC}
import utils.http.CustomDirectives._
import utils.http.Http._

object GenericTreeRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("tree" / Segment / Segment) { (context, name) ⇒
        (get & pathEnd) {
          goodOrFailures {
            TreeManager.getFullTree(name, context)
          }
        } ~
        (post & pathEnd & entity(as[NodePayload])) { payload ⇒
          goodOrFailures {
            TreeManager.updateTree(name, context, payload)
          }
        } ~
        pathPrefix(Segment) { (path) ⇒
          (post & pathEnd & entity(as[NodePayload])) { payload ⇒
            goodOrFailures {
              TreeManager.updateTree(name, context, payload, Some(path))
            }
          } ~
          (patch & pathEnd & entity(as[NodeValuesPayload])) { payload ⇒
            goodOrFailures {
              TreeManager.editNode(name, context, path, payload)
            }
          }
        } ~
        (patch & pathEnd & entity(as[MoveNodePayload])) { payload ⇒
          goodOrFailures {
            TreeManager.moveNode(name, context, payload)
          }
        } ~
        (patch & pathEnd & entity(as[MoveNodePayload])) { payload ⇒
          goodOrFailures {
            TreeManager.moveNode(name, context, payload)
          }
        }
      }
    }
  }
}
