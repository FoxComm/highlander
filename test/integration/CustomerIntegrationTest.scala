import akka.http.scaladsl.model.StatusCodes

import algebra.Monoid
import cats.data.Xor
import com.stripe.exception.CardException
import com.stripe.model
import com.stripe.model.{Customer ⇒ StripeCustomer, Card, ExternalAccount}
import models.{Orders, Customer, CreditCards, CreditCard, Customers, Addresses, StoreAdmins, OrderPayments,
Region, Regions}
import models.OrderPayments.scope._
import responses.CustomerResponse
import org.mockito.Mockito
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import payloads.CreateAddressPayload
import services.{GeneralFailure, CannotUseInactiveCreditCard, CreditCardManager, NotFoundFailure}
import services.{StripeRuntimeException, GeneralFailure, Result, CannotUseInactiveCreditCard, CreditCardManager,
NotFoundFailure}
import util.IntegrationTestBase
import utils.jdbc._
import utils.Seeds.Factories
import utils.Slick.implicits._
import cats.implicits._
import utils.{Apis, StripeApi}
import org.mockito.Mockito.{ when }
import org.mockito.{ Matchers ⇒ m }
import org.mockito.Mockito.reset

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with MockitoSugar {

  import concurrent.ExecutionContext.Implicits.global

  override def makeApis: Option[Apis] = Some(Apis(stripeApi))
  private  var stripeApi: StripeApi   = mock[StripeApi]

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  import slick.driver.PostgresDriver.api._
  import util.SlickSupport.implicits._

  def customersApi(customer: Customer, shipRegion: Option[Region] = None, billRegion: Option[Region] = None) = {
    info("show a customer")
    val response = GET(s"v1/customers/${customer.id}")
    val customerRoot = CustomerResponse.build(customer, shipRegion = shipRegion, billRegion = billRegion)

    response.status must ===(StatusCodes.OK)
    response.as[CustomerResponse.Root] must === (customerRoot)

    info("shows a list of customers")
    val responseList = GET(s"v1/customers")
    val customers = Seq(customerRoot)

    responseList.status must === (StatusCodes.OK)
    responseList.as[Seq[CustomerResponse.Root]] must === (customers)
  }

  "Customer" - {
    "accounts are unique based on email, non-guest, and active" in {
      val stub = Factories.customer.copy(isGuest = false, disabled = false)
      Customers.save(stub).futureValue
      val failure = GeneralFailure("record was not unique")
      val xor = withUniqueConstraint(Customers.save(stub).run())(_ ⇒ failure).futureValue

      leftValue(xor) must ===(failure)
    }

    "accounts are NOT unique for guest account and email" in {
      val stub = Factories.customer.copy(isGuest = true)
      val customers = (1 to 3).map(_ ⇒ Customers.save(stub).futureValue)
      customers.map(_.id) must contain allOf(1,2,3)
    }
  }

  "admin APIs" - {
    "should with Fixture" in new Fixture {
      behave like customersApi(customer, region)
    }
    "should with no default address" in new FixtureWithoutDefaultAddress {
      behave like customersApi(customer, region)
    }
    "should with creditCard" in new CreditCardFixture {
      val billRegion = Regions.findById(creditCard.regionId).run().futureValue
      behave like customersApi(customer, region, billRegion)
    }
    "should with no default creditCard" in new NoDefaultCreditCardFixture {
      val billRegion = Regions.findById(creditCard.regionId).run().futureValue
      behave like customersApi(customer, region, billRegion)
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
          val deleted = CreditCards.findById(creditCard.id).run().futureValue.value

          response.status must ===(StatusCodes.NoContent)
          deleted.inWallet must === (false)
          deleted.deletedAt mustBe 'defined
        }

      }

      "when editing a credit card" - {
        "when successful" - {
          "removes the original card from wallet" in new CreditCardFixture {
            reset(stripeApi)

            when(stripeApi.findCustomer(m.any(), m.any())).
              thenReturn(Result.good(new StripeCustomer))

            when(stripeApi.findDefaultCard(m.any(), m.any())).
              thenReturn(Result.good(new Card))

            when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
              thenReturn(Result.good(new Card))

            val payload = payloads.EditCreditCard(holderName = Some("Bob"))
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val inactive = CreditCards.findById(creditCard.id).run().futureValue.value

            response.status must ===(StatusCodes.NoContent)

          }

          "creates a new version of the edited card in the wallet" in new CreditCardFixture {
            reset(stripeApi)

            when(stripeApi.findCustomer(m.any(), m.any())).
              thenReturn(Result.good(new StripeCustomer))

            when(stripeApi.findDefaultCard(m.any(), m.any())).
              thenReturn(Result.good(new Card))

            when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
              thenReturn(Result.good(new Card))

            val payload  = payloads.EditCreditCard(holderName = Some("Bob"))
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

            response.status must ===(StatusCodes.NoContent)
            newVersion.inWallet mustBe true
            newVersion.isDefault must ===(creditCard.isDefault)
          }

          "updates the customer's cart to use the new version" in new CreditCardFixture {
            reset(stripeApi)

            when(stripeApi.findCustomer(m.any(), m.any())).
              thenReturn(Result.good(new StripeCustomer))

            when(stripeApi.findDefaultCard(m.any(), m.any())).
              thenReturn(Result.good(new Card))

            when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
              thenReturn(Result.good(mock[Card]))

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

          "copies an existing address book entry to the creditCard" in new CreditCardFixture {
            val payload = payloads.EditCreditCard(holderName = Some("Bob"), addressId = address.id.some)
            val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
            val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
            val numAddresses = Addresses.length.result.futureValue

            response.status must ===(StatusCodes.NoContent)
            numAddresses must === (1)
            (newVersion.zip, newVersion.regionId) must ===((address.zip, address.regionId))
          }

          "creates a new address book entry if a full address was given" in new CreditCardFixture {
            reset(stripeApi)

            when(stripeApi.findCustomer(m.any(), m.any())).
              thenReturn(Result.good(new StripeCustomer))

            when(stripeApi.findDefaultCard(m.any(), m.any())).
              thenReturn(Result.good(new Card))

            when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
              thenReturn(Result.good(mock[Card]))

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

        "fails if stripe returns an error" in new CreditCardFixture {
          reset(stripeApi)

          when(stripeApi.findCustomer(m.any(), m.any())).
            thenReturn(Result.good(new StripeCustomer))

          when(stripeApi.findDefaultCard(m.any(), m.any())).
            thenReturn(Result.good(new Card))

          when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
            thenReturn(Result.failure(
              StripeRuntimeException(
                new CardException(
                  "Your card's expiration year is invalid",
                  "invalid_expiry_year",
                  "exp_year", null, null, null))))

          val payload = payloads.EditCreditCard(expYear = Some(2000))
          val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

          response.status must === (StatusCodes.BadRequest)
          response.errors must === (List("Your card's expiration year is invalid"))
        }
      }
    }
  }


  trait Fixture {
    val (customer, address, region, admin) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      region ← Regions.findById(address.regionId)
      admin ← StoreAdmins.save(authedStoreAdmin)
    } yield (customer, address, region, admin)).run().futureValue
  }

  trait FixtureWithoutDefaultAddress extends Fixture {
    override val address = Addresses.save(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      .run()
      .futureValue
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = CreditCards.save(Factories.creditCard.copy(customerId = customer.id)).run().futureValue
  }

  trait NoDefaultCreditCardFixture extends CreditCardFixture {
    override val creditCard = CreditCards.save(Factories.creditCard.copy(
      isDefault = false,
      customerId = customer.id)).run().futureValue
  }
}

