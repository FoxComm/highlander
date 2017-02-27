package utils.apis

import java.util.concurrent.Executors

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, blocking}

import cats.data.Xor
import cats.implicits._
import com.stripe.exception.{CardException, StripeException}
import com.stripe.model.{DeletedCard, ExternalAccount, Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import com.typesafe.scalalogging.LazyLogging
import failures.StripeFailures.{CardNotFoundForNewCustomer, StripeFailure}
import failures.{Failures, GeneralFailure}
import services.Result
import utils.apis.StripeMappings.cardExceptionMap

/**
  * Low-level Stripe API wrapper implementation.
  * All calls should be executed in blocking pool.
  * If you add new methods, be sure to provide default mock in `MockedApis` trait for testing!
  */
class StripeWrapper extends StripeApiWrapper with LazyLogging {

  private[this] implicit val blockingIOPool: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String): Result[StripeCustomer] = {
    logger.info(s"Find customer, id: $id")
    inBlockingPool(StripeCustomer.retrieve(id))
  }

  def findCardByCustomerId(gatewayCustomerId: String, gatewayCardId: String): Result[StripeCard] = {
    logger.info(
        s"Find card for customer, customer id: $gatewayCustomerId, card id: $gatewayCardId")
    inBlockingPool(StripeCustomer.retrieve(gatewayCustomerId).getSources.retrieve(gatewayCardId))
      .flatMapXor(accountToCard)
  }

  def findCardForCustomer(stripeCustomer: StripeCustomer,
                          gatewayCardId: String): Result[StripeCard] = {
    logger.info(s"Find card for customer, customer: $stripeCustomer, card id: $gatewayCardId")
    inBlockingPool(stripeCustomer.getSources.retrieve(gatewayCardId)).flatMapXor(accountToCard)
  }

  def getCustomersOnlyCard(stripeCustomer: StripeCustomer): Result[StripeCard] = {
    // No external request ⇒ no logging
    val maybeCard  = stripeCustomer.getSources.getData.headOption
    val cardXorNot = maybeCard.toRightXor(CardNotFoundForNewCustomer(stripeCustomer.getId).single)
    accountToCard(cardXorNot)
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

  private def accountToCard(account: Failures Xor ExternalAccount): Result[StripeCard] =
    account match {
      case Xor.Left(xs) ⇒
        Result.failures(xs)
      case Xor.Right(c: StripeCard) if c.getObject.equals("card") ⇒
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
    val f = Future(Xor.right(blocking(action))).recover {
      case t: CardException if cardExceptionMap.contains(t.getCode) ⇒
        Xor.left(cardExceptionMap(t.getCode).single)
      case t: StripeException ⇒
        Xor.left(StripeFailure(t).single)
      // TODO: what about case _? @michalrus
    }

    Result.fromFXor(f)
  }
}
