import akka.http.scaladsl.model.StatusCodes
import models._
import org.scalatest.prop.TableDrivenPropertyChecks._
import util.IntegrationTestBase
import utils.Seeds.Factories

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
        "succeeds if the card exists" in new Fixture {
          val creditCard = CreditCards.save(Factories.creditCard.copy(customerId = customer.id,
            billingAddressId = address.id)).run().futureValue
          val response = DELETE(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}")
          val deleted = CreditCards.findById(creditCard.id).run().futureValue.get

          response.status must ===(StatusCodes.NoContent)
          deleted.inWallet must === (false)
          deleted.deletedAt mustBe 'defined
        }

        "fails if the card cannot be found" in new Fixture {
          val response = DELETE(s"v1/customers/${customer.id}/payment-methods/credit-cards/99")
          response.status must ===(StatusCodes.NotFound)
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
    val creditCard = (for {
      cc ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id, billingAddressId = address.id))
    } yield cc).run().futureValue
  }
}

