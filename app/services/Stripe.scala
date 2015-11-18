package services

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

import cats.data.{XorT, Xor}
import cats.data.Xor.{left, right}
import com.stripe.exception.{AuthenticationException, StripeException, CardException, InvalidRequestException}
import com.stripe.model.{Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer, Account, ExternalAccount}
import com.stripe.net.{RequestOptions ⇒ StripeRequestOptions}
import models.{CreditCard, Customer, Address}

import payloads.{EditCreditCard, CreateCreditCard}
import utils.{StripeApi, Apis}
import utils.Money._

// TODO(yax): do not default apiKey, it should come from store
final case class Stripe(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ")
  (implicit apis: Apis, ec: ExecutionContext) {

  val api: StripeApi = apis.stripe

  // Creates a customer in Stripe along with their first CC
  def createCard(email: String, card: CreateCreditCard,
    stripeCustomerId: Option[String], address: Address): Result[(StripeCustomer, StripeCard)] = {

    val source = Map[String, Object](
      "object"        → "card",
      "number"        → card.number,
      "exp_month"     → card.expMonth.toString,
      "exp_year"      → card.expYear.toString,
      "cvc"           → card.cvv.toString,
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
        card ← ResultT(api.createCard(cust, params, apiKey))
      } yield (cust, card)
    }

    def newCustomer: ResultT[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object](
        "description" → "FoxCommerce",
        "email"       → email,
        "source"      → mapAsJavaMap(source)
      )

      for {
        cust  ← ResultT(api.createCustomer(params, apiKey))
        card  ← ResultT(getCard(cust))
        _     ← ResultT.fromXor(cvcCheck(card))
      } yield (cust, card)
    }

    stripeCustomerId.fold(newCustomer)(existingCustomer).value
  }

  def authorizeAmount(customerId: String, amount: Int, currency: Currency): Result[StripeCharge] = {
    val chargeMap: Map[String, Object] = Map(
      "amount"    → amount.toString,
      "currency"  → currency.toString,
      "customer"  → customerId,
      "capture"   → (false: java.lang.Boolean)
    )

    api.createCharge(chargeMap, apiKey)
  }

  def captureCharge(chargeId: String, amount: Int): Result[StripeCharge] =
    api.captureCharge(chargeId, Map[String, Object]("amount" → amount.toString), apiKey)

  def editCard(cc: CreditCard): Result[ExternalAccount] = {

    def update(stripeCard: StripeCard): Result[ExternalAccount] = {

      val params = Map[String, Object](
        "address_line1" → cc.address1,
        "address_line2" → cc.address2,
        // ("address_state" → cc.region),
        "address_zip" → cc.zip,
        "address_city" → cc.city,
        "name" → cc.addressName,
        "exp_year" → cc.expYear.toString,
        "exp_month" → cc.expMonth.toString
      )

      api.updateExternalAccount(stripeCard, params, this.apiKey)
    }

    (for {
      customer    ← ResultT(getCustomer(cc.gatewayCustomerId))
      stripeCard  ← ResultT(getCard(customer))
      updated     ← ResultT(update(stripeCard))
    } yield updated).value
  }

  private def getCustomer(id: String): Result[StripeCustomer] =
    api.findCustomer(id, this.apiKey)

  private def getCard(customer: StripeCustomer): Result[StripeCard] =
    api.findDefaultCard(customer, this.apiKey)

  private def cvcCheck(card: StripeCard): Failures Xor StripeCard = {
    card.getCvcCheck.some.getOrElse("").toLowerCase match {
      case "pass" ⇒ right(card)
      case _      ⇒ left(CreditCardFailure.InvalidCvc.single)
    }
  }
}

