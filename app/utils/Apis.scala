package utils

import java.util.concurrent.Executors

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future, blocking}

import cats.data.Xor
import cats.implicits._
import com.stripe.exception.StripeException
import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer, ExternalAccount, Charge ⇒ StripeCharge}
import com.stripe.exception.{StripeException, CardException}
import com.stripe.net.RequestOptions
import services.CreditCardFailure.StripeFailure
import services.{ResultT, CreditCardFailure, Failure, Failures, GeneralFailure, Result}

final case class Apis(stripe: StripeApi)

trait StripeApi {
  def createCustomer(options: Map[String, AnyRef], secretKey: String): Result[StripeCustomer]

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef], secretKey: String): Result[StripeCard]

  def createCharge(options: Map[String, AnyRef], secretKey: String): Result[StripeCharge]

  def captureCharge(chargeId: String, options: Map[String, AnyRef], secretKey: String): Result[StripeCharge]

  def getExtAccount(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount]

  def findCustomer(id: String, secretKey: String): Result[StripeCustomer]

  def findDefaultCard(customer: StripeCustomer, secretKey: String): Result[StripeCard]

  def updateExternalAccount(card: ExternalAccount, options: Map[String, AnyRef], secretKey: String): Result[ExternalAccount]
}

object StripeApi {
  val cardExceptionMap: Map[String, Failure] = Map(
    "invalid_number"        → CreditCardFailure.InvalidNumber,
    "invalid_expiry_month"  → CreditCardFailure.MonthExpirationInvalid,
    "invalid_expiry_year"   → CreditCardFailure.YearExpirationInvalid,
    "invalid_cvc"           → CreditCardFailure.InvalidCvc,
    "incorrect_number"      → CreditCardFailure.IncorrectNumber,
    "expired_card"          → CreditCardFailure.ExpiredCard,
    "incorrect_cvc"         → CreditCardFailure.IncorrectCvc,
    "incorrect_zip"         → CreditCardFailure.IncorrectZip,
    "card_declined"         → CreditCardFailure.CardDeclined,
    "missing"               → CreditCardFailure.Missing,
    "processing_error"      → CreditCardFailure.ProcessingError
  )
}

class WiredStripeApi extends StripeApi {
  private val blockingIOPool: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String, secretKey: String): Result[StripeCustomer] =
    inBlockingPool(secretKey)(requestOptions ⇒ StripeCustomer.retrieve(id, requestOptions))

  def findDefaultCard(customer: StripeCustomer, secretKey: String): Result[StripeCard] =
    inBlockingPool(secretKey)(requestOptions ⇒ customer.getSources.retrieve(customer.getDefaultSource, requestOptions)).
      flatMap(accountToCard)(blockingIOPool)

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.IsInstanceOf", "org.brianmckenna.wartremover.warts.AsInstanceOf"))
  final def accountToCard(account: Failures Xor ExternalAccount): Result[StripeCard] = account match {
    case Xor.Left(xs) ⇒
      Result.failures(xs)
    case Xor.Right(c: StripeCard) if c.getObject.equals("card") ⇒
      Result.good(c)
    case _ ⇒
      Result.failure(GeneralFailure("Not a stripe card: " ++ account.toString))
  }

  def createCustomer(options: Map[String, AnyRef], secretKey: String): Result[StripeCustomer] =
    inBlockingPool(secretKey)(requestOptions ⇒ StripeCustomer.create(mapAsJavaMap(options), requestOptions))

  def createCharge(options: Map[String, AnyRef], secretKey: String): Result[StripeCharge] =
    inBlockingPool(secretKey)(requestOptions ⇒ StripeCharge.create(mapAsJavaMap(options), requestOptions))

  def getCharge(chargeId: String, secretKey: String): Result[StripeCharge] =
    inBlockingPool(secretKey)(requestOptions ⇒ StripeCharge.retrieve(chargeId, requestOptions))

  def captureCharge(chargeId: String, options: Map[String, AnyRef], secretKey: String): Result[StripeCharge] = {
    // for ResultT
    implicit val ec: ExecutionContext = blockingIOPool

    (for {
      charge  ← ResultT(getCharge(chargeId, secretKey))
      capture ← ResultT(inBlockingPool(secretKey)(requestOptions ⇒ charge.capture(mapAsJavaMap(options),
        requestOptions)))
    } yield capture).value
  }

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef], secretKey: String): Result[StripeCard] =
    inBlockingPool(secretKey)(requestOptions ⇒ customer.createCard(mapAsJavaMap(options), requestOptions))

  def getExtAccount(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount] =
    inBlockingPool(secretKey)(requestOptions ⇒ customer.getSources.retrieve(id, requestOptions))

  def updateExternalAccount(card: ExternalAccount, options: Map[String, AnyRef], secretKey: String): Result[ExternalAccount] =
    inBlockingPool(secretKey)(requestOptions ⇒ card.update(options, requestOptions))

  // TODO: This needs a life-cycle hook so we can shut it down.

  /**
  * Executes code inside an execution context that is optimised for blocking I/O operations and returns a Future.
  * Stripe exceptions are caught and turned into a [[StripeFailure]].
  */
  @inline protected [utils] final def inBlockingPool[A](secretKey: String)(thunk: RequestOptions ⇒ A): Future[Failures Xor A] = {
    val requestOptions = RequestOptions.builder().setApiKey(secretKey).build()

    implicit val ec: ExecutionContext = blockingIOPool

    Future(Xor.right(blocking(thunk(requestOptions)))).recoverWith {
      case t: CardException if StripeApi.cardExceptionMap.contains(t.getCode) ⇒
        Result.failure(StripeApi.cardExceptionMap(t.getCode))
      case t: StripeException ⇒
        Result.failure(StripeFailure(t))
    }
  }
}

