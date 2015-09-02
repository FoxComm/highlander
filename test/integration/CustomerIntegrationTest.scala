import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import models._
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.prop.TableDrivenPropertyChecks._
import services.{CannotUseInactiveCreditCard, Failure, NotFoundFailure, CustomerManager}
import util.IntegrationTestBase
import utils.Seeds.Factories

import akka.http.scaladsl.model.{HttpResponse}

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import slick.driver.PostgresDriver.api._
  import utils._
  import concurrent.ExecutionContext.Implicits.global
  import org.json4s.jackson.JsonMethods._
  import Extensions._

  "admin APIs" - {
    "shows a customer" in new Fixture {
      val response = GET(s"v1/customers/${customer.id}")

      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Customer] must === (customer)
    }

    "shows a list of customers" in new Fixture {
      val response = GET(s"v1/customers")
      val customers = Seq(customer)

      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Seq[Customer]] must === (customers)
    }

    "toggles the disabled flag on a customer account" in new Fixture {
      customer.disabled must ===(false)

      val response = POST(s"v1/customers/${customer.id}/disable", payloads.ToggleCustomerDisabled(true))
      response.status must === (StatusCodes.OK)

      val c = parse(response.bodyText).extract[Customer]
      c.disabled must === (true)
    }

    "credit cards" - {
      "shows customer's credit cards only in their wallet" in new CreditCardFixture {
        val deleted = CreditCards.save(creditCard.copy(id = 0, inWallet = false)).run().futureValue

        val response = GET(s"v1/customers/${customer.id}/payment-methods/credit-cards")
        val cc = response.as[Seq[CreditCard]]

        response.status must ===(StatusCodes.OK)
        cc must have size(1)
        cc.head must ===(creditCard)
        cc.head.id must !== (deleted.id)
      }

      "sets the isDefault flag on a credit card" in new CreditCardFixture {
        CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).run().futureValue

        val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
        val response = POST(
          s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}/default",
          payload)
        response.status must ===(StatusCodes.OK)

        val cc = parse(response.bodyText).extract[CreditCard]
        cc.isDefault must ===(true)
      }

      "fails to set the credit card as default if a default currently exists" in new Fixture {
        val default = CreditCards.save(Factories.creditCard.copy(isDefault = true, customerId = customer.id,
          billingAddressId = address.id)).run().futureValue
        val nonDefault = CreditCards.save(Factories.creditCard.copy(isDefault = false, customerId = customer.id,
          billingAddressId = address.id)).run().futureValue

        val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
        val response = POST(
          s"v1/customers/${customer.id}/payment-methods/credit-cards/${nonDefault.id}/default",
          payload)

        response.status must ===(StatusCodes.BadRequest)
        response.bodyText must include("customer already has default credit card")
      }

      "when deleting a credit card" - {
        "succeeds if the card exists" in new CreditCardFixture {
          val response = DELETE(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}")
          val deleted = CreditCards.findById(creditCard.id).run().futureValue.get

          response.status must ===(StatusCodes.NoContent)
          deleted.inWallet must === (false)
          deleted.deletedAt mustBe 'defined
        }

      }

      "when editing a credit card" - {
        /* TODO: enable me when we've introduced Stripe mocking */
        "succeeds" ignore new CreditCardFixture {
          val payload = payloads.EditCreditCard(holderName = Some("Bob"))
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
          val inactive = CreditCards.findById(creditCard.id).run().futureValue.get
          val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

          response.status must ===(StatusCodes.NoContent)
          inactive.inWallet mustBe false
          newVersion.inWallet mustBe true
          newVersion.isDefault must ===(inactive.isDefault)
        }

        "fails if the card cannot be found" in new CreditCardFixture {
          val payload = payloads.EditCreditCard
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/99", payload)

          response.status must ===(StatusCodes.NotFound)
          response.errors must ===(NotFoundFailure(CreditCard, 99).description)
        }

        "fails if the card is not inWallet" in new CreditCardFixture {
          CustomerManager.deleteCreditCard(customer.id, creditCard.id).futureValue
          val payload = payloads.EditCreditCard
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

          response.status must ===(StatusCodes.BadRequest)
          response.errors must ===(CannotUseInactiveCreditCard(creditCard).description)
        }

        /* TODO: enable me when we've introduced Stripe mocking */
        "fails if stripe returns an error" ignore new CreditCardFixture {
          val payload = payloads.EditCreditCard(expYear = Some(2000))
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

          response.status must ===(StatusCodes.BadRequest)
          response.errors must ===(CannotUseInactiveCreditCard(creditCard).description)
        }
      }
    }
  }

  trait Fixture {
    val (customer, address, admin) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      admin ← StoreAdmins.save(authedStoreAdmin)
    } yield (customer, address, admin)).run().futureValue
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = CreditCards.save(Factories.creditCard.copy(customerId = customer.id,
      billingAddressId = address.id)).run().futureValue
  }
}

