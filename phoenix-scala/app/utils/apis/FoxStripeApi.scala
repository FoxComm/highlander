package utils.apis

import com.stripe.model.DeletedCard
import models.location.Address
import models.payment.creditcard.CreditCard
import payloads.PaymentPayloads.CreateCreditCardFromSourcePayload
import services._
import utils.Money._
import utils.aliases._
import utils.aliases.stripe._

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

  def authorizeAmount(customerId: String,
                      creditCardId: String,
                      amount: Int,
                      currency: Currency): Result[StripeCharge]

  def captureCharge(chargeId: String, amount: Int): Result[StripeCharge]

  def editCard(cc: CreditCard): Result[StripeCard]

  def deleteCard(cc: CreditCard): Result[DeletedCard]

}
