package testutils.apis

import akka.http.scaladsl.model.HttpResponse
import payloads.CapturePayloads
import testutils._

trait PhoenixPaymentApi extends HttpSupport { self: FoxSuite ⇒
  private val prefix = "v1/service"

  case object captureApi {
    val productPath = s"$prefix/capture"

    def capture(payload: CapturePayloads.Capture): HttpResponse = POST(productPath, payload)
  }

}
