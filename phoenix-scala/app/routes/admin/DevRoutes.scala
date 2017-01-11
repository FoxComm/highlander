package routes.admin

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import models.location.Address
import payloads.AddressPayloads.CreateAddressPayload
import payloads.OrderPayloads.OrderTimeMachine
import services.Authenticator.AuthData
import services.orders.TimeMachine
import utils._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object DevRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], tr: TR, tracer: TEI) = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("order-time-machine") {
        (post & pathEnd & entity(as[OrderTimeMachine])) { payload ⇒
          mutateOrFailures {
            TimeMachine.changePlacedAt(payload.referenceNumber, payload.placedAt)
          }
        }
      } ~
      pathPrefix("credit-card-token") {
        (post & pathEnd & entity(as[CreditCardDetailsPayload])) { payload ⇒
          goodOrFailures {
            TestStripeSupport
              .createToken(cardNumber = payload.cardNumber,
                           expMonth = payload.expMonth,
                           expYear = payload.expYear,
                           cvv = payload.cvv,
                           address = Address.fromPayload(payload.address, payload.customerId))
              .map(_.map { token ⇒
                CreditCardTokenResponse(token = token.getId,
                                        brand = token.getCard.getBrand,
                                        lastFour = token.getCard.getLast4)
              })
          }
        }
      } ~
      pathPrefix("version") {
        (get & pathEnd) {
          complete(renderPlain(version))
        }
      }
    }
  }

  lazy val version: String = {
    val stream      = getClass.getResourceAsStream("/version")
    val versionFile = scala.io.Source.fromInputStream(stream)
    try {
      versionFile.getLines.toSeq.mkString("\n")
    } catch {
      case _: Throwable ⇒ "No version file found!"
    }
  }
}

// FOR TESTING PURPOSES ONLY. I WILL CHEW YOUR FACE OFF IF YOU MOVE THIS ELSEWHERE --Anna
case class CreditCardDetailsPayload(customerId: Int,
                                    cardNumber: String,
                                    expMonth: Int,
                                    expYear: Int,
                                    cvv: Int,
                                    address: CreateAddressPayload)

case class CreditCardTokenResponse(token: String, brand: String, lastFour: String)
