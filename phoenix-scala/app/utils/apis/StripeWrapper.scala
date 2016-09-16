package utils.apis

import java.util.concurrent.Executors

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, blocking}

import cats.data.Xor
import cats.implicits._
import com.stripe.exception.{CardException, StripeException}
import com.stripe.model.{DeletedExternalAccount, ExternalAccount, Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import failures.StripeFailures.{CardNotFoundForNewCustomer, StripeFailure}
import failures.{Failures, GeneralFailure}
import services.{Result, ResultT}
import utils.apis.StripeMappings.cardExceptionMap

/**
  * Low-level Stripe API wrapper implementation.
  * All calls should be executed in blocking pool.
  * If you add new methods, be sure to provide default mock in `MockedApis` trait for testing!
  */
class StripeWrapper extends StripeApiWrapper {

  private val blockingIOPool: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String): Result[StripeCustomer] =
    inBlockingPool(StripeCustomer.retrieve(id))

  def findCardByCustomerId(gatewayCustomerId: String, gatewayCardId: String): Result[StripeCard] =
    inBlockingPool(StripeCustomer.retrieve(gatewayCustomerId).getSources.retrieve(gatewayCardId))
      .flatMap(accountToCard)(blockingIOPool)

  def findCardForCustomer(stripeCustomer: StripeCustomer,
                          gatewayCardId: String): Result[StripeCard] =
    inBlockingPool(stripeCustomer.getSources.retrieve(gatewayCardId))
      .flatMap(accountToCard)(blockingIOPool)

  def getCustomersOnlyCard(stripeCustomer: StripeCustomer): Result[StripeCard] = {
    val maybeCard  = stripeCustomer.getSources.getData.headOption
    val cardXorNot = maybeCard.toRightXor(CardNotFoundForNewCustomer(stripeCustomer.getId).single)
    accountToCard(cardXorNot)
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
  @inline protected[utils] final def inBlockingPool[A <: AnyRef](
      action: ⇒ A): Future[Failures Xor A] = {
    implicit val ec: ExecutionContext = blockingIOPool

    Future(Xor.right(blocking(action))).recoverWith {
      case t: CardException if cardExceptionMap.contains(t.getCode) ⇒
        Result.failure(cardExceptionMap(t.getCode))
      case t: StripeException ⇒
        Result.failure(StripeFailure(t))
    }
  }
}
