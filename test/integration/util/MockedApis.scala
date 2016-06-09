package util

import org.scalatest.mock.MockitoSugar
import utils.apis.{AmazonS3, Apis, StripeApi}

trait MockedApis extends MockitoSugar {
  lazy val stripeApiMock = mock[StripeApi]
  lazy val amazonApiMock = mock[AmazonS3]

  implicit lazy val apisOverride: Apis = Apis(stripeApiMock, amazonApiMock)
}
