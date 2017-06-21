package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.utils.http.JsonSupport._
import phoenix.models.account.User
import phoenix.payloads.AmazonOrderPayloads._
import phoenix.services.AmazonOrderManager._
import phoenix.responses.cord.AmazonOrderResponse._
import phoenix.responses.cord.AmazonOrderResponse
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._

object AmazonOrderRoutes {
  def routes(implicit ec: EC, db: DB, auth: AU): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("amazon-orders") {
        (post & pathEnd & entity(as[CreateAmazonOrderPayload])) { payload ⇒
          mutateOrFailures {
            createAmazonOrder(payload)
          }
        } ~
        pathPrefix(Segment) { amazonOrderId ⇒
          (patch & pathEnd & entity(as[UpdateAmazonOrderPayload])) { payload ⇒
            mutateOrFailures {
              updateAmazonOrder(amazonOrderId, payload)
            }
          }
        }
      }
    }
}
