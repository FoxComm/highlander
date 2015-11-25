package services

import java.time.{ZoneId, Instant}

import services.CreditCardFailure.{IncorrectCvc, CardDeclined}
import util.{StripeSupport, IntegrationTestBase}
import utils.{WiredStripeApi, Apis}
import utils.Money.Currency
import utils.Seeds.Factories
import utils.Slick.implicits._
import cats.implicits._
import slick.driver.PostgresDriver.api._

class StripeTest
  extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global
  import Tags._

  implicit val apis: Apis = Apis(new WiredStripeApi)
  val service = Stripe()
  val today = Instant.now().atZone(ZoneId.of("UTC"))

  // Re-use this existing customer so we don't have to create new customers for every test
  val existingCustId: String = "cus_7Ktq659oRPXB1U"

  "Stripe" - {
    "authorizeAmount" - {
      "fails if the customerId doesn't exist" taggedAs External in {
        val result = service.authorizeAmount("BAD-CUSTOMER", 100, currency = Currency.USD).futureValue

        result.leftVal.getMessage must include("No such customer")
      }

      "successfully creates an authorization charge" taggedAs External in {
        val result = service.authorizeAmount(existingCustId, 100, currency = Currency.USD).futureValue
        val charge = result.rightVal

        charge.getAmount.toInt must === (100)
        charge.getCurrency.toUpperCase must === (Currency.USD.getCode)
        charge.getStatus must === ("succeeded")
        charge.getPaid mustBe true
        charge.getCaptured mustBe false
        charge.getFailureCode.some mustBe 'empty
        charge.getAmountRefunded.toInt must === (0)
        charge.getCustomer must === (existingCustId)
      }
    }

    "createCard" - {
      "fails if the card is declined" taggedAs External in {
        val payload = payloads.CreateCreditCard(holderName = "yax", number = StripeSupport.declinedCard,
          cvv = "123", expYear = today.getYear, expMonth = today.getMonthValue)
        val result = service.createCard("yax@yax.com", payload, none, Factories.address).futureValue

        result.leftVal.head must === (CardDeclined)
      }

      "fails if the cvc is incorrect" taggedAs External in {
        val payload = payloads.CreateCreditCard(holderName = "yax", number = StripeSupport.incorrectCvc,
          cvv = "123", expYear = today.getYear, expMonth = today.getMonthValue)
        val result = service.createCard("yax@yax.com", payload, none, Factories.address).futureValue

        result.leftVal.head must === (IncorrectCvc)
      }

      "successfully creates a card and new customer when given no customerId" taggedAs External in {
        val address = Factories.address
        val payload = payloads.CreateCreditCard(holderName = "yax", number = StripeSupport.successfulCard,
          cvv = "123", expYear = today.getYear, expMonth = today.getMonthValue)
        val result = service.createCard("yax@yax.com", payload, none, address).futureValue

        val (cust, card) = result.rightVal
        cust.getDescription must === ("FoxCommerce")
        cust.getEmail must === ("yax@yax.com")

        card.getAddressLine1 must === (address.address1)
        card.getAddressLine2 mustBe 'empty
        card.getAddressZip must === (address.zip)
        card.getAddressCity must === (address.city)
        card.getBrand must === ("Visa")
        card.getName must === ("yax")
        card.getExpMonth.toInt must === (today.getMonthValue)
        card.getExpYear.toInt must === (today.getYear)
        card.getLast4 must === (payload.lastFour)
        card.getCountry must === ("US")
      }

      "successfully creates a card using an existing customer given a customerId" taggedAs External in {
        val address = Factories.address
        val payload = payloads.CreateCreditCard(holderName = "yax", number = StripeSupport.successfulCard,
          cvv = "123", expYear = today.getYear, expMonth = today.getMonthValue)
        val result = service.createCard("yax@yax.com", payload, existingCustId.some, address).futureValue

        val (cust, card) = result.rightVal

        cust.getId must === (existingCustId)

        card.getAddressLine1 must === (address.address1)
        card.getAddressLine2 mustBe 'empty
        card.getAddressZip must === (address.zip)
        card.getAddressCity must === (address.city)
        card.getBrand must === ("Visa")
        card.getName must === ("yax")
        card.getExpMonth.toInt must === (today.getMonthValue)
        card.getExpYear.toInt must === (today.getYear)
        card.getLast4 must === (payload.lastFour)
        card.getCountry must === ("US")
      }
    }

    "captureCharge" - {
      "fails if the charge was not found" taggedAs External in {
        val result = service.captureCharge("BAD-CHARGE-ID", 100).futureValue

        result.leftVal.getMessage must include("No such charge")
      }

      "successfully captures a charge" taggedAs External in {
        val auth = service.authorizeAmount(existingCustId, 100, currency = Currency.USD).futureValue
        val capture = service.captureCharge(auth.rightVal.getId, 75).futureValue.rightVal

        capture.getCaptured mustBe true
        capture.getPaid mustBe true
        capture.getAmount.toInt must === (100)
        capture.getAmountRefunded.toInt must === (25)
      }
    }
  }
}
