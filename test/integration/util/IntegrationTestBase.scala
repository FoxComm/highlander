package util

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.{Millisecond, Seconds, Span}

trait IntegrationTestBase extends TestBase
  with AbstractPatienceConfiguration
  with DbTestSupport {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = Span(6, Seconds),
    interval = Span(1, Millisecond)
  )
}
