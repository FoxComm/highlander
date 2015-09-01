package services

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import com.stripe.exception.{CardException, InvalidRequestException}
import com.stripe.model.{Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import com.stripe.net.{RequestOptions ⇒ StripeRequestOptions}
import models.Customer

import payloads.CreateCreditCard

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
final case class StripeGateway(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  // Creates a customer in Stripe along with their first CC
  def createCustomerAndCard(customer: Customer, card: CreateCreditCard)
    (implicit ec: ExecutionContext): Result[StripeCustomer] = tryFutureWrap {

    val base = Map[String, Object](
      "description" -> "FoxCommerce",
      "email" -> customer.email
    )

    val source = Map[String, Object](
      "object" -> "card",
      "number" -> card.number,
      "exp_month" -> card.expMonth.toString,
      "exp_year" -> card.expYear.toString,
      "cvc" -> card.cvv.toString,
      "name" -> card.holderName
    )

    val params = card.address.fold(base.updated("source", mapAsJavaMap(source))) { address =>
      val sourceWithAddress = source ++ Map[String, Object](
        "address_line1" -> address.street1,
        "address_line2" -> address.street2.orNull,
        "address_city" -> address.city,
        // "address_state" -> address.state.orNull,
        "address_zip" -> address.zip
      )
      base.updated("source", mapAsJavaMap(sourceWithAddress))
    }

    Xor.right(StripeCustomer.create(mapAsJavaMap(params), options))
  }

  def authorizeAmount(customerId: String, amount: Int)
                     (implicit ec: ExecutionContext): Result[String] = tryFutureWrap {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
      "customer" -> customerId, "capture" -> capture)

    val charge = StripeCharge.create(mapAsJavaMap(chargeMap), options)
    /*
      TODO: https://stripe.com/docs/api#create_charge
      Since we're using tokenized, we presumably pass verification process, but might want to handle here
    */

    Xor.right(charge.getId)
  }

  private [this] def tryFutureWrap[A](f: ⇒ Failures Xor A)
                                     (implicit ec: ExecutionContext): Result[A] = {
    Future(f).recoverWith {
      case t: InvalidRequestException ⇒ Result.failure(StripeFailure(t))
      case t: CardException           ⇒ Result.failure(StripeFailure(t))
    }
  }

  private [this] def options: StripeRequestOptions = StripeRequestOptions.builder().setApiKey(this.apiKey).build()
}
