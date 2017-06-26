package phoenix.utils.apis

import cats.implicits._
import com.stripe.exception.{CardException, StripeException}
import com.stripe.model.{DeletedCard, ExternalAccount, Token, Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.failures._
import java.util.concurrent.{Executors, ScheduledExecutorService}

import com.stripe.Stripe
import phoenix.failures.StripeFailures._
import phoenix.utils._
import phoenix.utils.apis.StripeMappings.cardExceptionMap

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Low-level Stripe API wrapper implementation.
  * All calls should be executed in blocking pool.
  * If you add new methods, be sure to provide default mock in `MockedApis` trait for testing!
  */
class StripeWrapper(timeout: FiniteDuration) extends StripeApiWrapper with LazyLogging {
  Stripe.setConnectTimeout((timeout.toMillis / 3).toInt)
  Stripe.setReadTimeout(timeout.toMillis.toInt)

  def retrieveToken(t: String): Result[Token] = {
    logger.info(s"Retrieve token details: $t")
    inBlockingPool(Token.retrieve(t))
  }

  private[this] implicit val scheduler: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor()

  private[this] implicit val blockingIOPool: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String): Result[StripeCustomer] = {
    logger.info(s"Find customer, id: $id")
    inBlockingPool(StripeCustomer.retrieve(id))
  }

  def findCardByCustomerId(gatewayCustomerId: String, gatewayCardId: String): Result[StripeCard] = {
    logger.info(s"Find card for customer, customer id: $gatewayCustomerId, card id: $gatewayCardId")
    inBlockingPool(StripeCustomer.retrieve(gatewayCustomerId).getSources.retrieve(gatewayCardId))
      .flatMapEither(accountToCard)
  }

  def findCardForCustomer(stripeCustomer: StripeCustomer, gatewayCardId: String): Result[StripeCard] = {
    logger.info(s"Find card for customer, customer: $stripeCustomer, card id: $gatewayCardId")
    inBlockingPool(stripeCustomer.getSources.retrieve(gatewayCardId)).flatMapEither(accountToCard)
  }

  def getCustomersOnlyCard(stripeCustomer: StripeCustomer): Result[StripeCard] = {
    // No external request ⇒ no logging
    val maybeCard     = stripeCustomer.getSources.getData.headOption
    val cardEitherNot = maybeCard.toRight(CardNotFoundForNewCustomer(stripeCustomer.getId).single)
    accountToCard(cardEitherNot)
  }

  def createCustomer(options: Map[String, AnyRef]): Result[StripeCustomer] = {
    logger.info(s"Create customer, options: $options")
    inBlockingPool(StripeCustomer.create(mapAsJavaMap(options)))
  }

  def createCharge(options: Map[String, AnyRef]): Result[StripeCharge] = {
    logger.info(s"Create charge (auth), options: $options")
    inBlockingPool(StripeCharge.create(mapAsJavaMap(options)))
  }

  def getCharge(chargeId: String): Result[StripeCharge] = {
    logger.info(s"Get charge, id: $chargeId")
    inBlockingPool(StripeCharge.retrieve(chargeId))
  }

  def refundCharge(chargeId: String, options: Map[String, AnyRef]): Result[StripeCharge] = {
    logger.info(s"Refund charge, id: $chargeId, options: $options")
    for {
      charge ← getCharge(chargeId)
      refund ← inBlockingPool(charge.refund(mapAsJavaMap(options)))
    } yield refund
  }

  def captureCharge(chargeId: String, options: Map[String, AnyRef]): Result[StripeCharge] = {
    logger.info(s"Capture charge, id: $chargeId, options: $options")
    for {
      charge  ← getCharge(chargeId)
      capture ← inBlockingPool(charge.capture(mapAsJavaMap(options)))
    } yield capture
  }

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef]): Result[StripeCard] = {
    logger.info(s"Create card, customer: $customer, options: $options")
    inBlockingPool(customer.createCard(mapAsJavaMap(options)))
  }

  def updateCard(card: StripeCard, options: Map[String, AnyRef]): Result[StripeCard] = {
    logger.info(s"Update card, card: $card, options: $options")
    inBlockingPool(card.update(options))
  }

  def deleteCard(card: StripeCard): Result[DeletedCard] = {
    logger.info(s"Delete card, card: $card")
    inBlockingPool(card.delete())
  }

  // TODO: This needs a life-cycle hook so we can shut it down.

  private def accountToCard(account: Either[Failures, ExternalAccount]): Result[StripeCard] =
    account match {
      case Left(xs) ⇒
        Result.failures(xs)
      case Right(c: StripeCard) if c.getObject.equals("card") ⇒
        Result.good(c)
      case _ ⇒
        Result.failure(GeneralFailure("Not a stripe card: " ++ account.toString))
    }

  /**
    * Executes code inside an execution context that is optimised for blocking I/O operations and returns a Future.
    * Stripe exceptions are caught and turned into a [[StripeFailure]].
    */
  // param: ⇒ A makes method param "lazy". Do not remove!
  @inline protected[utils] final def inBlockingPool[A <: AnyRef](action: ⇒ A): Result[A] = {
    // TODO: don’t we need to catch Future (and DBIO) failures like that in general? Also handling ExecutionException. See dispatch.EnrichedFuture#either @michalrus
    val apiCall = Future(Either.right(action))
      .recover {
        case t: CardException if cardExceptionMap.contains(t.getCode) ⇒
          Either.left(cardExceptionMap(t.getCode).single)
        case t: StripeException ⇒
          Either.left(StripeFailure(t).single)
      }
      .timeoutAfter(timeout, logger)(
        Either.left(StripeProcessingFailure("Request to Stripe timed out").single))
    Result.fromFEither(apiCall)
  }
}
