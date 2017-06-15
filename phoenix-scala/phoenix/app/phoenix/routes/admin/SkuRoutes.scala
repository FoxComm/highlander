package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.ImagePayloads.AlbumPayload
import phoenix.payloads.SkuPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.image.ImageManager
import phoenix.services.inventory.SkuManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object SkuRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("skus") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (post & pathEnd & entity(as[SkuPayload])) { payload ⇒
              mutateOrFailures {
                SkuManager.createSku(auth.model, payload)
              }
            } ~
            pathPrefix(Segment) { code ⇒
              (get & pathEnd) {
                getOrFailures {
                  SkuManager.getSku(code)
                }
              } ~
              (patch & pathEnd & entity(as[SkuPayload])) { payload ⇒
                mutateOrFailures {
                  SkuManager.updateSku(auth.model, code, payload)
                }
              } ~
              (delete & pathEnd) {
                mutateOrFailures {
                  SkuManager.archiveByCode(code)
                }
              } ~
              pathPrefix("albums") {
                (get & pathEnd) {
                  getOrFailures {
                    ImageManager.getAlbumsForSku(code)
                  }
                } ~
                (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
                  mutateOrFailures {
                    ImageManager.createAlbumForSku(auth.model, code, payload)
                  }
                }
              }
            }
          }
        }
      }
    }
}
