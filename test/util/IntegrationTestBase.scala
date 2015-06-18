package util

import org.scalatest.concurrent.IntegrationPatience

trait IntegrationTestBase extends TestBase
  with IntegrationPatience
  with DbTestSupport
