package services

import models.TokenizedCreditCard

import com.stripe.Stripe
import com.stripe.model.{Token, Card => StripeCard, Charge => StripeCharge}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import collection.JavaConversions.mapAsJavaMap
import scala.concurrent.{ExecutionContext, Future}

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
// TODO(yax): make this a future b/c DON'T BLOCK
case class StripeGateway(paymentToken: String, apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {
  def getTokenizedCard(implicit ec: ExecutionContext): Future[(TokenizedCreditCard, StripeCard) Or Throwable] = {
    val reqOpts = StripeRequestOptions.builder().setApiKey(this.apiKey).build()

    Future {
      try {
        val retrievedToken = Token.retrieve(this.paymentToken, reqOpts)
        val stripeCard = retrievedToken.getCard
        Good((TokenizedCreditCard.fromStripe(stripeCard, this.paymentToken), stripeCard))
      } catch {
        case t: com.stripe.exception.InvalidRequestException =>
          Bad(t)
      }
    }
  }

  def authorizeAmount(tokenizedCard: TokenizedCreditCard, amount: Int)
                     (implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
      "source" -> tokenizedCard.gatewayTokenId, "capture" -> capture)
    val reqOpts = StripeRequestOptions.builder().setApiKey(this.apiKey).build()

    Future {
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
}
