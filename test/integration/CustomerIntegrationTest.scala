import java.time.Instant

import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import com.stripe.exception.CardException
import com.stripe.model.{Card, Customer ⇒ StripeCustomer}
import models.OrderPayments.scope._
import models.{Order, Addresses, CreditCard, CreditCards, Customer, Customers, OrderPayments, Orders, Regions,
StoreAdmins}
import org.mockito.Mockito.{reset, when}
import org.mockito.{Matchers ⇒ m}
import org.scalatest.mock.MockitoSugar
import payloads.CreateAddressPayload

import responses.{ResponseWithFailuresAndMetadata, CustomerResponse}
import services.orders.OrderPaymentUpdater
import services.{CannotUseInactiveCreditCard, CreditCardManager, GeneralFailure, NotFoundFailure404,
Result, StripeRuntimeException, CustomerEmailNotUnique}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.jdbc._
import utils.{Apis, Seeds, StripeApi}

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with SortingAndPaging[CustomerResponse.Root]
  with MockitoSugar {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import slick.driver.PostgresDriver.api._
  import util.SlickSupport.implicits._

  // paging and sorting API
  val uriPrefix = "v1/customers"

  def responseItems = {
    val items = (1 to numOfResults).map { i ⇒
      val dbio = Customers.save(Seeds.Factories.generateCustomer)

      dbio.map { CustomerResponse.build(_) }
    }

    DBIO.sequence(items).transactionally.run().futureValue
  }
  val sortColumnName = "name"

  def responseItemsSort(items: IndexedSeq[CustomerResponse.Root]) = items.sortBy(_.name)

  def mf = implicitly[scala.reflect.Manifest[CustomerResponse.Root]]
  // paging and sorting API end

  override def makeApis: Option[Apis] = Some(Apis(stripeApi))

  private val stripeApi: StripeApi = mock[StripeApi]

  "Customer" - {
    "accounts are unique based on email, non-guest, and active" in {
      val stub = Factories.customer.copy(isGuest = false, isDisabled = false)
      Customers.save(stub).futureValue
      val failure = GeneralFailure("record was not unique")
      val xor = withUniqueConstraint(Customers.save(stub).run())(_ ⇒ failure).futureValue

      leftValue(xor) must === (failure)
    }

    "accounts are NOT unique for guest account and email" in {
      val stub = Factories.customer.copy(isGuest = true)
      val customers = (1 to 3).map(_ ⇒ Customers.save(stub).futureValue)
      customers.map(_.id) must contain allOf(1, 2, 3)
    }
  }

  "GET /v1/customers" - {
    "lists customers" in new Fixture {
      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root#ResponseSeq].result must === (Seq(customerRoot))
    }

    "lists customers without default address" in new Fixture {
      Addresses.filter(_.id === address.id).map(_.isDefaultShipping).update(false).run().futureValue
      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root#ResponseSeq].result must === (Seq(customerRoot))
    }

    "customer listing shows valid billingRegion" in new CreditCardFixture {
      val billRegion = Regions.findOneById(creditCard.regionId).run().futureValue

      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region, billingRegion = billRegion)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root#ResponseSeq].result must === (Seq(customerRoot))
    }

    "customer listing shows valid billingRegion without default CreditCard" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).run().futureValue
      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root#ResponseSeq].result must === (Seq(customerRoot))
    }
  }

  "POST /v1/customers" - {
    "successfully creates customer from payload" in new Fixture {
      val response = POST(s"v1/customers", payloads.CreateCustomerPayload(email = "test@example.com",
        name = Some("test")))

      response.status must ===(StatusCodes.OK)

      val root = response.as[CustomerResponse.Root]
      val created = Customers.findOneById(root.id).run().futureValue.value
      created.id must === (root.id)
    }

    "fails if email is already in use" in new Fixture {
      val response = POST(s"v1/customers", payloads.CreateCustomerPayload(email = customer.email,
        name = Some("test")))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(CustomerEmailNotUnique.description)
    }
  }

  "GET /v1/customers/:customerId" - {
    "fetches customer info" in new Fixture {
      val response = GET(s"$uriPrefix/${customer.id}")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "fetches customer info without default address" in new Fixture {
      Addresses.filter(_.id === address.id).map(_.isDefaultShipping).update(false).run().futureValue
      val response = GET(s"$uriPrefix/${customer.id}")
      val customerRoot = CustomerResponse.build(customer)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion" in new CreditCardFixture {
      val billRegion = Regions.findOneById(creditCard.regionId).run().futureValue

      val response = GET(s"$uriPrefix/${customer.id}")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region, billingRegion = billRegion)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion without default CreditCard" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).run().futureValue
      val response = GET(s"$uriPrefix/${customer.id}")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }
  }

  "PATCH /v1/customers/:customerId" - {
    "successfully updates customer attributes" in new Fixture {
      val payload = payloads.UpdateCustomerPayload(name = "John Doe".some, email = "newemail@example.org".some,
        phoneNumber = "555 555 55".some)
      val newEmail = payload.email.getOrElse("")
      (payload.name, newEmail, payload.phoneNumber) must !== ((customer.name, customer.email, customer
        .phoneNumber))

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      val updated = response.as[responses.CustomerResponse.Root]
      (updated.name, updated.email, updated.phoneNumber) must === ((payload.name, newEmail, payload
        .phoneNumber))
    }

    "fails if email is already in use" in new Fixture {
      val newUserResponse = POST(s"v1/customers", payloads.CreateCustomerPayload(email = "test@example.com",
        name = Some("test")))

      newUserResponse.status must ===(StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload = payloads.UpdateCustomerPayload(email = customer.email.some)
      val response = PATCH(s"v1/customers/${root.id}", payload)

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(CustomerEmailNotUnique.description)
    }
  }

  "POST /v1/customers/:customerId/activate" - {
    "fails if email is already in use by non-guest user" in new Fixture {
      val newUserResponse = POST(s"v1/customers", payloads.CreateCustomerPayload(email = customer.email,
        isGuest = Some(true)))

      newUserResponse.status must ===(StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload = payloads.ActivateCustomerPayload(name = "test")
      val response = POST(s"v1/customers/${root.id}/activate", payload)

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(CustomerEmailNotUnique.description)
    }

    "sucessfully activate non-guest user" in new Fixture {
      val newUserResponse = POST(s"v1/customers", payloads.CreateCustomerPayload(email = "guest@example.com",
        isGuest = Some(true)))

      newUserResponse.status must ===(StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload = payloads.ActivateCustomerPayload(name = "test")
      val response = POST(s"v1/customers/${root.id}/activate", payload)
      response.status must === (StatusCodes.OK)

      val created = Customers.findOneById(root.id).run().futureValue.value
      CustomerResponse.build(created) must === (root.copy(name = Some("test")))
      created.isGuest must === (false)
    }
  }

  "POST /v1/customers/:customerId/disable" - {
    "toggles the isDisabled flag on a customer account" in new Fixture {
      customer.isDisabled must === (false)

      val response = POST(s"$uriPrefix/${customer.id}/disable", payloads.ToggleCustomerDisabled(true))
      response.status must === (StatusCodes.OK)

      response.as[Customer].isDisabled must === (true)
    }
  }

  "GET /v1/customers/:customerId/payment-methods/credit-cards" - {
    "shows customer's credit cards only in their wallet" in new CreditCardFixture {
      val deleted = CreditCards.save(creditCard.copy(id = 0, inWallet = false)).run().futureValue

      val response = GET(s"$uriPrefix/${customer.id}/payment-methods/credit-cards")
      val cc = response.as[ResponseWithFailuresAndMetadata[Seq[CreditCard]]].result

      response.status must === (StatusCodes.OK)
      cc must have size 1
      cc.head must === (creditCard)
      cc.head.id must !== (deleted.id)
    }
  }

  "POST /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId/default" - {
    "sets the isDefault flag on a credit card" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).run().futureValue

      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}/default", payload)
      response.status must === (StatusCodes.OK)

      response.as[CreditCard].isDefault must === (true)
    }

    "fails to set the credit card as default if a default currently exists" in new Fixture {
      val default = CreditCards.save(Factories.creditCard.copy(isDefault = true, customerId = customer.id))
        .run().futureValue
      val nonDefault = CreditCards.save(Factories.creditCard.copy(isDefault = false, customerId = customer.id))
        .run().futureValue

      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${nonDefault.id}/default", payload)

      response.status must === (StatusCodes.BadRequest)
      response.bodyText must include("customer already has default credit card")
    }
  }

  "DELETE /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId" - {
    "deletes successfully if the card exists" in new CreditCardFixture {
      val response = DELETE(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}")
      val deleted = CreditCards.findOneById(creditCard.id).run().futureValue.value

      response.status must === (StatusCodes.NoContent)
      deleted.inWallet must === (false)
      deleted.deletedAt mustBe 'defined
    }
  }

  "PATCH /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId" - {
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
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val inactive = CreditCards.findOneById(creditCard.id).run().futureValue.value

        response.status must === (StatusCodes.NoContent)

      }

      "creates a new version of the edited card in the wallet" in new CreditCardFixture {
        reset(stripeApi)

        when(stripeApi.findCustomer(m.any(), m.any())).
          thenReturn(Result.good(new StripeCustomer))

        when(stripeApi.findDefaultCard(m.any(), m.any())).
          thenReturn(Result.good(new Card))

        when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
          thenReturn(Result.good(new Card))

        val payload = payloads.EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

        response.status must === (StatusCodes.NoContent)
        newVersion.inWallet mustBe true
        newVersion.isDefault must === (creditCard.isDefault)
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
        OrderPaymentUpdater.addCreditCard(order.refNum, creditCard.id).futureValue

        val payload = payloads.EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (pmt :: Nil) = OrderPayments.filter(_.orderId === order.id).creditCards.result.run().futureValue.toList
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

        response.status must === (StatusCodes.NoContent)
        pmt.amount mustBe 'empty
        pmt.isCreditCard mustBe true
        pmt.paymentMethodId must === (newVersion.id)
      }

      "copies an existing address book entry to the creditCard" in new CreditCardFixture {
        val payload = payloads.EditCreditCard(holderName = Some("Bob"), addressId = address.id.some)
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
        val numAddresses = Addresses.length.result.futureValue

        response.status must === (StatusCodes.NoContent)
        numAddresses must === (1)
        (newVersion.zip, newVersion.regionId) must === ((address.zip, address.regionId))
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
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
        val addresses = Addresses.futureValue
        val newAddress = addresses.last

        response.status must === (StatusCodes.NoContent)
        addresses must have size 2
        (newVersion.zip, newVersion.regionId) must === (("54321", address.regionId + 1))
        (newVersion.zip, newVersion.regionId) must === ((newAddress.zip, newAddress.regionId))
      }
    }

    "fails if the card cannot be found" in new CreditCardFixture {
      val payload = payloads.EditCreditCard
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/99", payload)

      response.status must === (StatusCodes.NotFound)
      response.errors must === (NotFoundFailure404(CreditCard, 99).description)
    }

    "fails if the card is not inWallet" in new CreditCardFixture {
      CreditCardManager.deleteCreditCard(customer.id, creditCard.id).futureValue
      val payload = payloads.EditCreditCard
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (CannotUseInactiveCreditCard(creditCard).description)
    }

    "fails if the payload is invalid" in new CreditCardFixture {
      val payload = payloads.EditCreditCard(holderName = "".some)
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

      response.status must === (StatusCodes.BadRequest)
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
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (List("Your card's expiration year is invalid"))
    }
  }

  trait Fixture {
    val (customer, address, region, admin) = (for {
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      region ← Regions.findOneById(address.regionId)
      admin ← StoreAdmins.save(authedStoreAdmin)
    } yield (customer, address, region, admin)).run().futureValue
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = CreditCards.save(Factories.creditCard.copy(customerId = customer.id)).run().futureValue
  }
}
