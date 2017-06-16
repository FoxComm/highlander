package utils

import cats.implicits._
import com.stripe.model.DeletedCard
import core.db._
import java.io.File
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.MockProducer
import org.json4s.jackson.JsonMethods._
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import phoenix.models.activity.OpaqueActivity
import phoenix.models.inventory.ProductVariantSku
import phoenix.server.Setup
import phoenix.services.activity.ActivityBase
import phoenix.utils.ElasticsearchApi
import phoenix.utils.JsonFormatters._
import phoenix.utils.TestStripeSupport.randomStripeishId
import phoenix.utils.aliases._
import phoenix.utils.aliases.stripe._
import phoenix.utils.apis._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Random, Try}
import shapeless.syntax.typeable._

trait RealStripeApi extends MockedApis {

  override implicit def apisOverride: Option[Apis] =
    Apis(Setup.setupStripe(), amazonApiMock, middlewarehouseApiMock, elasticSearchMock, kafkaMock).some
}

trait MockedApis extends MockitoSugar with MustMatchers with OptionValues with AppendedClues {
  private[this] implicit def formats = phoenixFormats

  implicit def apis: Apis =
    Apis(stripeApiMock, amazonApiMock, middlewarehouseApiMock, elasticSearchMock, kafkaMock)

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
      .thenReturn(Result.good("http://amazon-image.url/1"))
    when(mocked.uploadFileF(any[String], any[File])(any[EC]))
      .thenReturn(Future.successful("http://amazon-image.url/1"))
    mocked
  }

  lazy val middlewarehouseApiMock: MiddlewarehouseApi = {
    val mocked = mock[MiddlewarehouseApi]
    when(mocked.hold(any[OrderInventoryHold])(any[EC], any[AU])).thenReturn(Result.unit)
    when(mocked.cancelHold(any[String])(any[EC], any[AU])).thenReturn(Result.unit)
    when(mocked.createSku(any[Int], any[CreateSku])(any[EC], any[AU]))
      .thenReturn(DbResultT.pure(ProductVariantSku(skuId = -1, mwhSkuId = -1)))
    mocked
  }

  lazy val elasticSearchMock: ElasticsearchApi = mock[ElasticsearchApi] // TODO: fill me with some defaults?

  lazy val kafkaMock: MockProducer[GenericData.Record, GenericData.Record] =
    new MockProducer[GenericData.Record, GenericData.Record](true, null, null)

  implicit def apisOverride: Option[Apis] = apis.some

  def assertActivities(assertion: List[OpaqueActivity] ⇒ Assertion): Assertion =
    assertion(
      kafkaMock
        .history()
        .asScala
        .map { record ⇒
          val activityKind = Option(record.value().get("kind"))
            .flatMap(_.cast[String])
            .value
            .withClue("Failed to find activity kind inside kafka record")
          val data = Option(record.value().get("data"))
            .flatMap(_.cast[String])
            .flatMap(parseOpt(_))
            .value
            .withClue("Failed to find activity data inside kafka record")

          OpaqueActivity(activityKind, data)
        }(collection.breakOut))

  def mustProduceActivity[A <: ActivityBase[A]: Manifest](entity: A): Assertion =
    assertActivities(_ must contain(entity.toOpaque))
}
