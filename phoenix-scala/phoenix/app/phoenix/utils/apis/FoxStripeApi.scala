package phoenix.utils.apis

import core.db._
import core.utils.Money._
import com.stripe.model.{DeletedCard, Token}
import phoenix.models.location.Address
import phoenix.models.payment.creditcard.CreditCard
import phoenix.payloads.PaymentPayloads.CreateCreditCardFromSourcePayload
import phoenix.utils.aliases.stripe._
import core.utils.Money.Currency
import core.db._

/**
  * Fox Stripe API wrapper
  */
trait FoxStripeApi {

  def createCardFromToken(email: Option[String],
                          token: String,
                          stripeCustomerId: Option[String],
                          address: Address)(implicit ec: EC): Result[(StripeCustomer, StripeCard)]

  @deprecated(message = "Use `createCardFromToken` instead", "Until we are PCI compliant")
  def createCardFromSource(email: Option[String],
                           card: CreateCreditCardFromSourcePayload,
                           stripeCustomerId: Option[String],
                           address: Address)(implicit ec: EC): Result[(StripeCustomer, StripeCard)]

  def authorizeAmount(
      stripeTokenId: String,
      amount: Long,
      currency: Currency,
      customerId: Option[String] = None // Unnecessary for one time payments like Apple Pay
  ): Result[StripeCharge]

  def captureCharge(chargeId: String, amount: Long): Result[StripeCharge]

  def authorizeRefund(chargeId: String, amount: Long, reason: RefundReason): Result[StripeCharge]

  def editCard(cc: CreditCard): Result[StripeCard]

  def deleteCard(cc: CreditCard): Result[DeletedCard]

  def retrieveToken(t: String): Result[StripeToken]

}

sealed abstract class RefundReason(val apiValue: String)
object RefundReason {
  case object Duplicate           extends RefundReason("duplicate")
  case object Fraudulent          extends RefundReason("fraudulent")
  case object RequestedByCustomer extends RefundReason("requested_by_customer")
}
