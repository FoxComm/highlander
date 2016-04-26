package utils

import java.util.concurrent.Executors

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future, blocking}

import cats.data.Xor
import cats.implicits._
import com.stripe.model.{ExternalAccount, DeletedExternalAccount, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import com.stripe.exception.{CardException, StripeException}
import com.stripe.net.RequestOptions
import failures.CreditCardFailures._
import failures.{Failure, Failures, GeneralFailure}
import models.stripe._
import services.{Result, ResultT}

case class Apis(stripe: StripeApi)

trait StripeApi {
  def createCustomer(options: Map[String, AnyRef], secretKey: String): Result[StripeCustomer]

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef], secretKey: String): Result[StripeCard]

  def createCharge(options: Map[String, AnyRef], secretKey: String): Result[StripeCharge]

  def captureCharge(chargeId: String, options: Map[String, AnyRef], secretKey: String): Result[StripeCharge]

  def getExtAccount(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount]

  def findCustomer(id: String, secretKey: String): Result[StripeCustomer]

  def findDefaultCard(customer: StripeCustomer, secretKey: String): Result[StripeCard]

  def updateExternalAccount(card: ExternalAccount, options: Map[String, AnyRef], secretKey: String): Result[ExternalAccount]

  def deleteExternalAccount(card: ExternalAccount, secretKey: String): Result[DeletedExternalAccount]
}

object StripeApi {
  val cardExceptionMap: Map[String, Failure] = Map(
    "invalid_number"        → InvalidNumber,
    "invalid_expiry_month"  → MonthExpirationInvalid,
    "invalid_expiry_year"   → YearExpirationInvalid,
    "invalid_cvc"           → InvalidCvc,
    "incorrect_number"      → IncorrectNumber,
    "expired_card"          → ExpiredCard,
    "incorrect_cvc"         → IncorrectCvc,
    "incorrect_zip"         → IncorrectZip,
    "card_declined"         → CardDeclined,
    "missing"               → Missing,
    "processing_error"      → ProcessingError
  )
}

class WiredStripeApi extends StripeApi {
  private val blockingIOPool: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String, secretKey: String): Result[StripeCustomer] =
    inBlockingPool(secretKey)(requestOptions ⇒ StripeCustomer.retrieve(id, requestOptions))

  def findDefaultCard(customer: StripeCustomer, secretKey: String): Result[StripeCard] =
    inBlockingPool(secretKey)(requestOptions ⇒ customer.getSources.retrieve(customer.getDefaultSource, requestOptions)).
      flatMap(accountToCard)(blockingIOPool)

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

  def deleteExternalAccount(card: ExternalAccount, secretKey: String): Result[DeletedExternalAccount] =
    inBlockingPool(secretKey)(requestOptions ⇒ card.delete(requestOptions))

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

