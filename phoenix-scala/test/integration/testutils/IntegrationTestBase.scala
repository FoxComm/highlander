package testutils

import org.scalatest.AppendedClues

trait IntegrationTestBase
    extends TestBase
    with DbTestSupport
    with GimmeSupport
    with AppendedClues {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

}
