package testutils.fixtures

import testutils._
import utils.MockedApis
import utils.aliases._

trait TestFixtureBase extends GimmeSupport with TestActivityContext.AdminAC with MockedApis {
  implicit def ctx: OC
  implicit def db: DB
  implicit def ec: EC
}
