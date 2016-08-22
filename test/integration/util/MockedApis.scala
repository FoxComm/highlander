package util

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.Result
import utils.ElasticsearchApi
import utils.aliases._
import utils.apis._

trait MockedApis extends MockitoSugar {

  lazy val stripeApiMock = mock[StripeApi]

  lazy val amazonApiMock = {
    val mocked = mock[AmazonApi]
    when(mocked.uploadFile(any[String], any[File])(any[EC]))
      .thenReturn(Result.good("amazon-image-url"))
    mocked
  }

  lazy val middlewarehouseApiMock = {
    val mocked = mock[MiddlewarehouseApi]
    when(mocked.reserve(any[OrderReservation])(any[EC])).thenReturn(Result.unit)
    mocked
  }

  implicit lazy val apisOverride: Apis = Apis(stripeApiMock, amazonApiMock, middlewarehouseApiMock)

  implicit lazy val es: ElasticsearchApi = utils.ElasticsearchApi.default()
}
