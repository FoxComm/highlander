package util

import java.io.File

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.Result
import utils.aliases._
import utils.apis.{OrderReservation, _}

trait MockedApis extends MockitoSugar {
  lazy val stripeApiMock          = mock[StripeApi]
  lazy val amazonApiMock          = mock[AmazonApi]
  lazy val middlewarehouseApiMock = mock[MiddlewarehouseApi]

  implicit lazy val apisOverride: Apis = Apis(stripeApiMock, amazonApiMock, middlewarehouseApiMock)

  when(middlewarehouseApiMock.reserve(any[OrderReservation])).thenReturn(Result.unit)

  when(amazonApiMock.uploadFile(any[String], any[File])(any[EC]))
    .thenReturn(Result.good("amazon-image-url"))
}
