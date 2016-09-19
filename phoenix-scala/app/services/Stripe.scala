package services

import scala.collection.JavaConversions.mapAsJavaMap

import cats.data.Xor
import cats.data.Xor.{left, right}
import cats.implicits._
import com.stripe.model.{DeletedExternalAccount, ExternalAccount}
import failures.CustomerFailures.CustomerMustHaveCredentials
import failures.{CreditCardFailures, Failures}
import models.location.Address
import models.payment.creditcard.CreditCard
import payloads.PaymentPayloads.CreateCreditCard
import utils.Money._
import utils.aliases._
import utils.aliases.stripe._
import utils.apis._

case class Stripe(implicit apis: Apis, ec: EC) {

  val api: StripeApi = apis.stripe

  // Creates a account in Stripe along with their first CC
  def createCard(email: Option[String],
                 card: CreateCreditCard,
                 stripeAccountId: Option[String],
                 address: Address) = email match {
    case Some(e) ⇒ createCardInner(e, card, stripeAccountId, address)
    case _       ⇒ Result.failure(CustomerMustHaveCredentials)
  }

  def createCardInner(email: String,
                      card: CreateCreditCard,
                      stripeAccountId: Option[String],
                      address: Address): Result[(StripeCustomer, StripeCard)] = {

    val source = Map[String, Object](
        "object"        → "card",
        "number"        → card.cardNumber,
        "exp_month"     → card.expMonth.toString,
        "exp_year"      → card.expYear.toString,
        "cvc"           → card.cvv,
        "name"          → card.holderName,
        "address_line1" → address.address1,
        "address_line2" → address.address2.orNull,
        "address_city"  → address.city,
        "address_zip"   → address.zip
    )

    def existingCustomer(id: String): ResultT[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object]("source" → mapAsJavaMap(source))

      for {
        cust ← ResultT(getCustomer(id))
        card ← ResultT(api.createCard(cust, params))
      } yield (cust, card)
    }

    def newCustomer: ResultT[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object](
          "description" → "FoxCommerce",
          "email"       → email,
          "source"      → mapAsJavaMap(source)
      )

      for {
        cust ← ResultT(api.createCustomer(params))
        card ← ResultT(getCard(cust))
        _    ← ResultT.fromXor(cvcCheck(card))
      } yield (cust, card)
    }

    stripeAccountId.fold(newCustomer)(existingCustomer).value
  }

  def authorizeAmount(accountId: String, amount: Int, currency: Currency): Result[StripeCharge] = {
    val chargeMap: Map[String, Object] = Map(
        "amount"   → amount.toString,
        "currency" → currency.toString,
        "customer" → accountId,
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
      customer   ← ResultT(getCustomer(cc.gatewayAccountId))
      stripeCard ← ResultT(getCard(customer))
      updated    ← ResultT(update(stripeCard))
    } yield updated).value
  }

  def deleteCard(cc: CreditCard): Result[DeletedExternalAccount] = {
    (for {
      customer   ← ResultT(getCustomer(cc.gatewayAccountId))
      stripeCard ← ResultT(getCard(customer))
      updated    ← ResultT(api.deleteExternalAccount(stripeCard))
    } yield updated).value
  }

  private def getCustomer(id: String): Result[StripeCustomer] =
    api.findCustomer(id)

  private def getCard(customer: StripeCustomer): Result[StripeCard] =
    api.findDefaultCard(customer)

  private def cvcCheck(card: StripeCard): Failures Xor StripeCard = {
    card.getCvcCheck.some.getOrElse("").toLowerCase match {
      case "pass" ⇒ right(card)
      case _      ⇒ left(CreditCardFailures.InvalidCvc.single)
    }
  }
}
