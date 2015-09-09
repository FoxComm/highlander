package utils

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import com.stripe.exception.StripeException
import com.stripe.model.{Customer ⇒ StripeCustomer, ExternalAccount}
import com.stripe.model.{Card ⇒ StripeCard, ExternalAccount}
import collection.JavaConversions.mapAsJavaMap

import cats.data.Xor
import com.stripe.net.RequestOptions
import services.{RuntimeExceptionFailure, Result, Failures}

final case class Apis(stripe: StripeApi)

trait StripeApi {
  def createCustomer(options: Map[String, AnyRef], secretKey: String): Result[StripeCustomer]
  def getExtAccount(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount]
  def findCustomer(id: String, secretKey: String): Result[StripeCustomer]
  def findCard(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount]
  def updateExternalAccount(card: ExternalAccount, options: Map[String, AnyRef], secretKey: String): Result[ExternalAccount]
}

class WiredStripeApi extends StripeApi {
  // TODO: Name threads sensibly
  private val blockingEC: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def findCustomer(id: String, secretKey: String): Result[StripeCustomer] =
    async(secretKey)(requestOptions ⇒ StripeCustomer.retrieve(id, requestOptions))

  def findCard(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount] =
    async(secretKey)(requestOptions ⇒ customer.getSources.retrieve(id, requestOptions))

  def createCustomer(options: Map[String, AnyRef], secretKey: String): Result[StripeCustomer] =
    async(secretKey)(requestOptions ⇒ StripeCustomer.create(mapAsJavaMap(options), requestOptions))

  def getExtAccount(customer: StripeCustomer, id: String, secretKey: String): Result[ExternalAccount] =
    async(secretKey)(requestOptions ⇒ customer.getSources.retrieve(id, requestOptions))

  def updateExternalAccount(card: ExternalAccount, options: Map[String, AnyRef], secretKey: String): Result[ExternalAccount] =
    async(secretKey)(requestOptions ⇒ card.update(options))

  // TODO: This needs a life-cycle hook so we can shut it down.
  //       It does not share the Actor system’s thread pool by design,
  //       since it does blocking IO like it’s 1991.
  private def async[A](secretKey: String)(code: RequestOptions ⇒ A): Future[Failures Xor A] = {
    val requestOptions = RequestOptions.builder().setApiKey(secretKey).build()

    implicit val ec: ExecutionContext = blockingEC

    Future(code(requestOptions))(blockingEC).
      flatMap(Result.good).
      recoverWith {
        case re: RuntimeException ⇒ Result.failure(RuntimeExceptionFailure(re))
      }
  }
}

