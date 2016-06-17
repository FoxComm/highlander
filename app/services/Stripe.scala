package services

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.Future

import cats.data.Xor
import cats.data.Xor.{left, right}
import cats.implicits._
import com.stripe.model.{DeletedExternalAccount, ExternalAccount}
import failures.{CreditCardFailures, Failures, StripeFailures}
import models.location.Address
import models.payment.creditcard.CreditCard
import payloads.PaymentPayloads.CreateCreditCard
import utils.Money._
import utils.apis._
import utils.aliases._
import utils.aliases.stripe._
import utils.FoxConfig.{RichConfig, config}

case class Stripe(apiKey: Option[String])(implicit apis: Apis, ec: EC) {

  val api: StripeApi = apis.stripe

  // Creates a customer in Stripe along with their first CC
  def createCard(email: String,
                 card: CreateCreditCard,
                 stripeCustomerId: Option[String],
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

      withApiKey { key ⇒
        for {
          cust ← ResultT(getCustomer(id))
          card ← ResultT(api.createCard(cust, params, key))
        } yield (cust, card)
      }
    }

    def newCustomer: ResultT[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object](
          "description" → "FoxCommerce",
          "email"       → email,
          "source"      → mapAsJavaMap(source)
      )

      withApiKey { key ⇒
        for {
          cust ← ResultT(api.createCustomer(params, key))
          card ← ResultT(getCard(cust))
          _    ← ResultT.fromXor(cvcCheck(card))
        } yield (cust, card)
      }
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

    withApiKey { key ⇒
      api.createCharge(chargeMap, key)
    }
  }

  def captureCharge(chargeId: String, amount: Int): Result[StripeCharge] =
    withApiKey { key ⇒
      api.captureCharge(chargeId, Map[String, Object]("amount" → amount.toString), key)
    }

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

      withApiKey { key ⇒
        api.updateExternalAccount(stripeCard, params, key)
      }
    }

    (for {
      customer   ← ResultT(getCustomer(cc.gatewayCustomerId))
      stripeCard ← ResultT(getCard(customer))
      updated    ← ResultT(update(stripeCard))
    } yield updated).value
  }

  def deleteCard(cc: CreditCard): Result[DeletedExternalAccount] = {
    withApiKey { key ⇒
      (for {
        customer   ← ResultT(getCustomer(cc.gatewayCustomerId))
        stripeCard ← ResultT(getCard(customer))
        updated    ← ResultT(api.deleteExternalAccount(stripeCard, key))
      } yield updated).value
    }
  }

  private def getCustomer(id: String): Result[StripeCustomer] =
    withApiKey { key ⇒
      api.findCustomer(id, key)
    }

  private def getCard(customer: StripeCustomer): Result[StripeCard] =
    withApiKey { key ⇒
      api.findDefaultCard(customer, key)
    }

  private def cvcCheck(card: StripeCard): Failures Xor StripeCard = {
    card.getCvcCheck.some.getOrElse("").toLowerCase match {
      case "pass" ⇒ right(card)
      case _      ⇒ left(CreditCardFailures.InvalidCvc.single)
    }
  }

  private def withApiKey[T](func: String ⇒ Result[T]): Result[T] =
    apiKey match {
      case Some(key) ⇒ func(key)
      case _         ⇒ Result.left(StripeFailures.UnableToReadStripeApiKey.single)
    }

  private def withApiKey[T](func: String ⇒ ResultT[T]): ResultT[T] =
    apiKey match {
      case Some(key) ⇒ func(key)
      case _         ⇒ ResultT.leftAsync(StripeFailures.UnableToReadStripeApiKey.single)
    }
}

object Stripe {
  def apply()(implicit apis: Apis, ec: EC): Stripe = {
    val token = config.getOptString("stripe.key")
    new Stripe(token)
  }
}
