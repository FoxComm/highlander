package utils

import java.time.ZonedDateTime

import scala.collection.JavaConversions._
import com.stripe.model.{DeletedCustomer, Token}
import faker.Lorem
import models.location.Address
import services.Result
import utils.aliases.stripe.StripeCustomer
import utils.apis.StripeWrapper
import utils.seeds.Seeds.Factories

object TestStripeSupport {

  val stripe = new StripeWrapper()

  // "fox" suffix is to indicate its ours
  def randomStripeishId: String = Lorem.bothify("?#?#?#?####?#?#???_fox")

  def createTokenForCard(cardNumber: String): Result[Token] =
    createToken(cardNumber = cardNumber,
                expYear = ZonedDateTime.now.getYear + 3,
                expMonth = 5,
                cvv = 123,
                address = Factories.address)

  // This is intended only to use within tests to mimic stripe.js behavior
  def createToken(cardNumber: String,
                  expYear: Int,
                  expMonth: Int,
                  cvv: Int,
                  address: Address): Result[Token] = {
    val card = Map[String, Any]("number" → cardNumber,
                                "exp_month"     → expMonth,
                                "exp_year"      → expYear,
                                "cvc"           → cvv,
                                "name"          → address.name,
                                "address_line1" → address.address1,
                                "address_line2" → address.address2.orNull,
                                "address_city"  → address.city,
                                "address_zip"   → address.zip)

    stripe.inBlockingPool(Token.create(Map("card" → mapAsJavaMap(card))))
  }

  def getCustomer(id: String): Result[StripeCustomer] = {
    stripe.findCustomer(id)
  }

  def deleteCustomer(customer: StripeCustomer): Result[DeletedCustomer] = {
    stripe.inBlockingPool(customer.delete)
  }

  // https://stripe.com/docs/testing#cards
  private[this] val successfulCards = Map(
    "Visa"                 → "4242424242424242",
    "Visa (debit)"         → "4000056655665556",
    "MasterCard"           → "5555555555554444",
    "MasterCard (debit)"   → "5200828282828210",
    "MasterCard (prepaid)" → "5105105105105100",
    "American Express"     → "378282246310005",
    "Discover"             → "6011111111111117",
    "Diners Club"          → "30569309025904",
    "JCB"                  → "3530111333300000"
  )

  // https://stripe.com/docs/testing#cards
  private[this] val failureCards = Map(
    "card_declined"      → "4000000000000002",
    "incorrect_number"   → "4242424242424241", // fails the Luhn check
    "expired_card"       → "4000000000000069",
    "incorrect_cvc_code" → "4000000000000127"
  )

  def successfulCard: String =
    this.successfulCards("Visa")

  def declinedCard: String =
    this.failureCards("card_declined")

  def incorrectNumberCard: String =
    this.failureCards("incorrect_number")

  def incorrectCvc: String =
    this.failureCards("incorrect_cvc_code")
}
