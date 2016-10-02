package util

import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration

trait PhoenixStorefrontApi extends HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒
}
