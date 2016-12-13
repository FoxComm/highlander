package services

import java.time.{Instant, ZoneId}

import cats.implicits._
import com.stripe.Stripe
import failures.CreditCardFailures.CardDeclined
import testutils._
import utils.Money.Currency.USD
import utils.TestStripeSupport._
import utils.apis._
import utils.seeds.Seeds.Factories

trait RealStripeApis extends IntegrationTestBase {
  // Mutate Stripe state, set real key
  Stripe.apiKey = config.getString("stripe.key")
}

// Test that actually calls Stripe
// Other integration tests should mock Stripe API and check that some method has been called on a mock
// !!! Do not mix MockedApis in here
class StripeTest extends RealStripeApis {

  val stripe = new FoxStripe(new StripeWrapper())

  import Tags._

  val today      = Instant.now().atZone(ZoneId.of("UTC"))
  val okExpYear  = today.getYear + 1
  val okExpMonth = today.getMonthValue
  val theAddress = Factories.address

  val token = createToken(cardNumber = successfulCard,
                          expYear = okExpYear,
                          expMonth = okExpMonth,
                          cvv = 123,
                          address = theAddress).gimme

  val customerEmail = faker.Internet.email

  val (cust, card) = stripe
    .createCardFromToken(email = customerEmail.some,
                         token = token.getId,
                         stripeCustomerId = none,
                         address = theAddress)
    .gimme

  val realStripeCustomerId = cust.getId

  "Stripe" - {
    "authorizeAmount" - {
      "fails if the customerId doesn't exist" taggedAs External in {
        val result = stripe.authorizeAmount("BAD-CUSTOMER", 100, currency = USD).futureValue

        result.leftVal.getMessage must include("No such customer")
      }

      "successfully creates an authorization charge" taggedAs External in {
        val auth = stripe.authorizeAmount(realStripeCustomerId, 100, currency = USD).gimme

        auth.getAmount.toInt must === (100)
        auth.getCurrency.toUpperCase must === (USD.getCode)
        auth.getStatus must === ("succeeded")
        auth.getPaid mustBe true
        auth.getCaptured mustBe false
        Option(auth.getFailureCode) mustBe 'empty
        auth.getAmountRefunded.toInt must === (0)
        auth.getCustomer must === (realStripeCustomerId)
      }
    }

    "createCard" - {
      "fails if the card is declined" taggedAs External in {
        val token = createTokenForCard(declinedCard).gimme

        val result = stripe
          .createCardFromToken(email = "yax@yax.com".some,
                               token = token.getId,
                               address = theAddress,
                               stripeCustomerId = realStripeCustomerId.some)
          .futureValue

        result.leftVal.head must === (CardDeclined)
      }

      "fails if token does not exist" taggedAs External in {
        val result = stripe
          .createCardFromToken(email = "yax@yax.com".some,
                               token = "BAD-TOKEN",
                               stripeCustomerId = none,
                               address = theAddress)
          .futureValue

        result.leftVal.head.description must === ("No such token: BAD-TOKEN")
      }

      "successfully creates a card and new customer when given no customerId" taggedAs External in {

        cust.getDescription must === ("FoxCommerce")
        cust.getEmail must === (customerEmail)

        card.getAddressLine1 must === (theAddress.address1)
        card.getAddressLine2 mustBe 'empty
        card.getAddressZip must === (theAddress.zip)
        card.getAddressCity must === (theAddress.city)
        card.getBrand must === ("Visa")
        card.getName must === (theAddress.name)
        card.getExpMonth.toInt must === (okExpMonth)
        card.getExpYear.toInt must === (okExpYear)
        card.getLast4 must === (successfulCard.takeRight(4))
        card.getCountry must === ("US")
      }

      "successfully creates a card using an existing customer given a customerId" taggedAs External in {
        val token = createToken(cardNumber = successfulCard,
                                cvv = 123,
                                expYear = okExpYear,
                                expMonth = okExpMonth,
                                address = theAddress).gimme

        val (cust, card) = stripe
          .createCardFromToken(email = "yax@yax.com".some,
                               token = token.getId,
                               stripeCustomerId = realStripeCustomerId.some,
                               address = theAddress)
          .gimme

        cust.getId must === (realStripeCustomerId)
        card.getAddressLine1 must === (theAddress.address1)
        card.getAddressLine2 mustBe 'empty
        card.getAddressZip must === (theAddress.zip)
        card.getAddressCity must === (theAddress.city)
        card.getBrand must === ("Visa")
        card.getName must === (theAddress.name)
        card.getExpMonth.toInt must === (okExpMonth)
        card.getExpYear.toInt must === (okExpYear)
        card.getLast4 must === (successfulCard.takeRight(4))
        card.getCountry must === ("US")
      }
    }

    "captureCharge" - {
      "fails if the charge was not found" taggedAs External in {
        val result = stripe.captureCharge("BAD-CHARGE-ID", 100).futureValue

        result.leftVal.getMessage must include("No such charge")
      }

      "successfully captures a charge" taggedAs External in {
        val auth    = stripe.authorizeAmount(realStripeCustomerId, 100, currency = USD).gimme
        val capture = stripe.captureCharge(auth.getId, 75).gimme

        capture.getCaptured mustBe true
        capture.getPaid mustBe true
        capture.getAmount.toInt must === (100)
        capture.getAmountRefunded.toInt must === (25)
      }
    }

    "deleteCustomer" - {
      "successfully deletes a customer" taggedAs External in {
        val result = deleteCustomer(cust)

        getCustomer(realStripeCustomerId).value must === (None)
      }
    }
  }
}
