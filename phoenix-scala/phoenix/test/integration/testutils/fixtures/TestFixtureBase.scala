package testutils.fixtures

import phoenix.utils.aliases._
import testutils._
import utils.MockedApis

trait TestFixtureBase extends GimmeSupport with TestActivityContext.AdminAC with MockedApis {

  implicit val db: DB
  implicit val ctx: OC
  implicit val ec: EC
}
