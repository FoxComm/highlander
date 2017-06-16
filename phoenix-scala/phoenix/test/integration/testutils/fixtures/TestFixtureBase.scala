package testutils.fixtures

import phoenix.utils.aliases._
import testutils._
import utils.MockedApis

trait TestFixtureBase extends GimmeSupport with TestActivityContext.AdminAC with MockedApis {

  implicit def db: DB
  implicit def ctx: OC
  implicit def ec: EC
}
