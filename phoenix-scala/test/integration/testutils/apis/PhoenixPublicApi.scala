package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import payloads.LoginPayload
import payloads.UserPayloads._
import testutils._

trait PhoenixPublicApi extends HttpSupport { self: FoxSuite ⇒

  object publicApi {
    val rootPrefix: String = "v1/public"

    def doLogin(payload: LoginPayload): HttpResponse =
      POST(s"$rootPrefix/login", payload)

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
