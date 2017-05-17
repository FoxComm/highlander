package utils.apis

import com.stripe.model.{DeletedCard, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import services._
import utils.aliases.stripe._
import utils.db._

/**
  * Low-level wrapper for Stripe API
  */
trait StripeApiWrapper {

  def findCustomer(id: String): Result[StripeCustomer]

  def findCardByCustomerId(gatewayCustomerId: String, gatewayCardId: String): Result[StripeCard]

  def createCustomer(options: Map[String, AnyRef]): Result[StripeCustomer]

  def createCharge(options: Map[String, AnyRef]): Result[StripeCharge]

  def getCharge(chargeId: String): Result[StripeCharge]

  def captureCharge(chargeId: String, options: Map[String, AnyRef]): Result[StripeCharge]

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef]): Result[StripeCard]

  def updateCard(card: StripeCard, options: Map[String, AnyRef]): Result[StripeCard]

  def deleteCard(card: StripeCard): Result[DeletedCard]
}
