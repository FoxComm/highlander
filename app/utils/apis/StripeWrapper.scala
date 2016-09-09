package utils.apis

import java.util.concurrent.Executors

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future, blocking}

import cats.data.Xor
import cats.implicits._
import com.stripe.exception.{CardException, StripeException}
import com.stripe.model.{DeletedExternalAccount, ExternalAccount, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import failures.StripeFailures.StripeFailure
import failures.{Failures, GeneralFailure}
import services.{Result, ResultT}
import utils.aliases.stripe._
import utils.apis.StripeMappings.cardExceptionMap

/**
  * Low-level Stripe API wrapper implementation.
  * All calls should be executed in blocking pool.
  */
class StripeWrapper extends StripeApiWrapper {

  private val blockingIOPool: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String): Result[StripeCustomer] =
    inBlockingPool(StripeCustomer.retrieve(id))

  def findDefaultCard(customer: StripeCustomer): Result[StripeCard] =
    inBlockingPool(customer.getSources.retrieve(customer.getDefaultSource))
      .flatMap(accountToCard)(blockingIOPool)

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
