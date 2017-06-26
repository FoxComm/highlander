package services

import java.time.{Instant, ZoneId}
import cats.implicits._
import com.stripe.Stripe
import java.time.{Instant, ZoneId}
import phoenix.failures.CreditCardFailures.CardDeclined
import phoenix.utils.TestStripeSupport._
import phoenix.utils.apis._
import phoenix.utils.seeds.Factories
import com.stripe.model.{ApplePayDomain, Token}
import com.stripe.net.RequestOptions
import testutils._
import core.utils.Money.Currency.USD
import utils.RealStripeApi
import core.db._
import phoenix.server.Setup

class StripeTest extends IntegrationTestBase with RealStripeApi {
  val stripe = Setup.setupStripe()

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

  val (customer, card) = stripe
    .createCardFromToken(email = customerEmail.some,
                         token = token.getId,
                         stripeCustomerId = none,
                         address = theAddress)
    .gimme

  val realStripeCustomerId = customer.getId
  val realStripeCardId     = card.getId

  "Stripe" - {
    "authorizeAmount" - {
      "fails if the customerId doesn't exist" taggedAs External in {
        val result = stripe
          .authorizeAmount("BAD-CARD", 100, currency = USD, "BAD-CUSTOMER".some)
          .gimmeFailures

        result.getMessage must include("No such customer")
      }

      "successfully creates an authorization charge" taggedAs External in {
        val auth = stripe
          .authorizeAmount(realStripeCardId, 100, currency = USD, realStripeCustomerId.some)
          .gimme

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
          .gimmeFailures

        result.head must === (CardDeclined)
      }

      "fails if token does not exist" taggedAs External in {
        val result = stripe
          .createCardFromToken(email = "yax@yax.com".some,
                               token = "BAD-TOKEN",
                               stripeCustomerId = none,
                               address = theAddress)
          .gimmeFailures

        result.head.description must === ("No such token: BAD-TOKEN")
      }

      "successfully creates a card and new customer when given no customerId" taggedAs External in {

        customer.getDescription must === ("FoxCommerce")
        customer.getEmail must === (customerEmail)

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
        val result = stripe.captureCharge("BAD-CHARGE-ID", 100).gimmeFailures

        result.getMessage must include("No such charge")
      }

      "successfully captures a charge" taggedAs External in {
        val auth = stripe
          .authorizeAmount(realStripeCardId, 100, currency = USD, realStripeCustomerId.some)
          .gimme
        val capture = stripe.captureCharge(auth.getId, 75).gimme

        capture.getCaptured mustBe true
        capture.getPaid mustBe true
        capture.getAmount.toInt must === (100)
        capture.getAmountRefunded.toInt must === (25)
      }

    }

    "authorizeRefund" - {
      "fails if the charge was not found" taggedAs External in {
        val result = stripe
          .authorizeRefund("BAD-CHARGE-ID", 100, RefundReason.RequestedByCustomer)
          .gimmeFailures

        result.getMessage must include("No such charge")
      }

      "fails if the refund amount exceeds a charge" taggedAs External in {
        val auth = stripe
          .authorizeAmount(realStripeCardId, 100, currency = USD, realStripeCustomerId.some)
          .gimme
        stripe.captureCharge(auth.getId, 90).gimme

        val result =
          stripe.authorizeRefund(auth.getId, 91, RefundReason.RequestedByCustomer).gimmeFailures
        result.getMessage must === (
          "Refund amount ($0.91) is greater than unrefunded amount on charge ($0.90)")
      }

      "successfully partially refunds a charge" taggedAs External in {
        val auth = stripe
          .authorizeAmount(realStripeCardId, 100, currency = USD, realStripeCustomerId.some)
          .gimme
        stripe.captureCharge(auth.getId, 100).gimme

        val refunded1 =
          stripe.authorizeRefund(auth.getId, 30, RefundReason.RequestedByCustomer).gimme
        refunded1.getAmount.toInt must === (100)
        refunded1.getAmountRefunded.toInt must === (30)

        val refunded2 =
          stripe.authorizeRefund(auth.getId, 50, RefundReason.RequestedByCustomer).gimme
        refunded2.getAmount.toInt must === (100)
        refunded2.getAmountRefunded.toInt must === (80)
      }

      "successfully refunds entire charge" taggedAs External in {
        val auth = stripe
          .authorizeAmount(realStripeCardId, 100, currency = USD, realStripeCustomerId.some)
          .gimme

        val refunded =
          stripe.authorizeRefund(auth.getId, 100, RefundReason.RequestedByCustomer).gimme
        refunded.getAmount.toInt must === (100)
        refunded.getAmountRefunded.toInt must === (100)
      }
    }

    "deleteCustomer" - {
      "successfully deletes a customer" taggedAs External in {
        deleteCustomer(customer).void.gimme
        getCustomer(realStripeCustomerId).gimme.getDeleted must === (Boolean.box(true))
      }
    }

    "Test Apple Pay APIs" - {
      import scala.collection.JavaConversions._

      "Stripe API should be able to provide allowed domains for Apple Pay" in {
        val domains = Map[String, AnyRef]("domain_name" → "stage-tpg.foxcommerce.com")
        ApplePayDomain.create(mapAsJavaMap(domains))
      }

      "Random token should fail" in {
        val randomToken = stripe.retrieveToken("random").gimmeFailures
        randomToken.head.description must === ("No such token: random")
      }

      "Retrieve token and make sure it's valid" in {
        val token: Token = createToken(cardNumber = successfulCard,
                                       cvv = 123,
                                       expYear = okExpYear,
                                       expMonth = okExpMonth,
                                       address = theAddress).gimme

        stripe.retrieveToken(token.getId).gimme.getId must === (token.getId)
      }
    }
  }
}
