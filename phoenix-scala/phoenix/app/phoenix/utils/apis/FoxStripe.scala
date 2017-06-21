package phoenix.utils.apis

import cats.implicits._
import com.stripe.model.DeletedCard
import com.typesafe.scalalogging.LazyLogging
import core.db._
import core.utils.Money._
import phoenix.failures.CustomerFailures.CustomerMustHaveCredentials
import phoenix.models.location.Address
import phoenix.models.payment.creditcard.CreditCard
import phoenix.payloads.PaymentPayloads.CreateCreditCardFromSourcePayload
import phoenix.utils.aliases.stripe._

import scala.collection.JavaConversions._

object FoxStripe extends LazyLogging {
  val stripe = "api.stripe.com"

  /**
    * As an attempt to investigate issue [1], let's try this naive ping impl.
    * It works on my local, so I want to test it on appliances and staging.
    * -- Anna
    *
    * [1] https://github.com/FoxComm/highlander/issues/2090
    */
  def ping(): Unit = {
    import sys.process._

    if (s"ping -c1 $stripe".run().exitValue() == 0)
      logger.info(s"$stripe ping: SUCCESS")
    else
      logger.error(s"$stripe ping: FAILURE")
  }
}

/**
  * Fox Stripe API implementation
  * @param stripe Low-level implementation of Stripe API
  */
class FoxStripe(stripe: StripeWrapper)(implicit ec: EC) extends FoxStripeApi {

  def createCardFromToken(email: Option[String],
                          token: String,
                          stripeCustomerId: Option[String],
                          address: Address)(implicit ec: EC): Result[(StripeCustomer, StripeCard)] = {
    FoxStripe.ping()
    email match {
      case Some(e) ⇒
        createCardAndMaybeCustomer(e, Map("source" → token), stripeCustomerId, address)
      case _ ⇒
        Result.failure(CustomerMustHaveCredentials)
    }
  }

  @deprecated(message = "Use `createCardFromToken` instead", "Until we are PCI compliant")
  def createCardFromSource(email: Option[String],
                           card: CreateCreditCardFromSourcePayload,
                           stripeCustomerId: Option[String],
                           address: Address)(implicit ec: EC): Result[(StripeCustomer, StripeCard)] = {
    lazy val details = Map[String, Object](
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
    lazy val source = Map("source" → mapAsJavaMap(details))

    email match {
      case Some(e) ⇒ createCardAndMaybeCustomer(e, source, stripeCustomerId, address)
      case _       ⇒ Result.failure(CustomerMustHaveCredentials)
    }
  }

  private def createCardAndMaybeCustomer(
      email: String,
      source: Map[String, Object],
      stripeCustomerId: Option[String],
      address: Address)(implicit ec: EC): Result[(StripeCustomer, StripeCard)] = {
    def existingCustomer(id: String): Result[(StripeCustomer, StripeCard)] =
      for {
        cust ← stripe.findCustomer(id)
        card ← stripe.createCard(cust, source)
      } yield (cust, card)

    def newCustomer: Result[(StripeCustomer, StripeCard)] = {
      val params = Map[String, Object](
        "description" → "FoxCommerce",
        "email"       → email
      ) ++ source

      for {
        cust ← stripe.createCustomer(params)
        card ← stripe.getCustomersOnlyCard(cust)
      } yield (cust, card)
    }

    stripeCustomerId.fold(newCustomer)(existingCustomer)
  }

  def authorizeAmount(paymentSourceId: String,
                      amount: Long,
                      currency: Currency,
                      customerId: Option[String]): Result[StripeCharge] = {
    import scala.collection.mutable

    val chargeMap: mutable.Map[String, AnyRef] = mutable.Map(
      "amount"   → amount.toString,
      "currency" → currency.toString,
      "source"   → paymentSourceId,
      "capture"  → (false: java.lang.Boolean)
    )

    // we must pass customer.id for cc and must not for Apple Pay
    customerId map (cid ⇒ chargeMap += ("customer" → cid))

    stripe.createCharge(chargeMap.toMap)
  }

  def captureCharge(chargeId: String, amount: Long): Result[StripeCharge] =
    stripe.captureCharge(chargeId, Map[String, Object]("amount" → amount.toString))

  def authorizeRefund(chargeId: String, amount: Long, reason: RefundReason): Result[StripeCharge] =
    stripe.refundCharge(chargeId, Map[String, Object]("amount" → amount.toString, "reason" → reason.apiValue))

  def editCard(cc: CreditCard): Result[StripeCard] = {

    def update(stripeCard: StripeCard): Result[StripeCard] = {

      val params = Map[String, Object](
        "address_line1" → cc.address.address1,
        "address_line2" → cc.address.address2,
        // ("address_state" → cc.region),
        "address_zip"  → cc.address.zip,
        "address_city" → cc.address.city,
        "name"         → cc.address.name,
        "exp_year"     → cc.expYear.toString,
        "exp_month"    → cc.expMonth.toString
      )

      stripe.updateCard(stripeCard, params)
    }

    for {
      stripeCard ← getCard(cc.gatewayCustomerId, cc.gatewayCardId)
      updated    ← update(stripeCard)
    } yield updated
  }

  private def getCard(gatewayCustomerId: String, gatewayCardId: String): Result[StripeCard] =
    stripe.findCardByCustomerId(gatewayCustomerId, gatewayCardId)

  def deleteCard(cc: CreditCard): Result[DeletedCard] =
    for {
      stripeCard ← getCard(cc.gatewayCustomerId, cc.gatewayCardId)
      updated    ← stripe.deleteCard(stripeCard)
    } yield updated

  def retrieveToken(t: String): Result[StripeToken] =
    stripe.retrieveToken(t)
}
