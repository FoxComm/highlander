package utils.apis

import com.stripe.model.{DeletedExternalAccount, ExternalAccount, Charge ⇒ StripeCharge, Customer ⇒ StripeCustomer}
import services._
import utils.aliases.stripe._

/**
  * Low-level wrapper for Stripe API
  */
trait StripeApiWrapper {

  def findCustomer(id: String): Result[StripeCustomer]

  def findDefaultCard(customer: StripeCustomer): Result[StripeCard]

  def createCustomer(options: Map[String, AnyRef]): Result[StripeCustomer]

  def createCharge(options: Map[String, AnyRef]): Result[StripeCharge]

  def getCharge(chargeId: String): Result[StripeCharge]

  def captureCharge(chargeId: String, options: Map[String, AnyRef]): Result[StripeCharge]

  def createCard(customer: StripeCustomer, options: Map[String, AnyRef]): Result[StripeCard]

  def getExtAccount(customer: StripeCustomer, id: String): Result[ExternalAccount]

  def updateExternalAccount(card: ExternalAccount,
                            options: Map[String, AnyRef]): Result[ExternalAccount]

  def deleteExternalAccount(card: ExternalAccount): Result[DeletedExternalAccount]
}
