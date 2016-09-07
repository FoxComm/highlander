package services

import scala.collection.JavaConversions.mapAsJavaMap

import cats.implicits._
import com.stripe.model.{DeletedExternalAccount, ExternalAccount}
import failures.CustomerFailures.CustomerMustHaveCredentials
import models.location.Address
import models.payment.creditcard.CreditCard
import payloads.PaymentPayloads.CreateCreditCardFromSourcePayload
import utils.Money._
import utils.aliases._
import utils.aliases.stripe._
import utils.apis._

case class Stripe(implicit apis: Apis, ec: EC) {

  val api: StripeApi = apis.stripe

  @deprecated(message = "Use `createCardFromToken` instead", "Until we are PCI compliant")
  def createCardFromSource(email: Option[String],
                           card: CreateCreditCardFromSourcePayload,
                           stripeCustomerId: Option[String],
                           address: Address) = {
    lazy val details = Map[String, Object]("object" → "card",
                                           "number"        → card.cardNumber,
                                           "exp_month"     → card.expMonth.toString,
                                           "exp_year"      → card.expYear.toString,
                                           "cvc"           → card.cvv,
                                           "name"          → card.holderName,
                                           "address_line1" → address.address1,
                                           "address_line2" → address.address2.orNull,
                                           "address_city"  → address.city,
                                           "address_zip"   → address.zip)
    lazy val source = Map("source" → mapAsJavaMap(details))

    email match {
      case Some(e) ⇒ createCardAndMaybeCustomer(e, source, stripeCustomerId, address)
      case _       ⇒ Result.failure(CustomerMustHaveCredentials)
    }
  }

  def createCardFromToken(email: Option[String],
                          token: String,
                          stripeCustomerId: Option[String],
                          address: Address): Result[(StripeCustomer, StripeCard)] = email match {
    case Some(e) ⇒
      createCardAndMaybeCustomer(e, Map("source" → token), stripeCustomerId, address)
    case _ ⇒
      Result.failure(CustomerMustHaveCredentials)
  }

  private def createCardAndMaybeCustomer(
      email: String,
      source: Map[String, Object],
      stripeCustomerId: Option[String],
      address: Address): Result[(StripeCustomer, StripeCard)] = {
    def existingCustomer(id: String): ResultT[(StripeCustomer, StripeCard)] = {
      for {
        cust ← ResultT(getCustomer(id))
        card ← ResultT(api.createCard(cust, source))
      } yield (cust, card)
    }

    def newCustomer: ResultT[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object](
            "description" → "FoxCommerce",
            "email"       → email
        ) ++ source

      for {
        cust ← ResultT(api.createCustomer(params))
        card ← ResultT(getCard(cust))
      } yield (cust, card)
    }

    stripeCustomerId.fold(newCustomer)(existingCustomer).value
  }

  def authorizeAmount(customerId: String, amount: Int, currency: Currency): Result[StripeCharge] = {
    val chargeMap: Map[String, Object] = Map(
        "amount"   → amount.toString,
        "currency" → currency.toString,
        "customer" → customerId,
        "capture"  → (false: java.lang.Boolean)
    )

    api.createCharge(chargeMap)
  }

  def captureCharge(chargeId: String, amount: Int): Result[StripeCharge] =
    api.captureCharge(chargeId, Map[String, Object]("amount" → amount.toString))

  def editCard(cc: CreditCard): Result[ExternalAccount] = {

    def update(stripeCard: StripeCard): Result[ExternalAccount] = {

      val params = Map[String, Object](
          "address_line1" → cc.address1,
          "address_line2" → cc.address2,
          // ("address_state" → cc.region),
          "address_zip"  → cc.zip,
          "address_city" → cc.city,
          "name"         → cc.addressName,
          "exp_year"     → cc.expYear.toString,
          "exp_month"    → cc.expMonth.toString
      )

      api.updateExternalAccount(stripeCard, params)
    }

    (for {
      customer   ← ResultT(getCustomer(cc.gatewayCustomerId))
      stripeCard ← ResultT(getCard(customer))
      updated    ← ResultT(update(stripeCard))
    } yield updated).value
  }

  def deleteCard(cc: CreditCard): Result[DeletedExternalAccount] = {
    (for {
      customer   ← ResultT(getCustomer(cc.gatewayCustomerId))
      stripeCard ← ResultT(getCard(customer))
      updated    ← ResultT(api.deleteExternalAccount(stripeCard))
    } yield updated).value
  }

  private def getCustomer(id: String): Result[StripeCustomer] =
    api.findCustomer(id)

  private def getCard(customer: StripeCustomer): Result[StripeCard] =
    api.findDefaultCard(customer)

}
