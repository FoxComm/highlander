package services

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._

import cats.data.{XorT, Xor}
import cats.data.Xor.{left, right}
import com.stripe.exception.{StripeException, CardException, InvalidRequestException}
import com.stripe.model.{Card ⇒ StripeCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer, ExternalAccount}
import com.stripe.net.{RequestOptions ⇒ StripeRequestOptions}
import models.{CreditCard, Customer, Address}

import payloads.{EditCreditCard, CreateCreditCard}

abstract class PaymentGateway
case object BraintreeGateway extends PaymentGateway

// TODO(yax): do not default apiKey, it should come from store
final case class StripeGateway(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") extends PaymentGateway {

  // Creates a customer in Stripe along with their first CC
  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf"))
  def createCustomerAndCard(customer: Customer, card: CreateCreditCard, stripeId: Option[String], address: Address)
    (implicit ec: ExecutionContext): Result[(StripeCustomer, StripeCard)] = {

    val base = Map[String, Object](
      "description" -> "FoxCommerce",
      "email" -> customer.email
    )

    val source = Map[String, Object](
      "object" -> "card",
      "number" -> card.number,
      "exp_month" -> card.expMonth.toString,
      "exp_year" -> card.expYear.toString,
      "cvc" -> card.cvv.toString,
      "name" -> card.holderName
    )

    val params = card.address.fold(base.updated("source", mapAsJavaMap(source))) { address =>
      val sourceWithAddress = source ++ Map[String, Object](
        "address_line1" -> address.street1,
        "address_line2" -> address.street2.orNull,
        "address_city" -> address.city,
        // "address_state" -> address.state.orNull,
        "address_zip" -> address.zip
      )
      base.updated("source", mapAsJavaMap(sourceWithAddress))
    }

    def create: ResultT[StripeCustomer] =
      ResultT(tryFutureWrap[StripeCustomer]{ Xor.right(StripeCustomer.create(mapAsJavaMap(params), options)) })

    (for {
      sCustomer ← create
      card      ← ResultT(getCard(sCustomer))
      _         ← ResultT.fromXor(cvcCheck(card))
    } yield (sCustomer, card)).value
  }

  def authorizeAmount(customerId: String, amount: Int)
                     (implicit ec: ExecutionContext): Result[String] = tryFutureWrap {
    val capture: java.lang.Boolean = false
    val chargeMap: Map[String, Object] = Map("amount" -> "100", "currency" -> "usd",
      "customer" -> customerId, "capture" -> capture)

    val charge = StripeCharge.create(mapAsJavaMap(chargeMap), options)
    /*
      TODO: https://stripe.com/docs/api#create_charge
      Since we're using tokenized, we presumably pass verification process, but might want to handle here
    */

    Xor.right(charge.getId)
  }

  def editCard(cc: CreditCard)
    (implicit ec: ExecutionContext): Result[ExternalAccount] = {

    def update(stripeCard: StripeCard)
      (implicit ec: ExecutionContext): Result[ExternalAccount] = {

      val params = Map[String, Object](
        "address_line1" → cc.street1,
        "address_line2" → cc.street2,
        // ("address_state" → cc.region),
        "address_zip" → cc.zip,
        "address_city" → cc.city,
        "name" → cc.addressName,
        "exp_year" → cc.expYear.toString,
        "exp_month" → cc.expMonth.toString
      )

      tryFutureWrap[ExternalAccount]{ right(stripeCard.update(mapAsJavaMap(params), options)) }
    }

    (for {
      customer    ← ResultT(getCustomer(cc.gatewayCustomerId))
      stripeCard  ← ResultT(getCard(customer))
      updated     ← ResultT(update(stripeCard))
    } yield updated).value
  }

  private def getCustomer(id: String)
    (implicit ec: ExecutionContext): Result[StripeCustomer] =
    tryFutureWrap[StripeCustomer] { right(StripeCustomer.retrieve(id, options)) }

  private def getCard(customer: StripeCustomer)
    (implicit ec: ExecutionContext): Result[StripeCard] = (for {
    account ← ResultT(tryFutureWrap[ExternalAccount] {
      right(customer.getSources.retrieve(customer.getDefaultSource, options))
    })
    card ← ResultT(Future.successful(toCard(account)))
  } yield card).value

  private def cvcCheck(card: StripeCard): Failures Xor StripeCard = {
    card.getCvcCheck.some.getOrElse("").toLowerCase match {
      case "pass" ⇒ right(card)
      case _      ⇒ left(CVCFailure.single)
    }
  }

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf", "org.brianmckenna.wartremover.warts.Null"))
  private def toCard(extAccount: ExternalAccount)
    (implicit ec: ExecutionContext): Failures Xor StripeCard =
    if (extAccount.getObject.equals("card"))
      right(extAccount.asInstanceOf[StripeCard])
    else
      left(GeneralFailure("externalAccount is not a stripe card").single)

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
