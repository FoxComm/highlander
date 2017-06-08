package testutils

import org.scalatest.BeforeAndAfterEach
import scala.concurrent.ExecutionContextExecutor
import utils.MockedApis

trait IntegrationTestBase
    extends TestBase
    with DbTestSupport
    with GimmeSupport
    with JwtTestAuth
    with MockedApis
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global

  override protected def beforeEach(): Unit =
    kafkaMock.clear()
}
