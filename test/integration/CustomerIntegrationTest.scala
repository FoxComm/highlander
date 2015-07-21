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
      val states = Table(
        ("action", "disabled"),
        ("disable", true),
        ("enable", false)
      )

      customer.disabled must === (false)

      forAll(states) { (action, disabled) ⇒
        val response = PATCH(s"v1/users/${customer.id}/$action")
        response.status must === (StatusCodes.OK)

        val c = parse(response.bodyText).extract[Customer]
        c.disabled must === (disabled)
      }
    }

    "toggles the isDefault flag on a credit card" in new Fixture {
      val creditCard = CreditCards.save(Factories.creditCard.copy(isDefault = true, customerId = customer.id)).run()
        .futureValue

      val states = Table(
        "isDefault",
        false,
        true,
        false
      )

      forAll(states) { isDefault ⇒
        val payload = payloads.UpdateCreditCard(isDefault = isDefault)
        val response = PATCH(s"v1/users/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        response.status must === (StatusCodes.OK)

        val cc = parse(response.bodyText).extract[CreditCard]
        cc.isDefault must === (isDefault)
      }
    }

    "fails to set the credit card as default if a default currently exists" in new Fixture {
      val default = CreditCards.save(Factories.creditCard.copy(isDefault = true, customerId = customer.id))
        .run().futureValue
      val nonDefault = CreditCards.save(Factories.creditCard.copy(isDefault = false, customerId = customer.id))
        .run().futureValue

      val payload = payloads.UpdateCreditCard(isDefault = true)
      val response = PATCH(s"v1/users/${customer.id}/payment-methods/credit-cards/${nonDefault.id}", payload)
      response.status must ===(StatusCodes.BadRequest)

      val cc = parse(response.bodyText).extract[CreditCard]
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).run().futureValue
    val admin = StoreAdmins.save(authedStoreAdmin).run().futureValue
  }
}

