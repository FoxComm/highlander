package testutils.apis

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LoginPayload
import payloads.UserPayloads._
import testutils._
import cats.implicits._

trait PhoenixPublicApi extends HttpSupport { self: FoxSuite ⇒

  object publicApi {
    val rootPrefix: String = "v1/public"

    object prepare {
      def login(payload: LoginPayload): HttpRequest =
        buildRequest(HttpMethods.POST, s"$rootPrefix/login", payload.some)

      def logout(): HttpRequest =
        buildRequest(HttpMethods.POST, s"$rootPrefix/logout")

      def register(payload: CreateCustomerPayload): HttpRequest =
        buildRequest(HttpMethods.POST, s"$rootPrefix/registrations/new", payload.some)
    }

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
