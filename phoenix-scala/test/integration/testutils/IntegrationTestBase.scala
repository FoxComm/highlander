package testutils

import utils.MockedApis

import scala.concurrent.ExecutionContextExecutor

trait IntegrationTestBase
    extends TestBase
    with DbTestSupport
    with GimmeSupport
    with MockedApis
    with RealTestAuth {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global

}
