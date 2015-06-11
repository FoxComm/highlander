package services

import models.TokenizedCreditCard

import com.stripe.Stripe
import com.stripe.model.Token
import com.stripe.model.{Charge => StripeCharge}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import scala.util.{Try, Failure, Success}
import collection.JavaConversions.mapAsJavaMap

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
case class StripeGateway(paymentToken: String, apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  def getTokenizedCard: Try[TokenizedCreditCard] = {
    val reqOpts = StripeRequestOptions.builder().setApiKey(this.apiKey).build()

    try {
      val retrievedToken = Token.retrieve(this.paymentToken, reqOpts)
      Success(TokenizedCreditCard(retrievedToken.getCard, this.paymentToken))
    } catch {
      case t: com.stripe.exception.InvalidRequestException =>
        Failure(t)
    }
  }

  def authorizeAmount(tokenizedCard: TokenizedCreditCard, amount: Int): String Or List[ErrorMessage] = {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
      "source" -> tokenizedCard.gatewayTokenId, "capture" -> capture)
    val reqOpts = StripeRequestOptions.builder().setApiKey(this.apiKey).build()

    try {
      val charge = StripeCharge.create(mapAsJavaMap(chargeMap), reqOpts)
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

