package utils

import java.io.File

import cats.implicits._
import com.stripe.model.DeletedCard
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import testutils.TestBase
import utils.TestStripeSupport.randomStripeishId
import utils.aliases._
import utils.aliases.stripe._
import utils.apis._
import utils.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}

trait MockedApis extends MockitoSugar {

  val stripeCustomer: StripeCustomer = newStripeCustomer
  def newStripeCustomer: StripeCustomer = {
    val stripeCustomer = new StripeCustomer
    stripeCustomer.setId(s"cus_$randomStripeishId")
    stripeCustomer
  }

  val stripeCard: StripeCard = newStripeCard
  def newStripeCard: StripeCard = {
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
    when(mocked.createCharge(any())).thenAnswer(new Answer[Result[StripeCharge]] {
      def answer(invocation: InvocationOnMock): Result[StripeCharge] = {
        val map    = invocation.getArgument[Map[String, AnyRef]](0)
        val charge = new StripeCharge
        map.get("amount").flatMap(s ⇒ Try(s.toString.toInt).toOption).foreach(charge.setAmount(_))
        map.get("currency").foreach(s ⇒ charge.setCurrency(s.toString))
        charge.setId(Random.nextString(10))
        Result.good(charge)
      }
    })
    when(mocked.refundCharge(any(), any())).thenAnswer(new Answer[Result[StripeCharge]] {
      def answer(invocation: InvocationOnMock): Result[StripeCharge] = {
        val id     = invocation.getArgument[String](0)
        val map    = invocation.getArgument[Map[String, AnyRef]](1)
        val charge = new StripeCharge
        map
          .get("amount")
          .flatMap(s ⇒ Try(s.toString.toInt).toOption)
          .foreach(charge.setAmountRefunded(_))
        charge.setId(id)
        Result.good(charge)
      }
    })

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

  lazy val amazonApiMock: AmazonApi = {
    val mocked = mock[AmazonApi]
    when(mocked.uploadFile(any[String], any[File])(any[EC]))
      .thenReturn(Result.good("amazon-image-url"))
    mocked
  }

  lazy val middlewarehouseApiMock: MiddlewarehouseApi = {
    val mocked = mock[MiddlewarehouseApi]
    when(mocked.hold(any[OrderInventoryHold])(any[EC], any[AU])).thenReturn(Result.unit)
    when(mocked.cancelHold(any[String])(any[EC], any[AU])).thenReturn(Result.unit)
    mocked
  }

  lazy val elasticSearchMock: ElasticsearchApi = mock[ElasticsearchApi] // TODO: fill me with some defaults

  implicit lazy val apisOverride: Apis =
    Apis(stripeApiMock, amazonApiMock, middlewarehouseApiMock, elasticSearchMock)
}
