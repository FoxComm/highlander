import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import com.stripe.exception.CardException
import models.order._
import OrderPayments.scope._
import models.activity.ActivityContext
import models.customer._
import models.location.{Addresses, Regions}
import models.order.{Order, Orders}
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.rma.{Rma, Rmas}
import models.stripe._
import models.StoreAdmins
import models.traits.Originator
import org.mockito.Mockito.{reset, when}
import org.mockito.{Matchers ⇒ m}
import org.scalatest.mock.MockitoSugar
import payloads.CreateAddressPayload
import responses.CreditCardsResponse.{Root ⇒ CardResponse}
import responses.CustomerResponse
import responses.order.FullOrder
import services.orders.OrderPaymentUpdater
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick.implicits._
import utils.jdbc._
import utils.seeds.Seeds.Factories
import utils.seeds.RankingSeedsGenerator.generateCustomer
import utils.{Apis, StripeApi}
import Extensions._
import slick.driver.PostgresDriver.api._
import util.SlickSupport.implicits._
import concurrent.ExecutionContext.Implicits.global

import failures.CreditCardFailures.{CannotUseInactiveCreditCard, StripeFailure}
import failures.CustomerFailures._
import failures.{GeneralFailure, NotFoundFailure404}
import services.{CreditCardManager, Result}

class CustomerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with SortingAndPaging[CustomerResponse.Root]
  with MockitoSugar {

  implicit val ac = ActivityContext(userId = 1, userType = "b", transactionId = "c")

  // paging and sorting API
  val uriPrefix = "v1/customers"

  def responseItems = {
    val insertCustomers = (1 to numOfResults).map { _ ⇒ generateCustomer }
    val dbio = for {
      customers ← Customers.createAll(insertCustomers) >> Customers.result
    } yield customers.map(CustomerResponse.build(_))

    dbio.transactionally.run().futureValue.toIndexedSeq
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
      Customers.create(stub).futureValue
      val failure = GeneralFailure("record was not unique")
      val xor = swapDatabaseFailure(Customers.create(stub).run())((NotUnique, failure)).futureValue

      xor.leftVal must === (failure.single)
    }

    "accounts are NOT unique for guest account and email" in {
      val stub = Factories.customer.copy(isGuest = true)
      val customers = (1 to 3).map(_ ⇒ Customers.create(stub).futureValue.rightVal)
      customers.map(_.id) must contain allOf(1, 2, 3)
    }
  }

  "GET /v1/customers" - {
    "lists customers" in new Fixture {
      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region)

      response.status must === (StatusCodes.OK)
      response.ignoreFailuresAndGiveMe[Seq[CustomerResponse.Root]] must === (Seq(customerRoot))
    }

    "lists customers without default address" in new Fixture {
      Addresses.filter(_.id === address.id).map(_.isDefaultShipping).update(false).run().futureValue
      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer)

      response.status must === (StatusCodes.OK)
      response.ignoreFailuresAndGiveMe[Seq[CustomerResponse.Root]] must === (Seq(customerRoot))
    }

    "customer listing shows valid billingRegion" in new CreditCardFixture {
      val billRegion = Regions.findOneById(creditCard.regionId).run().futureValue

      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region, billingRegion = billRegion)

      response.status must === (StatusCodes.OK)
      response.ignoreFailuresAndGiveMe[Seq[CustomerResponse.Root]] must === (Seq(customerRoot))
    }

    "customer listing shows valid billingRegion without default CreditCard" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).run().futureValue
      val response = GET(s"$uriPrefix")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region)

      response.status must === (StatusCodes.OK)
      response.ignoreFailuresAndGiveMe[Seq[CustomerResponse.Root]] must === (Seq(customerRoot))
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
      response.error must ===(CustomerEmailNotUnique.description)
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

    "ranking" - {
      "customer must be with rank" in new FixtureForRanking {
        pending
        CustomersRanks.refresh.futureValue

        // check that states used in sql still actual
        sqlu"UPDATE orders SET state = 'shipped' WHERE id = ${order.id}".run().futureValue
        sqlu"UPDATE rmas SET state = 'complete' WHERE id = ${orderPayment.id}".run().futureValue

        val response = GET(s"$uriPrefix/${customer.id}")
        response.status must ===(StatusCodes.OK)
        response.as[CustomerResponse.Root].rank must ===(Some(2))
        val rank = CustomersRanks.findById(customer.id).extract.result.head.run().futureValue
        val rank2 = CustomersRanks.findById(customer2.id).extract.result.head.run().futureValue
        rank.revenue must ===(63)
        rank2.revenue must ===(100)
        rank2.rank must ===(1)
      }
    }
  }

  "GET /v1/customers/:customerId/cart" - {
    "returns customer cart" in new CartFixture {
      val response = GET(s"$uriPrefix/${customer.id}/cart")
      response.status must === (StatusCodes.OK)

      val root = response.as[FullOrder.Root]
      root.referenceNumber must === (order.referenceNumber)
      root.orderState must === (Order.Cart)

      Orders.findActiveOrderByCustomer(customer).result.run().futureValue must have size 1
    }

    "creates cart if no present" in new Fixture {
      val response = GET(s"$uriPrefix/${customer.id}/cart")
      response.status must === (StatusCodes.OK)

      val root = response.as[FullOrder.Root]
      root.orderState must === (Order.Cart)

      Orders.findActiveOrderByCustomer(customer).result.run().futureValue must have size 1
    }

    "returns 404 if customer not found" in new CartFixture {
      val response = GET(s"$uriPrefix/999/cart")
      response.status must === (StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Customer, 999).description)
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
      response.error must ===(CustomerEmailNotUnique.description)
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
      response.error must ===(CustomerEmailNotUnique.description)
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
      CustomerResponse.build(created) must === (root.copy(name = Some("test"), isGuest = false))
      created.isGuest must === (false)
    }
  }

  "POST /v1/customers/:customerId/disable" - {
    "toggles the isDisabled flag on a customer account" in new Fixture {
      customer.isDisabled must === (false)

      val disableResp = POST(s"$uriPrefix/${customer.id}/disable", payloads.ToggleCustomerDisabled(true))
      disableResp.status must === (StatusCodes.OK)
      disableResp.as[CustomerResponse.Root].disabled must === (true)

      val enableResp = POST(s"$uriPrefix/${customer.id}/disable", payloads.ToggleCustomerDisabled(false))
      enableResp.status must === (StatusCodes.OK)
      enableResp.as[CustomerResponse.Root].disabled must === (false)
    }

    "fails if customer not found" in new Fixture {
      val response = POST(s"$uriPrefix/999/disable", payloads.ToggleCustomerDisabled(true))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 999).description)
    }

    "disable already disabled account is ok (overwrite behaviour)" in new Fixture {
      val updated = rightValue(Customers.update(customer, customer.copy(isDisabled = true)).futureValue)
      updated.isDisabled must === (true)

      val response = POST(s"$uriPrefix/${customer.id}/disable", payloads.ToggleCustomerDisabled(true))
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root].disabled must === (true)
    }
  }

  "POST /v1/customers/:customerId/blacklist" - {
    "toggles the isBlacklisted flag on a customer account" in new Fixture {
      customer.isBlacklisted must === (false)

      val responseAdd = POST(s"$uriPrefix/${customer.id}/blacklist", payloads.ToggleCustomerBlacklisted(true))
      responseAdd.status must === (StatusCodes.OK)
      responseAdd.as[CustomerResponse.Root].isBlacklisted must === (true)

      val responseRemove = POST(s"$uriPrefix/${customer.id}/blacklist", payloads.ToggleCustomerBlacklisted(false))
      responseRemove.status must === (StatusCodes.OK)
      responseRemove.as[CustomerResponse.Root].isBlacklisted must === (false)
    }

    "fails if customer not found" in new Fixture {
      val response = POST(s"$uriPrefix/999/blacklist", payloads.ToggleCustomerBlacklisted(true))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 999).description)
    }

    "blacklist already blacklisted account is ok (overwrite behaviour)" in new Fixture {
      val updated = rightValue(Customers.update(customer, customer.copy(isBlacklisted = true)).futureValue)
      updated.isBlacklisted must === (true)

      val response = POST(s"$uriPrefix/${customer.id}/blacklist", payloads.ToggleCustomerBlacklisted(true))
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root].isBlacklisted must === (true)
    }
  }

  "GET /v1/customers/:customerId/payment-methods/credit-cards" - {
    "shows customer's credit cards only in their wallet" in new CreditCardFixture {
      val deleted = CreditCards.create(creditCard.copy(id = 0, inWallet = false)).run().futureValue.rightVal

      val response = GET(s"$uriPrefix/${customer.id}/payment-methods/credit-cards")
      val cc = response.as[Seq[CardResponse]]
      val ccRegion = Regions.findOneById(creditCard.regionId).run().futureValue.value

      response.status must === (StatusCodes.OK)
      cc must have size 1
      cc.head must === (responses.CreditCardsResponse.build(creditCard, ccRegion))
      cc.head.id must !== (deleted.id)
    }
  }

  "POST /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId/default" - {
    "sets the isDefault flag on a credit card" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).run().futureValue

      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}/default", payload)

      response.status must === (StatusCodes.OK)
      val ccResp = response.as[CardResponse]
      ccResp.isDefault mustBe true
      ccResp.id must === (creditCard.id)
    }

    "successfully replaces an existing default credit card" in new Fixture {
      val default = CreditCards.create(Factories.creditCard.copy(isDefault = true, customerId = customer.id))
        .run().futureValue.rightVal
      val nonDefault = CreditCards.create(Factories.creditCard.copy(isDefault = false, customerId = customer.id))
        .run().futureValue.rightVal

      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${nonDefault.id}/default", payload)

      val (prevDefault, currDefault) = (
        CreditCards.refresh(default).run().futureValue,
        CreditCards.refresh(nonDefault).run().futureValue)

      response.status must === (StatusCodes.OK)
      val ccResp = response.as[CardResponse]
      ccResp.isDefault mustBe true
      ccResp.id must === (currDefault.id)
      prevDefault.isDefault mustBe false
    }

    "fails when the credit card doesn't exist" in new Fixture {
      val payload = payloads.ToggleDefaultCreditCard(isDefault = true)
      val response = POST(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/99/default", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
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
          thenReturn(Result.good(new StripeCard))

        when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
          thenReturn(Result.good(new StripeCard))

        val payload = payloads.EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val inactive = CreditCards.findOneById(creditCard.id).run().futureValue.value

        response.status must === (StatusCodes.OK)

        val root = response.as[CardResponse]
        root.id must !== (creditCard.id)
        root.inWallet mustBe true
      }

      "creates a new version of the edited card in the wallet" in new CreditCardFixture {
        reset(stripeApi)

        when(stripeApi.findCustomer(m.any(), m.any())).
          thenReturn(Result.good(new StripeCustomer))

        when(stripeApi.findDefaultCard(m.any(), m.any())).
          thenReturn(Result.good(new StripeCard))

        when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
          thenReturn(Result.good(new StripeCard))

        val payload = payloads.EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

        response.status must === (StatusCodes.OK)
        newVersion.inWallet mustBe true
        newVersion.isDefault must === (creditCard.isDefault)
      }

      "updates the customer's cart to use the new version" in new CreditCardFixture {
        reset(stripeApi)

        when(stripeApi.findCustomer(m.any(), m.any())).
          thenReturn(Result.good(new StripeCustomer))

        when(stripeApi.findDefaultCard(m.any(), m.any())).
          thenReturn(Result.good(new StripeCard))

        when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
          thenReturn(Result.good(mock[StripeCard]))

        val order = Orders.create(Factories.cart.copy(customerId = customer.id)).run().futureValue.rightVal
        OrderPaymentUpdater.addCreditCard(Originator(admin), creditCard.id, Some(order.refNum)).futureValue

        val payload = payloads.EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (pmt :: Nil) = OrderPayments.filter(_.orderId === order.id).creditCards.result.run().futureValue.toList
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.run().futureValue.toList

        response.status must === (StatusCodes.OK)
        pmt.amount mustBe 'empty
        pmt.isCreditCard mustBe true
        pmt.paymentMethodId must === (newVersion.id)
      }

      "copies an existing address book entry to the creditCard" in new CreditCardFixture {
        val payload = payloads.EditCreditCard(holderName = Some("Bob"), addressId = address.id.some)
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
        val numAddresses = Addresses.length.result.futureValue

        response.status must === (StatusCodes.OK)
        numAddresses must === (1)
        (newVersion.zip, newVersion.regionId) must === ((address.zip, address.regionId))
      }

      "creates a new address book entry if a full address was given" in new CreditCardFixture {
        reset(stripeApi)

        when(stripeApi.findCustomer(m.any(), m.any())).
          thenReturn(Result.good(new StripeCustomer))

        when(stripeApi.findDefaultCard(m.any(), m.any())).
          thenReturn(Result.good(new StripeCard))

        when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
          thenReturn(Result.good(mock[StripeCard]))

        val payload = payloads.EditCreditCard(holderName = Some("Bob"),
          address = CreateAddressPayload(name = "Home Office", regionId = address.regionId + 1,
            address1 = "3000 Coolio Dr", city = "Seattle", zip = "54321").some)
        val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).result.futureValue.toList
        val addresses = Addresses.result.run().futureValue
        val newAddress = addresses.last

        response.status must === (StatusCodes.OK)
        addresses must have size 2
        (newVersion.zip, newVersion.regionId) must === (("54321", address.regionId + 1))
        (newVersion.zip, newVersion.regionId) must === ((newAddress.zip, newAddress.regionId))
      }
    }

    "fails if the card cannot be found" in new CreditCardFixture {
      val payload = payloads.EditCreditCard
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/99", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
    }

    "fails if the card is not inWallet" in new CreditCardFixture {
      CreditCardManager.deleteCreditCard(customer.id, creditCard.id, Some(admin)).futureValue
      val payload = payloads.EditCreditCard
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CannotUseInactiveCreditCard(creditCard).description)
    }

    "fails if the payload is invalid" in new CreditCardFixture {
      val payload = payloads.EditCreditCard(holderName = "".some)
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("holderName must not be empty")
    }

    "fails if stripe returns an error" in new CreditCardFixture {
      reset(stripeApi)

      when(stripeApi.findCustomer(m.any(), m.any())).
        thenReturn(Result.good(new StripeCustomer))

      when(stripeApi.findDefaultCard(m.any(), m.any())).
        thenReturn(Result.good(new StripeCard))

      when(stripeApi.updateExternalAccount(m.any(), m.any(), m.any())).
        thenReturn(Result.failure(
          StripeFailure(
            new CardException(
              "Your card's expiration year is invalid",
              "X_REQUEST_ID: 1",
              "invalid_expiry_year",
              "exp_year", null, null, null, null))))

      val payload = payloads.EditCreditCard(expYear = Some(2000))
      val response = PATCH(s"$uriPrefix/${customer.id}/payment-methods/credit-cards/${creditCard.id}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("Your card's expiration year is invalid")
    }
  }

  "GET /v1/customers/searchForNewOrder?term=:term" - {
    "successfully search by term" in new Fixture {
      val response = GET(s"${uriPrefix}/searchForNewOrder?term=${customer.email.drop(2)}")
      response.status must === (StatusCodes.OK)

      response.ignoreFailuresAndGiveMe[Seq[CustomerResponse.Root]].size must === (1)
    }
  }

  trait Fixture {
    val (customer, address, region, admin) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      region ← * <~ Regions.findOneById(address.regionId).toXor
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (customer, address, region, admin)).runTxn().futureValue.rightVal
  }

  trait CreditCardFixture extends Fixture {
    val creditCard = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).run().futureValue.rightVal
  }

  trait CartFixture extends Fixture {
    val order = Orders.create(Factories.order.copy(customerId = customer.id, state = Order.Cart,
      referenceNumber = "ABC-123")).run().futureValue.rightVal
  }

  trait FixtureForRanking extends CreditCardFixture {
    val (order, orderPayment, customer2) = (for {
      customer2 ← * <~ Customers.create(Factories.customer.copy(email = "second@example.org", name = Some("second")))
      order ← * <~ Orders.create(Factories.order.copy(customerId = customer.id,
        state = Order.Shipped,
        referenceNumber = "ABC-123"))
      order2 ← * <~ Orders.create(Factories.order.copy(customerId = customer2.id,
        state = Order.Shipped,
        referenceNumber = "ABC-456"))
      orderPayment ← * <~ OrderPayments.create(Factories.orderPayment.copy(orderId = order.id,
        paymentMethodId = creditCard.id,
        amount = None))
      orderPayment2 ← * <~ OrderPayments.create(Factories.orderPayment.copy(orderId = order2.id,
        paymentMethodId = creditCard.id,
        amount = None))
      rma ← * <~ Rmas.create(Factories.rma.copy(
        referenceNumber = "ABC-123.1",
        orderId = order.id,
        state = Rma.Complete,
        orderRefNum = order.referenceNumber,
        customerId = customer.id))
      rmaPayment ← * <~ sqlu"""insert into rma_payments(rma_id, payment_method_id, payment_method_type, amount, currency)
              values(${rma.id}, ${creditCard.id}, ${PaymentMethod.Type.show(PaymentMethod.CreditCard)}, 37,
               ${Currency.USD.toString})
            """
    } yield (order, orderPayment, customer2)).runTxn().futureValue.rightVal
  }
}
