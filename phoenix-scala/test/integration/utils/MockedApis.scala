package utils

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global

import com.stripe.model.DeletedCard
import org.mockito.ArgumentMatcher
import com.stripe.model.{DeletedExternalAccount, ExternalAccount}
import models.cord._
import models.cord.lineitems.CartLineItems.FindLineItemResult
import models.location._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.Result
import utils.TestStripeSupport.randomStripeishId
import utils.aliases._
import utils.aliases.stripe._
import utils.apis._

trait MockedApis extends MockitoSugar {

  val stripeCustomer = newStripeCustomer
  def newStripeCustomer = {
    val stripeCustomer = new StripeCustomer
    stripeCustomer.setId(s"cus_$randomStripeishId")
    stripeCustomer
  }

  val stripeCard = newStripeCard
  def newStripeCard = {
    val stripeCard = spy(new StripeCard)
    doReturn(s"card_$randomStripeishId", Nil: _*).when(stripeCard).getId
    stripeCard
  }

  lazy val stripeWrapperMock: StripeWrapper = initStripeApiMock(mock[StripeWrapper])
  lazy val stripeApiMock: FoxStripe         = new FoxStripe(stripeWrapperMock)

  /**
    * All values are initialized just to get you through anything that might call Stripe without NullPointer
    * because Mockito called something on an uninitialized mock.
    * If you add a method to StripeWrapper, it *must* be mocked here for `any()` args.
    */
  def initStripeApiMock(mocked: StripeWrapper): StripeWrapper = {
    reset(mocked)

    when(mocked.findCustomer(any())).thenReturn(Result.good(stripeCustomer))
    when(mocked.createCustomer(any())).thenReturn(Result.good(stripeCustomer))

    when(mocked.findCardForCustomer(any(), any())).thenReturn(Result.good(stripeCard))
    when(mocked.getCustomersOnlyCard(any())).thenReturn(Result.good(stripeCard))
    when(mocked.findCardByCustomerId(any(), any())).thenReturn(Result.good(stripeCard))
    when(mocked.createCard(any(), any())).thenReturn(Result.good(stripeCard))

    when(mocked.updateCard(any(), any())).thenReturn(Result.good(stripeCard))
    when(mocked.deleteCard(any())).thenReturn(Result.good(new DeletedCard()))

    when(mocked.captureCharge(any(), any())).thenReturn(Result.good(new StripeCharge))
    when(mocked.createCharge(any())).thenReturn(Result.good(new StripeCharge))

    mocked
  }

  def cardStripeIdMatches(expectedCardId: String) = new ArgumentMatcher[StripeCard]() {
    override def matches(other: StripeCard): Boolean = {
      val theyMatch = other.getId == expectedCardId
      if (!theyMatch)
        System.err.println(s"Expected Stripe card id: $expectedCardId, got ${other.getId}")
      theyMatch
    }
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

  lazy val avalaraApiMock = {
    val mocked = mock[AvalaraApi]
    when(mocked.validateAddress(any[Address], any[Region], any[Country])(any[EC]))
      .thenReturn(Result.unit)
    when(
        mocked.getTaxForCart(any[Cart],
                             any[Seq[FindLineItemResult]],
                             any[Address],
                             any[Region],
                             any[Country],
                             any[Int])(any[EC])).thenReturn(Result.good(0))
    when(
        mocked.getTaxForOrder(any[Cart],
                              any[Seq[FindLineItemResult]],
                              any[Address],
                              any[Region],
                              any[Country],
                              any[Int])(any[EC])).thenReturn(Result.good(0))
    when(mocked.cancelTax(any[Order])(any[EC])).thenReturn(Result.unit)
    mocked
  }

  implicit lazy val apisOverride: Apis =
    Apis(stripeApiMock, amazonApiMock, middlewarehouseApiMock, avalaraApiMock)

  implicit lazy val es: ElasticsearchApi = utils.ElasticsearchApi.default()
}
