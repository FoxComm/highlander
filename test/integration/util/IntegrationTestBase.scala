package util

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.{Millisecond, Seconds, Span}

trait IntegrationTestBase extends TestBase
  with AbstractPatienceConfiguration
  with DbTestSupport {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = Span(6, Seconds),
    interval = Span(1, Millisecond)
  )

  implicit val timeout = Timeout(6, TimeUnit.SECONDS)
}
