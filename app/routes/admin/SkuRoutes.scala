package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import services.SkuManager
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

import payloads.{CreateSkuForm, UpdateSkuForm, CreateSkuShadow, UpdateSkuShadow}


object SkuRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, admin: StoreAdmin, apis: Apis) = {

      activityContext(admin) { implicit ac ⇒

        pathPrefix("skus") {
          pathPrefix("forms" / Segment) { code ⇒
            (get & pathEnd) {
              goodOrFailures {
                SkuManager.getForm(code)
              }
            } ~ 
            (patch & pathEnd & entity(as[UpdateSkuForm])) { payload ⇒
              goodOrFailures {
                SkuManager.updateForm(code, payload)
              }
            } 
          } ~
          pathPrefix("forms") { 
            (post & pathEnd & entity(as[CreateSkuForm])) { payload ⇒
              goodOrFailures {
                SkuManager.createForm(payload)
              }
            } 
          } ~
          pathPrefix("shadows" / Segment / Segment) { (context, code)  ⇒
            (get & pathEnd) {
              goodOrFailures {
                SkuManager.getShadow(code, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateSkuShadow])) { payload ⇒
              goodOrFailures {
                SkuManager.updateShadow(code, payload, context)
              }
            } 
          } ~
          pathPrefix("shadows" / Segment) { (context)  ⇒
            (post & pathEnd & entity(as[CreateSkuShadow])) { payload ⇒
              goodOrFailures {
                SkuManager.createShadow(payload, context)
              }
            } 
          } ~
          pathPrefix("illuminated" / Segment / Segment) { (context, code) ⇒
            (get & pathEnd) {
              goodOrFailures {
                SkuManager.getIlluminatedSku(code, context)
              }
            }
          } 
        }
      }
  }
}
