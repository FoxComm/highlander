package utils

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import com.stripe.model.{DeletedExternalAccount, ExternalAccount}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.Result
import utils.TestStripeSupport.randomStripeishId
import utils.aliases._
import utils.aliases.stripe._
import utils.apis._

trait MockedApis extends MockitoSugar {

  val stripeCustomer = {
    val stripeCustomer = new StripeCustomer
    stripeCustomer.setId(s"cus_$randomStripeishId")
    stripeCustomer
  }

  val stripeCard = {
    val stripeCard = spy(new StripeCard)
    doReturn(s"card_$randomStripeishId", Nil: _*).when(stripeCard).getId
    stripeCard
  }

  lazy val stripeWrapperMock: StripeWrapper = initStripeApiMock(mock[StripeWrapper])
  lazy val stripeApiMock: FoxStripe         = new FoxStripe(stripeWrapperMock)

  def initStripeApiMock(mocked: StripeWrapper): StripeWrapper = {
    reset(mocked)

    when(mocked.findCustomer(any())).thenReturn(Result.good(stripeCustomer))
    when(mocked.createCustomer(any())).thenReturn(Result.good(stripeCustomer))

    when(mocked.findDefaultCard(any())).thenReturn(Result.good(stripeCard))
    when(mocked.createCard(any(), any())).thenReturn(Result.good(stripeCard))

    when(mocked.updateExternalAccount(any(), any())).thenReturn(Result.good(new ExternalAccount))
    when(mocked.deleteExternalAccount(any())).thenReturn(Result.good(new DeletedExternalAccount))

    when(mocked.captureCharge(any(), any())).thenReturn(Result.good(new StripeCharge))
    when(mocked.createCharge(any())).thenReturn(Result.good(new StripeCharge))

    mocked
  }

  lazy val amazonApiMock = {
    val mocked = mock[AmazonApi]
    when(mocked.uploadFile(any[String], any[File])(any[EC]))
      .thenReturn(Result.good("amazon-image-url"))
    mocked
  }

  lazy val middlewarehouseApiMock = {
    val mocked = mock[MiddlewarehouseApi]
    when(mocked.hold(any[OrderInventoryHold])(any[EC])).thenReturn(Result.unit)
    when(mocked.cancelHold(any[String])(any[EC])).thenReturn(Result.unit)
    mocked
  }

  implicit lazy val apisOverride: Apis = Apis(stripeApiMock, amazonApiMock, middlewarehouseApiMock)

  implicit lazy val es: ElasticsearchApi = utils.ElasticsearchApi.default()
}
