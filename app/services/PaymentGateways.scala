package services

import models.{Customer, TokenizedCreditCard}
import payloads.CreditCardPayload

import com.stripe.Stripe
import com.stripe.model.{Token, Card => StripeCard, Charge => StripeCharge, Customer => StripeCustomer}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future}

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
case class StripeGateway(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  def getTokenizedCard(paymentToken: String)(implicit ec: ExecutionContext): Future[(TokenizedCreditCard, StripeCard) Or Throwable] = {
    val reqOpts = StripeRequestOptions.builder().setApiKey(this.apiKey).build()

    Future {
      try {
        val retrievedToken = Token.retrieve(paymentToken, reqOpts)
        val stripeCard = retrievedToken.getCard
        Good((TokenizedCreditCard.fromStripe(stripeCard, paymentToken), stripeCard))
      } catch {
        case t: com.stripe.exception.InvalidRequestException =>
          Bad(t)
      }
    }
  }

  def createCustomer(customer: Customer, cardPayload: CreditCardPayload)
                    (implicit ec: ExecutionContext): Future[StripeCustomer Or ErrorMessage] = {
    val params: Map[String, Object] = Map(
      "description" -> "FoxCommerce",
      "email" -> customer.email,
      "source" -> Map(
        "object" -> "card",
        "number" -> cardPayload.number,
        "exp_month" -> cardPayload.expMonth.toString,
        "exp_year" -> cardPayload.expYear.toString,
        "cvc" -> cardPayload.cvv.toString,
        "name" -> cardPayload.holderName
        // TODO(yax): would like to add fields "address_line1", "address_city", "address_state", "address_zip"
      )
    )

    Future {
      try {
        val stripeCustomer = StripeCustomer.create(mapAsJavaMap(params), options)
        Good(stripeCustomer)
      } catch {
        case t: com.stripe.exception.InvalidRequestException =>
          Bad(t.getMessage)
      }
    }
  }

  def authorizeAmount(tokenizedCard: TokenizedCreditCard, amount: Int)
                     (implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
      "source" -> tokenizedCard.gatewayTokenId, "capture" -> capture)

    Future {
      try {
        val charge = StripeCharge.create(mapAsJavaMap(chargeMap), options)
        /*
          TODO: https://stripe.com/docs/api#create_charge
          Since we're using tokenized, we presumably pass verification process, but might want to handle here
        */
        Good(charge.getId)
      } catch {
        case t: com.stripe.exception.StripeException =>
          Bad(List(t.getMessage))
      }
    }
  }

  private [this] def options: StripeRequestOptions = StripeRequestOptions.builder().setApiKey(this.apiKey).build()
}
