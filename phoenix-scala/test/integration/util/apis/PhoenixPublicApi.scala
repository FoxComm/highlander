package util.apis

import akka.http.scaladsl.model.HttpResponse

import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import payloads.CustomerPayloads._
import util.{DbTestSupport, HttpSupport}

trait PhoenixPublicApi extends HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒

  object publicApi {
    val rootPrefix: String = "v1/public"

    def sendPasswordReset(payload: ResetPasswordSend): HttpResponse =
      POST(s"$rootPrefix/send-password-reset", payload)

    def resetPassword(payload: ResetPassword): HttpResponse =
      POST(s"$rootPrefix/reset-password", payload)

    def giftCardTypes(): HttpResponse =
      GET(s"$rootPrefix/gift-cards/types")

    def storeCreditTypes(): HttpResponse =
      GET(s"$rootPrefix/store-credits/types")

    def getReason(reasonType: String): HttpResponse =
      GET(s"$rootPrefix/reasons/$reasonType")
  }
}
