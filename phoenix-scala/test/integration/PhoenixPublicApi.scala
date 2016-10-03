import akka.http.scaladsl.model.HttpResponse

import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import payloads.CustomerPayloads._
import util.DbTestSupport

trait PhoenixPublicApi extends HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒

  object publicAPI {
    val prefix: String = "v1/public"

    def sendPasswordReset(payload: ResetPasswordSend): HttpResponse =
      POST(s"$prefix/send-password-reset", payload)

    def resetPassword(payload: ResetPassword): HttpResponse =
      POST(s"$prefix/reset-password", payload)
  }
}
