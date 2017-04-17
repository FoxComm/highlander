package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import org.json4s.native.JsonParser.StringVal
import payloads.AmazonOrderPayloads._
import services.AmazonOrderManager._
import responses.cord.AmazonOrderResponse._
import responses.cord.AmazonOrderResponse
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AmazonOrderRoutes {
  def routes(implicit ec: EC, db: DB, auth: AU): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("amazon_orders") {
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
}
