package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import ch.qos.logback.classic.{Level, Logger ⇒ LogBackLogger}
import org.slf4j.LoggerFactory
import phoenix.models.account.User
import phoenix.models.location.Address
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.OrderPayloads.OrderTimeMachine
import phoenix.services.Authenticator.AuthData
import phoenix.services.orders.TimeMachine
import phoenix.utils.TestStripeSupport
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object DevRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("order-time-machine") {
        (post & pathEnd & entity(as[OrderTimeMachine])) { payload ⇒
          mutateOrFailures {
            TimeMachine.changePlacedAt(payload.referenceNumber, payload.placedAt)
          }
        }
      } ~
      pathPrefix("set-log-level") {
        (post & pathEnd & entity(as[ChangeLogLevel])) { payload ⇒
          complete {
            val logger   = LoggerFactory.getLogger(payload.logger).asInstanceOf[LogBackLogger]
            val oldLevel = logger.getLevel
            val newLevel = Level.toLevel(payload.level, oldLevel)
            logger.setLevel(newLevel)
            ChangeLogLevelResponse(oldLevel = oldLevel.toString,
                                   newLevel = newLevel.toString,
                                   logger = logger.getName)
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
              .map { token ⇒
                CreditCardTokenResponse(token = token.getId,
                                        brand = token.getCard.getBrand,
                                        lastFour = token.getCard.getLast4)
              }
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

/**
  *
  * @param logger is fully qualified name of class where logger are used
  *               for example core.apis.ElasticsearchApi
  * @param level Log Level from LogBack, for example DEBUG, INFO, ALL, etc.
  *              In case of invalid level no changes will be applied.
  */
case class ChangeLogLevel(logger: String, level: String)
case class ChangeLogLevelResponse(oldLevel: String, newLevel: String, logger: String)
