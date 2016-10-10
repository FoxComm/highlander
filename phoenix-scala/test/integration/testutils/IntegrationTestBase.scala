package testutils

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.{Millisecond, Seconds, Span}

trait IntegrationTestBase
    extends TestBase
    with AbstractPatienceConfiguration
    with DbTestSupport
    with GimmeSupport {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
      timeout = Span(600, Seconds),
      interval = Span(1, Millisecond)
  )

  implicit val timeout: Timeout = Timeout(600, TimeUnit.SECONDS)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
}
