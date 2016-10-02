import org.scalatest.Suite
import org.scalatest.concurrent.PatienceConfiguration
import util.DbTestSupport

trait PhoenixStorefrontApi extends HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒
}
