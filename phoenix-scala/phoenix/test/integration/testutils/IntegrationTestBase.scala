package testutils

import utils.MockedApis

import scala.concurrent.ExecutionContextExecutor

trait IntegrationTestBase
    extends TestBase
    with DbTestSupport
    with GimmeSupport
    with JwtTestAuth
    with MockedApis {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global

}
