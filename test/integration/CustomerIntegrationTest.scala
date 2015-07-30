import akka.http.scaladsl.model.StatusCodes
import models._
import org.scalatest.prop.TableDrivenPropertyChecks._
import util.IntegrationTestBase
import utils.Seeds.Factories

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import org.json4s.jackson.JsonMethods._
  import Extensions._

  "admin APIs" - {
    "shows a customer" in new Fixture {
      val response = GET(s"v1/users/${customer.id}")

      response.status must === (StatusCodes.OK)
      parse(response.bodyText).extract[Customer] must === (customer)
    }

    "toggles the disabled flag on a customer account" in new Fixture {
      customer.disabled must ===(false)

      val response = POST(s"v1/users/${customer.id}/disable", payloads.ToggleCustomerDisabled(true))
      response.status must === (StatusCodes.OK)

      val c = parse(response.bodyText).extract[Customer]
      c.disabled must === (true)
    }

    "sets the isDefault flag on a credit card" in new Fixture {
      val creditCard = CreditCards.save(Factories.creditCard.copy(isDefault = false,
        customerId = customer.id, billingAddressId = address.id)).run().futureValue

      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(
        s"v1/users/${customer.id}/payment-methods/credit-cards/${creditCard.id}/default",
        payload)
      response.status must ===(StatusCodes.OK)

      val cc = parse(response.bodyText).extract[CreditCard]
      cc.isDefault must === (true)
    }

    "fails to set the credit card as default if a default currently exists" in new Fixture {
      val default = CreditCards.save(Factories.creditCard.copy(isDefault = true, customerId = customer.id,
        billingAddressId = address.id)).run().futureValue
      val nonDefault = CreditCards.save(Factories.creditCard.copy(isDefault = false, customerId = customer.id,
        billingAddressId = address.id)).run().futureValue

      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(
        s"v1/users/${customer.id}/payment-methods/credit-cards/${nonDefault.id}/default",
        payload)

      response.status must ===(StatusCodes.BadRequest)
      response.bodyText must include("customer already has default credit card")
    }
  }

  trait Fixture {
    val (customer, address, admin) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      admin ← StoreAdmins.save(authedStoreAdmin)
    } yield (customer, address, admin)).run().futureValue
  }
}

