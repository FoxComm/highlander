package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.ImagePayloads.AlbumPayload
import payloads.SkuPayloads._
import services.Authenticator.AuthData
import services.image.ImageManager
import services.inventory.SkuManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object SkuRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

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
}
