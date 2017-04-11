package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import org.json4s.native.JsonParser.StringVal
import payloads.AmazonOrderPayloads._
import services.AmazonOrderManager
import responses.cord.AmazonOrderResponse._
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AmazonOrderRoutes {
  def routes(implicit ec: EC, db: DB, auth: AU): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("amazon_orders") {
        (get & pathEnd) {
          getOrFailures {
            AmazonOrderManager.listAmazonOrders()
          }
        } ~
        pathPrefix(Segment) { amazonOrderId ⇒
          (get & pathEnd) {
            getOrFailures {
              AmazonOrderManager.findByAmazonOrderId(amazonOrderId)
            }
          }
        }
      }
    }
  }
}
