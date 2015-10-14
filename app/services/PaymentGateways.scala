package services

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

import cats.data.{XorT, Xor}
import cats.data.Xor.{left, right}
import com.stripe.exception.{StripeException, CardException, InvalidRequestException}
import com.stripe.model.{Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer, Account, ExternalAccount}
import com.stripe.net.{RequestOptions ⇒ StripeRequestOptions}
import models.{CreditCard, Customer, Address}

import payloads.{EditCreditCard, CreateCreditCard}
import utils.{StripeApi, Apis}

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
final case class StripeGateway(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {

  // Creates a customer in Stripe along with their first CC
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf"))
  def createCard(email: String, card: CreateCreditCard, stripeCustomerId: Option[String], address: Address)
    (implicit ec: ExecutionContext, apis: Apis): Result[(StripeCustomer, StripeCard)] = {

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
        card ← ResultT(tryFutureWrap[StripeCard] {
          Xor.right(cust.createCard(mapAsJavaMap(params), options))
        })
      } yield (cust, card)
    }

    def newCustomer: ResultT[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object](
        "description" → "FoxCommerce",
        "email"       → email,
        "source"      → mapAsJavaMap(source)
      )

      for {
        cust  ← ResultT(tryFutureWrap[StripeCustomer] {
          Xor.right(StripeCustomer.create(mapAsJavaMap(params), options))
        })
        card  ← ResultT(getCard(cust))
        _     ← ResultT.fromXor(cvcCheck(card))
      } yield (cust, card)
    }

    stripeCustomerId.fold(newCustomer)(existingCustomer).value
  }

  def authorizeAmount(customerId: String, amount: Int)
                     (implicit ec: ExecutionContext): Result[String] = tryFutureWrap {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" → "100", "currency" → "usd",
      "customer" → customerId, "capture" → capture)

    val charge = StripeCharge.create(mapAsJavaMap(chargeMap), options)
    /*
      TODO: https://stripe.com/docs/api#create_charge
      Since we're using tokenized, we presumably pass verification process, but might want to handle here
    */

    Xor.right(charge.getId)
  }

  def editCard(cc: CreditCard)
    (implicit ec: ExecutionContext, apis: Apis): Result[ExternalAccount] = {

    def update(stripeCard: StripeCard)
      (implicit ec: ExecutionContext): Result[ExternalAccount] = {

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

      apis.stripe.updateExternalAccount(stripeCard, params, this.apiKey)
    }

    (for {
      customer    ← ResultT(getCustomer(cc.gatewayCustomerId))
      stripeCard  ← ResultT(getCard(customer))
      updated     ← ResultT(update(stripeCard))
    } yield updated).value
  }

  private def getCustomer(id: String)
    (implicit ec: ExecutionContext, apis: Apis): Result[StripeCustomer] =
    apis.stripe.findCustomer(id, this.apiKey)


  private def getCard(customer: StripeCustomer)
    (implicit ec: ExecutionContext, apis: Apis): Result[StripeCard] =
      apis.stripe.findDefaultCard(customer, this.apiKey)

  private def cvcCheck(card: StripeCard): Failures Xor StripeCard = {
    card.getCvcCheck.some.getOrElse("").toLowerCase match {
      case "pass" ⇒ right(card)
      case _      ⇒ left(CVCFailure.single)
    }
  }

  private [this] def tryFutureWrap[A](f: ⇒ Failures Xor A)
                                     (implicit ec: ExecutionContext): Result[A] = {
    Future(f).recoverWith {
      case t: CardException if t.getCode == "incorrect_cvc" ⇒
        Result.failure(CVCFailure)
      case t: StripeException ⇒
        Result.failure(StripeFailure(t))
    }
  }

  private [this] def options: StripeRequestOptions = StripeRequestOptions.builder().setApiKey(this.apiKey).build()
}
