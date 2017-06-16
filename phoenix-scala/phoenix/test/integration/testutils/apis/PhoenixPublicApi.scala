package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.Cookie
import cats.implicits._
import phoenix.payloads.LoginPayload
import phoenix.payloads.UserPayloads._
import testutils._

trait PhoenixPublicApi extends HttpSupport { self: FoxSuite ⇒

  object publicApi {
    val rootPrefix: String = "v1/public"

    def getProducts(ref: String): HttpResponse =
      GET(s"$rootPrefix/products/$ref", jwtCookie = None)

    def login(payload: LoginPayload, jwtCookie: Cookie): HttpResponse =
      POST(s"$rootPrefix/login", payload, jwtCookie.some)

    def logout(jwtCookie: Cookie): HttpResponse =
      POST(s"$rootPrefix/logout", jwtCookie.some)

    def sendPasswordReset(payload: ResetPasswordSend): HttpResponse =
      POST(s"$rootPrefix/send-password-reset", payload, jwtCookie = None)

    def resetPassword(payload: ResetPassword): HttpResponse =
      POST(s"$rootPrefix/reset-password", payload, jwtCookie = None)

    def giftCardTypes(): HttpResponse =
      GET(s"$rootPrefix/gift-cards/types", jwtCookie = None)

    def storeCreditTypes(): HttpResponse =
      GET(s"$rootPrefix/store-credits/types", jwtCookie = None)

    def getReason(reasonType: String): HttpResponse =
      GET(s"$rootPrefix/reasons/$reasonType", jwtCookie = None)

    def getCountryById(cId: Int): HttpResponse =
      GET(s"$rootPrefix/countries/$cId", jwtCookie = None)

    def getRegionByShortName(shortName: String): HttpResponse =
      GET(s"$rootPrefix/regions/$shortName", jwtCookie = None)

    def getProduct(reference: String): HttpResponse =
      GET(s"$rootPrefix/products/$reference", jwtCookie = None)
  }
}
