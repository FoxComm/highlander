import akka.http.scaladsl.model.StatusCodes

import models.{Orders, Customer, CreditCards, CreditCard, Customers, Addresses, StoreAdmins, OrderPayments}
import models.OrderPayments.scope._
import payloads.CreateAddressPayload
import services.{CannotUseInactiveCreditCard, CreditCardManager, NotFoundFailure}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import cats.implicits._

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  import slick.driver.PostgresDriver.api._
  import util.SlickSupport.implicits._

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
        val default = CreditCards.save(Factories.creditCard.copy(isDefault = true, customerId = customer.id))
          .run().futureValue
        val nonDefault = CreditCards.save(Factories.creditCard.copy(isDefault = false, customerId = customer.id))
          .run().futureValue

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
        "when successful" - {
          /* TODO: enable me when we've introduced Stripe mocking */
          "removes the original card from wallet" ignore new CreditCardFixture {
            val payload = payloads.EditCreditCard(holderName = Some("Bob"))
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val inactive = CreditCards.findById(creditCard.id).run().futureValue.get

            response.status must ===(StatusCodes.NoContent)

          }

          /* TODO: enable me when we've introduced Stripe mocking */
          "creates a new version of the edited card in the wallet" ignore new CreditCardFixture {
            val payload = payloads.EditCreditCard(holderName = Some("Bob"))
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

            response.status must ===(StatusCodes.NoContent)
            newVersion.inWallet mustBe true
            newVersion.isDefault must ===(creditCard.isDefault)
          }

          /* TODO: enable me when we've introduced Stripe mocking */
          "updates the customer's cart to use the new version" ignore new CreditCardFixture {
            val order = Orders.save(Factories.cart.copy(customerId = customer.id)).run().futureValue
            services.OrderPaymentUpdater.addCreditCard(order.refNum, creditCard.id).futureValue

            val payload = payloads.EditCreditCard(holderName = Some("Bob"))
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val (pmt :: Nil) = OrderPayments.filter(_.orderId === order.id).creditCards.result.run().futureValue.toList
            val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

            response.status must ===(StatusCodes.NoContent)
            pmt.amount mustBe 'empty
            pmt.isCreditCard mustBe true
            pmt.paymentMethodId must ===(newVersion.id)
          }

          /* TODO: enable me when we've introduced Stripe mocking */
          "copies an existing address book entry to the creditCard" ignore new CreditCardFixture {
            val payload = payloads.EditCreditCard(holderName = Some("Bob"), addressId = address.id.some)
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
            val numAddresses = Addresses.length.result.futureValue

            response.status must ===(StatusCodes.NoContent)
            numAddresses must === (1)
            (newVersion.zip, newVersion.regionId) must ===((address.zip, address.regionId))
          }

          /* TODO: enable me when we've introduced Stripe mocking */
          "creates a new address book entry if a full address was given" ignore new CreditCardFixture {
            val payload = payloads.EditCreditCard(holderName = Some("Bob"),
              address = CreateAddressPayload(name = "Home Office", regionId = address.regionId + 1,
                address1 = "3000 Coolio Dr", city = "Seattle", zip = "54321").some)
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
            val addresses = Addresses.futureValue
            val newAddress = addresses.last

            response.status must ===(StatusCodes.NoContent)
            addresses must have size(2)
            (newVersion.zip, newVersion.regionId) must ===(("54321", address.regionId + 1))
            (newVersion.zip, newVersion.regionId) must ===((newAddress.zip, newAddress.regionId))
          }
        }

        "fails if the card cannot be found" in new CreditCardFixture {
          val payload = payloads.EditCreditCard
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/99", payload)

          response.status must ===(StatusCodes.NotFound)
          response.errors must ===(NotFoundFailure(CreditCard, 99).description)
        }

        "fails if the card is not inWallet" in new CreditCardFixture {
          CreditCardManager.deleteCreditCard(customer.id, creditCard.id).futureValue
          val payload = payloads.EditCreditCard
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

          response.status must ===(StatusCodes.BadRequest)
          response.errors must ===(CannotUseInactiveCreditCard(creditCard).description)
        }

        "fails if the payload is invalid" in new CreditCardFixture {
          val payload = payloads.EditCreditCard(holderName = "".some)
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

          response.status must ===(StatusCodes.BadRequest)
          response.errors must contain("holderName must not be empty")
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
    val creditCard = CreditCards.save(Factories.creditCard.copy(customerId = customer.id)).run().futureValue
  }
}

