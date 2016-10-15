package testutils

trait IntegrationTestBase extends TestBase with DbTestSupport with GimmeSupport {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
}
