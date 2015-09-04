package services

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

import cats.data.{XorT, Xor}
import com.stripe.exception.{CardException, InvalidRequestException}
import com.stripe.model.{Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer, ExternalAccount}
import com.stripe.net.{RequestOptions ⇒ StripeRequestOptions}
import models.{CreditCard, Customer}

import payloads.{EditCreditCard, CreateCreditCard}

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
final case class StripeGateway(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {

  // Creates a customer in Stripe along with their first CC
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf"))
  def createCustomerAndCard(customer: Customer, card: CreateCreditCard)
    (implicit ec: ExecutionContext): Result[(StripeCustomer, StripeCard)] = tryFutureWrap {

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


    val stripeCustomer = StripeCustomer.create(mapAsJavaMap(params), options)
    val extAccount = stripeCustomer.getSources.retrieve(stripeCustomer.getDefaultSource, options)

    // lol, really?
    if (extAccount.getObject.equals("card")) {
      Xor.right((stripeCustomer, extAccount.asInstanceOf[StripeCard]))
    } else {
      Xor.left(StripeCouldNotCreateCard.single)
    }
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

  def editCard(cc: CreditCard, payload: EditCreditCard)
    (implicit ec: ExecutionContext): Result[ExternalAccount] = tryFutureWrap {

    val stripeCustomer = StripeCustomer.retrieve(cc.gatewayCustomerId)
    val stripeCard = stripeCustomer.getSources.retrieve(cc.gatewayCardId)

    val options = List[Option[(String, String)]](
      payload.address.map("address_line1" → _),
      payload.address2.map("address_line2" → _),
      payload.state.map("address_state" → _),
      payload.zip.map("address_zip" → _),
      payload.city.map("address_city" → _),
      payload.holderName.map("name" → _),
      payload.expYear.map("exp_year" → _.toString),
      payload.expMonth.map("exp_month" → _.toString)
    )

    val params = options.collect { case Some(o) ⇒ o }.toMap[String, Object]

    Xor.right(stripeCard.update(mapAsJavaMap(params)))
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
