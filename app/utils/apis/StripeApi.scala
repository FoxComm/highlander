package utils.apis

import java.util.concurrent.Executors

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future, blocking}

import cats.data.Xor
import cats.implicits._
import com.stripe.exception.{CardException, StripeException}
import com.stripe.model.{DeletedExternalAccount, ExternalAccount, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import failures.CreditCardFailures._
import failures.StripeFailures.StripeFailure
import failures.{Failure, Failures, GeneralFailure}
import services.{Result, ResultT}
import utils.aliases.stripe._

trait StripeApi {

  def createCustomer(options: Map[String, AnyRef]): Result[StripeCustomer]

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef]): Result[StripeCard]

  def createCharge(options: Map[String, AnyRef]): Result[StripeCharge]

  def captureCharge(chargeId: String, options: Map[String, AnyRef]): Result[StripeCharge]

  def getExtAccount(customer: StripeCustomer, id: String): Result[ExternalAccount]

  def findCustomer(id: String): Result[StripeCustomer]

  def findDefaultCard(customer: StripeCustomer): Result[StripeCard]

  def updateExternalAccount(card: ExternalAccount,
                            options: Map[String, AnyRef]): Result[ExternalAccount]

  def deleteExternalAccount(card: ExternalAccount): Result[DeletedExternalAccount]
}

object StripeApi {
  val cardExceptionMap: Map[String, Failure] = Map(
      "invalid_number"       → InvalidNumber,
      "invalid_expiry_month" → MonthExpirationInvalid,
      "invalid_expiry_year"  → YearExpirationInvalid,
      "invalid_cvc"          → InvalidCvc,
      "incorrect_number"     → IncorrectNumber,
      "expired_card"         → ExpiredCard,
      "incorrect_cvc"        → IncorrectCvc,
      "incorrect_zip"        → IncorrectZip,
      "card_declined"        → CardDeclined,
      "missing"              → Missing,
      "processing_error"     → ProcessingError
  )
}

class WiredStripeApi extends StripeApi {
  private val blockingIOPool: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String): Result[StripeCustomer] =
    inBlockingPool(StripeCustomer.retrieve(id))

  def findDefaultCard(customer: StripeCustomer): Result[StripeCard] =
    inBlockingPool(customer.getSources.retrieve(customer.getDefaultSource))
      .flatMap(accountToCard)(blockingIOPool)

  final def accountToCard(account: Failures Xor ExternalAccount): Result[StripeCard] =
    account match {
      case Xor.Left(xs) ⇒
        Result.failures(xs)
      case Xor.Right(c: StripeCard) if c.getObject.equals("card") ⇒
        Result.good(c)
      case _ ⇒
        Result.failure(GeneralFailure("Not a stripe card: " ++ account.toString))
    }

  def createCustomer(options: Map[String, AnyRef]): Result[StripeCustomer] =
    inBlockingPool(StripeCustomer.create(mapAsJavaMap(options)))

  def createCharge(options: Map[String, AnyRef]): Result[StripeCharge] =
    inBlockingPool(StripeCharge.create(mapAsJavaMap(options)))

  def getCharge(chargeId: String): Result[StripeCharge] =
    inBlockingPool(StripeCharge.retrieve(chargeId))

  def captureCharge(chargeId: String, options: Map[String, AnyRef]): Result[StripeCharge] = {
    // for ResultT
    implicit val ec: ExecutionContext = blockingIOPool

    (for {
      charge  ← ResultT(getCharge(chargeId))
      capture ← ResultT(inBlockingPool(charge.capture(mapAsJavaMap(options))))
    } yield capture).value
  }

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef]): Result[StripeCard] =
    inBlockingPool(customer.createCard(mapAsJavaMap(options)))

  def getExtAccount(customer: StripeCustomer, id: String): Result[ExternalAccount] =
    inBlockingPool(customer.getSources.retrieve(id))

  def updateExternalAccount(card: ExternalAccount,
                            options: Map[String, AnyRef]): Result[ExternalAccount] =
    inBlockingPool(card.update(options))

  def deleteExternalAccount(card: ExternalAccount): Result[DeletedExternalAccount] =
    inBlockingPool(card.delete())

  // TODO: This needs a life-cycle hook so we can shut it down.

  /**
    * Executes code inside an execution context that is optimised for blocking I/O operations and returns a Future.
    * Stripe exceptions are caught and turned into a [[StripeFailure]].
    */
  // param: ⇒ A makes method param "lazy". Do not remove!
  @inline protected[utils] final def inBlockingPool[A <: AnyRef](
      action: ⇒ A): Future[Failures Xor A] = {
    implicit val ec: ExecutionContext = blockingIOPool

    Future(Xor.right(blocking(action))).recoverWith {
      case t: CardException if StripeApi.cardExceptionMap.contains(t.getCode) ⇒
        Result.failure(StripeApi.cardExceptionMap(t.getCode))
      case t: StripeException ⇒
        Result.failure(StripeFailure(t))
    }
  }
}
