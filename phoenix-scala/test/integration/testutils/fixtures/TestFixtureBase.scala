package testutils.fixtures

import testutils._
import utils.MockedApis
import utils.aliases._

trait TestFixtureBase extends GimmeSupport with TestActivityContext.AdminAC with MockedApis {

  implicit val db: DB
  implicit val ctx: OC
  implicit val ec: EC
}
