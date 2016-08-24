import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import com.stripe.exception.CardException
import com.stripe.model.{DeletedExternalAccount, ExternalAccount}
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.CustomerFailures._
import failures.StripeFailures.StripeFailure
import failures.{GeneralFailure, NotFoundFailure404}
import models.StoreAdmins
import models.cord.OrderPayments.scope._
import models.cord._
import models.customer._
import models.location.{Addresses, Regions}
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.returns._
import models.shipping.Shipment.Shipped
import models.shipping.{Shipment, Shipments}
import models.traits.Originator
import org.mockito.Mockito.{reset, when}
import org.mockito.{Matchers ⇒ m}
import org.scalatest.mock.MockitoSugar
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CustomerPayloads._
import payloads.PaymentPayloads._
import responses.CreditCardsResponse.{Root ⇒ CardResponse}
import responses.CustomerResponse
import responses.CustomerResponse.Root
import responses.cord.CartResponse
import services.carts.CartPaymentUpdater
import services.{CreditCardManager, Result}
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures
import utils.Money.Currency
import utils.aliases.stripe._
import utils.db._
import utils.jdbc._
import utils.seeds.Seeds.Factories

class CustomerIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockitoSugar
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "Customer" - {
    "accounts are unique based on email, non-guest, and active" in {
      val stub = Factories.customer.copy(isGuest = false, isDisabled = false)
      Customers.create(stub).gimme
      val failure = GeneralFailure("record was not unique")
      val xor     = swapDatabaseFailure(Customers.create(stub).run())((NotUnique, failure)).futureValue

      xor.leftVal must === (failure.single)
    }

    "accounts are NOT unique for guest account and email" in {
      val stub      = Factories.customer.copy(isGuest = true)
      val customers = (1 to 3).map(_ ⇒ Customers.create(stub).gimme)
      customers.map(_.id) must contain allOf (1, 2, 3)
    }
  }

  "POST /v1/customers" - {
    "successfully creates customer from payload" in {
      val response = POST(s"v1/customers",
                          CreateCustomerPayload(email = "test@example.com", name = Some("test")))

      response.status must === (StatusCodes.OK)

      val root    = response.as[CustomerResponse.Root]
      val created = Customers.findOneById(root.id).run().futureValue.value
      created.id must === (root.id)
    }

    "fails if email is already in use" in new Customer_Seed {
      val response = POST(s"v1/customers",
                          CreateCustomerPayload(email = customer.email.value, name = Some("test")))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CustomerEmailNotUnique.description)
    }
  }

  "GET /v1/customers/:customerId" - {
    "fetches customer info" in new Fixture {
      val response     = GET(s"v1/customers/${customer.id}")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region.some)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "fetches customer info without default address" in new Fixture {
      Addresses.filter(_.id === address.id).map(_.isDefaultShipping).update(false).gimme
      val response     = GET(s"v1/customers/${customer.id}")
      val customerRoot = CustomerResponse.build(customer)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion" in new CreditCardFixture {
      val billRegion = Regions.findOneById(creditCard.regionId).gimme

      val response = GET(s"v1/customers/${customer.id}")
      val customerRoot =
        CustomerResponse.build(customer, shippingRegion = region.some, billingRegion = billRegion)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion without default CreditCard" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).gimme
      val response     = GET(s"v1/customers/${customer.id}")
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region.some)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "empty phone number is resolved from" - {
      "default shipping address" in {
        val defaultPhoneNumber: String = "1111111111"

        val (customer, region) = (for {
          customer ← * <~ Customers.create(Factories.customer.copy(phoneNumber = None))
          address ← * <~ Addresses.create(
                       Factories.address.copy(customerId = customer.id,
                                              isDefaultShipping = true,
                                              phoneNumber = defaultPhoneNumber.some))
          region ← * <~ Regions.findOneById(address.regionId)
        } yield (customer, region)).gimme

        val response = GET(s"v1/customers/${customer.id}")
        val customerRoot =
          CustomerResponse.build(customer.copy(phoneNumber = defaultPhoneNumber.some),
                                 shippingRegion = region)

        response.status must === (StatusCodes.OK)
        response.as[CustomerResponse.Root] must === (customerRoot)
      }

      "last shipping address" in {

        val phoneNumbers = Seq("1111111111", "2222222222")

        val defaultAddress = Factories.address.copy(isDefaultShipping = true, phoneNumber = None)

        def shippingAddresses(orders: Seq[(Order, String)]) =
          orders.map {
            case (order, phone) ⇒
              OrderShippingAddress
                .buildFromAddress(defaultAddress)
                .copy(cordRef = order.refNum, phoneNumber = phone.some)
          }

        val (customer, region, shipments) = (for {
          customer ← * <~ Customers.create(Factories.customer.copy(phoneNumber = None))
          address  ← * <~ Addresses.create(defaultAddress.copy(customerId = customer.id))
          region   ← * <~ Regions.findOneById(address.regionId)
          cart1    ← * <~ Carts.create(Cart(referenceNumber = "ABC-1", customerId = customer.id))
          order1   ← * <~ Orders.createFromCart(cart1)
          order1   ← * <~ Orders.update(order1, order1.copy(state = Order.FulfillmentStarted))
          order1   ← * <~ Orders.update(order1, order1.copy(state = Order.Shipped))
          cart2    ← * <~ Carts.create(Cart(referenceNumber = "ABC-2", customerId = customer.id))
          order2   ← * <~ Orders.createFromCart(cart2)
          order2   ← Orders.update(order2, order2.copy(state = Order.FulfillmentStarted))
          order2   ← Orders.update(order2, order2.copy(state = Order.Shipped))
          orders = Seq(order1, order2)
          addresses ← * <~ shippingAddresses(orders.zip(phoneNumbers)).map(a ⇒
                           OrderShippingAddresses.create(a))
          shipments ← * <~ addresses.map(
                         address ⇒
                           Shipments.create(
                               Factories.shipment.copy(cordRef = address.cordRef,
                                                       shippingAddressId = address.id.some,
                                                       orderShippingMethodId = None,
                                                       state = Shipped)))
        } yield (customer, region, shipments)).gimme

        def updateShipmentTime(s: Shipment, newTime: Instant ⇒ Instant): Unit =
          Shipments.update(s, s.copy(updatedAt = s.updatedAt.map(time ⇒ newTime(time)))).gimme

        def runTest(expectedPhone: String) = {
          val response = GET(s"v1/customers/${customer.id}")
          response.status must === (StatusCodes.OK)
          val customerResponse = response.as[Root]
          customerResponse.shippingRegion must === (region)
          customerResponse.phoneNumber must === (expectedPhone.some)
        }

        updateShipmentTime(shipments.head, _.minusSeconds(10))
        runTest(expectedPhone = phoneNumbers(1))

        updateShipmentTime(shipments(1), _.minusSeconds(11))
        runTest(expectedPhone = phoneNumbers.head)
      }
    }

    "fetches customer info with lastOrderDays value" in new Order_Baked {
      val expectedCustomer =
        CustomerResponse.build(customer, shippingRegion = region.some, lastOrderDays = Some(0))

      val response = GET(s"v1/customers/${customer.id}")
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (expectedCustomer)

      Orders.update(order, order.copy(placedAt = Instant.now.minus(1, ChronoUnit.DAYS))).gimme

      val secondResponse = GET(s"v1/customers/${customer.id}")

      secondResponse.status must === (StatusCodes.OK)
      secondResponse.as[CustomerResponse.Root] must === (
          expectedCustomer.copy(lastOrderDays = Some(1l)))
    }

    "ranking" - {
      "customer must be with rank" in new FixtureForRanking {
        pending
        CustomersRanks.refresh.futureValue

        // check that states used in sql still actual
        sqlu"UPDATE orders SET state = 'shipped' WHERE reference_number = ${order.refNum}"
          .run()
          .futureValue
        sqlu"UPDATE rmas SET state = 'complete' WHERE id = ${orderPayment.id}".gimme

        val response = GET(s"v1/customers/${customer.id}")
        response.status must === (StatusCodes.OK)
        response.as[CustomerResponse.Root].rank must === (Some(2))
        val rank  = CustomersRanks.findById(customer.id).extract.result.head.gimme
        val rank2 = CustomersRanks.findById(customer2.id).extract.result.head.gimme
        rank.revenue must === (63)
        rank2.revenue must === (100)
        rank2.rank must === (1)
      }
    }
  }

  "GET /v1/customers/:customerId/cart" - {
    "returns customer cart" in new EmptyCustomerCart_Baked {
      val response = GET(s"v1/customers/${customer.id}/cart")
      response.status must === (StatusCodes.OK)

      val root = response.as[CartResponse]
      root.referenceNumber must === (cart.referenceNumber)

      Carts.findByCustomer(customer).gimme must have size 1
    }

    "creates cart if no present" in new Fixture {
      val response = GET(s"v1/customers/${customer.id}/cart")
      response.status must === (StatusCodes.OK)

      val root = response.as[CartResponse]

      Carts.findByCustomer(customer).gimme must have size 1
    }

    "returns 404 if customer not found" in new EmptyCustomerCart_Baked {
      val response = GET(s"v1/customers/999/cart")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 999).description)
    }
  }

  "PATCH /v1/customers/:customerId" - {
    "successfully updates customer attributes" in new Fixture {
      val payload = UpdateCustomerPayload(name = "John Doe".some,
                                          email = "newemail@example.org".some,
                                          phoneNumber = "555 555 55".some)
      (payload.name, payload.email, payload.phoneNumber) must !==(
          (customer.name, customer.email, customer.phoneNumber))

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      val updated = response.as[CustomerResponse.Root]
      (updated.name, updated.email, updated.phoneNumber) must === (
          (payload.name, payload.email, payload.phoneNumber))
    }

    "fails if email is already in use" in new Fixture {
      val newUserResponse =
        POST(s"v1/customers",
             CreateCustomerPayload(email = "test@example.com", name = Some("test")))

      newUserResponse.status must === (StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload  = UpdateCustomerPayload(email = customer.email)
      val response = PATCH(s"v1/customers/${root.id}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CustomerEmailNotUnique.description)
    }
  }

  "POST /v1/customers/:customerId/activate" - {
    "fails if email is already in use by non-guest user" in new Fixture {
      val newUserResponse =
        POST(s"v1/customers",
             CreateCustomerPayload(email = customer.email.value, isGuest = Some(true)))

      newUserResponse.status must === (StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload  = ActivateCustomerPayload(name = "test")
      val response = POST(s"v1/customers/${root.id}/activate", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CustomerEmailNotUnique.description)
    }

    "sucessfully activate non-guest user" in new Fixture {
      val newUserResponse =
        POST(s"v1/customers",
             CreateCustomerPayload(email = "guest@example.com", isGuest = Some(true)))

      newUserResponse.status must === (StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload  = ActivateCustomerPayload(name = "test")
      val response = POST(s"v1/customers/${root.id}/activate", payload)
      response.status must === (StatusCodes.OK)

      val created = Customers.findOneById(root.id).gimme.value
      CustomerResponse.build(created) must === (root.copy(name = Some("test"), isGuest = false))
      created.isGuest must === (false)
    }
  }

  "POST /v1/customers/:customerId/disable" - {
    "toggles the isDisabled flag on a customer account" in new Fixture {
      customer.isDisabled must === (false)

      val disableResp = POST(s"v1/customers/${customer.id}/disable", ToggleCustomerDisabled(true))
      disableResp.status must === (StatusCodes.OK)
      disableResp.as[CustomerResponse.Root].disabled must === (true)

      val enableResp = POST(s"v1/customers/${customer.id}/disable", ToggleCustomerDisabled(false))
      enableResp.status must === (StatusCodes.OK)
      enableResp.as[CustomerResponse.Root].disabled must === (false)
    }

    "fails if customer not found" in new Fixture {
      val response = POST(s"v1/customers/999/disable", ToggleCustomerDisabled(true))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 999).description)
    }

    "disable already disabled account is ok (overwrite behaviour)" in new Fixture {
      val updated = Customers.update(customer, customer.copy(isDisabled = true)).gimme
      updated.isDisabled must === (true)

      val response = POST(s"v1/customers/${customer.id}/disable", ToggleCustomerDisabled(true))
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root].disabled must === (true)
    }
  }

  "POST /v1/customers/:customerId/blacklist" - {
    "toggles the isBlacklisted flag on a customer account" in new Fixture {
      customer.isBlacklisted must === (false)

      val responseAdd =
        POST(s"v1/customers/${customer.id}/blacklist", ToggleCustomerBlacklisted(true))
      responseAdd.status must === (StatusCodes.OK)
      responseAdd.as[CustomerResponse.Root].isBlacklisted must === (true)

      val responseRemove =
        POST(s"v1/customers/${customer.id}/blacklist", ToggleCustomerBlacklisted(false))
      responseRemove.status must === (StatusCodes.OK)
      responseRemove.as[CustomerResponse.Root].isBlacklisted must === (false)
    }

    "fails if customer not found" in new Fixture {
      val response = POST(s"v1/customers/999/blacklist", ToggleCustomerBlacklisted(true))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 999).description)
    }

    "blacklist already blacklisted account is ok (overwrite behaviour)" in new Fixture {
      val updated = Customers.update(customer, customer.copy(isBlacklisted = true)).gimme
      updated.isBlacklisted must === (true)

      val response =
        POST(s"v1/customers/${customer.id}/blacklist", ToggleCustomerBlacklisted(true))
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root].isBlacklisted must === (true)
    }
  }

  "GET /v1/customers/:customerId/payment-methods/credit-cards" - {
    "shows customer's credit cards only in their wallet" in new CreditCardFixture {
      val deleted = CreditCards.create(creditCard.copy(id = 0, inWallet = false)).gimme

      val response = GET(s"v1/customers/${customer.id}/payment-methods/credit-cards")
      val cc       = response.as[Seq[CardResponse]]
      val ccRegion = Regions.findOneById(creditCard.regionId).gimme.value

      response.status must === (StatusCodes.OK)
      cc must have size 1
      cc.head must === (responses.CreditCardsResponse.build(creditCard, ccRegion))
      cc.head.id must !==(deleted.id)
    }
  }

  "POST /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId/default" - {
    "sets the isDefault flag on a credit card" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).gimme

      val payload = ToggleDefaultCreditCard(isDefault = true)
      val response =
        POST(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}/default",
             payload)

      response.status must === (StatusCodes.OK)
      val ccResp = response.as[CardResponse]
      ccResp.isDefault mustBe true
      ccResp.id must === (creditCard.id)
    }

    "successfully replaces an existing default credit card" in new Fixture {
      val default = CreditCards
        .create(Factories.creditCard.copy(isDefault = true, customerId = customer.id))
        .gimme
      val nonDefault = CreditCards
        .create(Factories.creditCard.copy(isDefault = false, customerId = customer.id))
        .gimme

      val payload = ToggleDefaultCreditCard(isDefault = true)
      val response =
        POST(s"v1/customers/${customer.id}/payment-methods/credit-cards/${nonDefault.id}/default",
             payload)

      val (prevDefault, currDefault) =
        (CreditCards.refresh(default).gimme, CreditCards.refresh(nonDefault).gimme)

      response.status must === (StatusCodes.OK)
      val ccResp = response.as[CardResponse]
      ccResp.isDefault mustBe true
      ccResp.id must === (currDefault.id)
      prevDefault.isDefault mustBe false
    }

    "fails when the credit card doesn't exist" in new Fixture {
      val payload = ToggleDefaultCreditCard(isDefault = true)
      val response =
        POST(s"v1/customers/${customer.id}/payment-methods/credit-cards/99/default", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
    }
  }

  "DELETE /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId" - {
    "deletes successfully if the card exists" in new CreditCardFixture {
      reset(stripeApiMock)

      when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

      when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

      when(stripeApiMock.deleteExternalAccount(m.any()))
        .thenReturn(Result.good(new DeletedExternalAccount))

      val response =
        DELETE(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}")
      val deleted = CreditCards.findOneById(creditCard.id).gimme.value

      response.status must === (StatusCodes.NoContent)
      deleted.inWallet must === (false)
      deleted.deletedAt mustBe 'defined
    }
  }

  "PATCH /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId" - {
    "when successful" - {
      "removes the original card from wallet" in new CreditCardFixture {
        reset(stripeApiMock)

        when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

        when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

        when(stripeApiMock.updateExternalAccount(m.any(), m.any()))
          .thenReturn(Result.good(new StripeCard))

        val payload = EditCreditCard(holderName = Some("Bob"))
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        val inactive = CreditCards.findOneById(creditCard.id).gimme.value

        response.status must === (StatusCodes.OK)

        val root = response.as[CardResponse]
        root.id must !==(creditCard.id)
        root.inWallet mustBe true
      }

      "creates a new version of the edited card in the wallet" in new CreditCardFixture {
        reset(stripeApiMock)

        when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

        when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

        when(stripeApiMock.updateExternalAccount(m.any(), m.any()))
          .thenReturn(Result.good(new StripeCard))

        val payload = EditCreditCard(holderName = Some("Bob"))
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).gimme.toList

        response.status must === (StatusCodes.OK)
        newVersion.inWallet mustBe true
        newVersion.isDefault must === (creditCard.isDefault)
      }

      "updates the customer's cart to use the new version" in new CreditCardFixture {
        reset(stripeApiMock)

        when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

        when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

        when(stripeApiMock.updateExternalAccount(m.any(), m.any()))
          .thenReturn(Result.good(mock[StripeCard]))

        CartPaymentUpdater
          .addCreditCard(Originator(storeAdmin), creditCard.id, Some(cart.refNum))
          .gimme

        val payload = EditCreditCard(holderName = Some("Bob"))
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        val (pmt :: Nil)        = OrderPayments.filter(_.cordRef === cart.refNum).creditCards.gimme.toList
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).gimme.toList

        response.status must === (StatusCodes.OK)
        pmt.amount mustBe 'empty
        pmt.isCreditCard mustBe true
        pmt.paymentMethodId must === (newVersion.id)
      }

      "copies an existing address book entry to the creditCard" in new CreditCardFixture {
        val payload = EditCreditCard(holderName = Some("Bob"), addressId = address.id.some)
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        val (newVersion :: Nil) =
          CreditCards.filter(_.parentId === creditCard.id).result.gimme.toList
        val numAddresses = Addresses.length.result.gimme

        response.status must === (StatusCodes.OK)
        numAddresses must === (1)
        (newVersion.zip, newVersion.regionId) must === ((address.zip, address.regionId))
      }

      "creates a new address book entry if a full address was given" in new CreditCardFixture {
        reset(stripeApiMock)

        when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

        when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

        when(stripeApiMock.updateExternalAccount(m.any(), m.any()))
          .thenReturn(Result.good(mock[StripeCard]))

        val payload = EditCreditCard(holderName = Some("Bob"),
                                     address =
                                       CreateAddressPayload(name = "Home Office",
                                                            regionId = address.regionId + 1,
                                                            address1 = "3000 Coolio Dr",
                                                            city = "Seattle",
                                                            zip = "54321").some)
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        val (newVersion :: Nil) =
          CreditCards.filter(_.parentId === creditCard.id).result.gimme.toList
        val addresses  = Addresses.gimme
        val newAddress = addresses.last

        response.status must === (StatusCodes.OK)
        addresses must have size 2
        (newVersion.zip, newVersion.regionId) must === (("54321", address.regionId + 1))
        (newVersion.zip, newVersion.regionId) must === ((newAddress.zip, newAddress.regionId))
      }
    }

    "fails if the card cannot be found" in new CreditCardFixture {
      val payload  = EditCreditCard
      val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/99", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
    }

    "fails if the card is not inWallet" in new CreditCardFixture {
      reset(stripeApiMock)

      when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

      when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

      when(stripeApiMock.updateExternalAccount(m.any(), m.any()))
        .thenReturn(Result.good(new ExternalAccount))

      when(stripeApiMock.deleteExternalAccount(m.any()))
        .thenReturn(Result.good(new DeletedExternalAccount))

      CreditCardManager.deleteCreditCard(customer.id, creditCard.id, Some(storeAdmin)).gimme

      val response =
        PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
              EditCreditCard)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CannotUseInactiveCreditCard(creditCard).description)
    }

    "fails if the payload is invalid" in new CreditCardFixture {
      val payload = EditCreditCard(holderName = "".some)
      val response =
        PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
              payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("holderName must not be empty")
    }

    "fails if stripe returns an error" in new CreditCardFixture {
      reset(stripeApiMock)

      when(stripeApiMock.findCustomer(m.any())).thenReturn(Result.good(new StripeCustomer))

      when(stripeApiMock.findDefaultCard(m.any())).thenReturn(Result.good(new StripeCard))

      val exception = new CardException("Your card's expiration year is invalid",
                                        "X_REQUEST_ID: 1",
                                        "invalid_expiry_year",
                                        "exp_year",
                                        null,
                                        null,
                                        null,
                                        null)
      when(stripeApiMock.updateExternalAccount(m.any(), m.any()))
        .thenReturn(Result.failure(StripeFailure(exception)))

      val payload = EditCreditCard(expYear = Some(2000))
      val response =
        PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
              payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("Your card's expiration year is invalid")
    }
  }

  "POST /v1/public/remind-password" - {
    "Successfully creates remind" in new Fixture {
      val email    = customer.email.value
      val response = POST(s"v1/public/remind-password", RemindPassword(email))
      response.status must === (StatusCodes.OK)

      val remindOpt = CustomerPasswordResets.filter(_.email === email).one.gimme
      remindOpt mustBe 'defined
      val remind = remindOpt.value
      remind.state must === (CustomerPasswordReset.Initial)
      remind.email must === (email)
    }

    "fails if customer already have active remind" in new Fixture {
      val email = customer.email.value
      val remind = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      val response = POST(s"v1/public/remind-password", RemindPassword(email))
      response.status must === (StatusCodes.BadRequest)
      response.error must === (PasswordResetAlreadyInitiated(email).description)
    }
  }

  "POST /v1/public/reset-password" - {
    "Successfully reset password" in new Fixture {
      val remind = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      val response =
        POST(s"v1/public/reset-password", ResetPassword(code = remind.code, newPassword = "456"))
      response.status must === (StatusCodes.OK)
      val updatedCustomer = Customers.mustFindById404(customer.id).gimme
      updatedCustomer.hashedPassword must !==(customer.hashedPassword)

      val newRemind = CustomerPasswordResets.mustFindById404(remind.id).gimme
      newRemind.state must === (CustomerPasswordReset.PasswordRestored)
      newRemind.activatedAt mustBe 'defined
    }

    "fails if customer reset code is already used" in new Fixture {
      val remind = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      val response =
        POST(s"v1/public/reset-password", ResetPassword(code = remind.code, newPassword = "456"))
      response.status must === (StatusCodes.OK)

      val response2 =
        POST(s"v1/public/reset-password", ResetPassword(code = remind.code, newPassword = "456"))
      response2.status must === (StatusCodes.BadRequest)
      response2.error must === (ResetPasswordCodeInvalid.description)
    }
  }

  trait Fixture extends StoreAdmin_Seed with CustomerAddress_Baked

  trait CreditCardFixture extends Fixture with EmptyCustomerCart_Baked {
    val creditCard = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme
  }

  trait FixtureForRanking extends EmptyCustomerCart_Baked with CreditCardFixture {
    val (order, orderPayment, customer2) = (for {
      customer2 ← * <~ Customers.create(
                     Factories.customer.copy(email = "second@example.org".some,
                                             name = "second".some))
      cart2  ← * <~ Carts.create(Cart(customerId = customer2.id, referenceNumber = "ABC-456"))
      order  ← * <~ Orders.createFromCart(cart)
      order  ← * <~ Orders.update(order, order.copy(state = Order.FulfillmentStarted))
      order  ← * <~ Orders.update(order, order.copy(state = Order.Shipped))
      order2 ← * <~ Orders.createFromCart(cart2)
      order2 ← * <~ Orders.update(order2, order2.copy(state = Order.FulfillmentStarted))
      order2 ← * <~ Orders.update(order2, order2.copy(state = Order.Shipped))
      orderPayment ← * <~ OrderPayments.create(
                        Factories.orderPayment.copy(cordRef = order.refNum,
                                                    paymentMethodId = creditCard.id,
                                                    amount = None))
      orderPayment2 ← * <~ OrderPayments.create(
                         Factories.orderPayment.copy(cordRef = order2.refNum,
                                                     paymentMethodId = creditCard.id,
                                                     amount = None))
      rma ← * <~ Returns.create(
               Factories.rma.copy(referenceNumber = "ABC-123.1",
                                  orderId = order.id,
                                  orderRef = order.refNum,
                                  state = Return.Complete,
                                  customerId = customer.id))
      returnPayment ← * <~ sqlu"""insert into return_payments(return_id, payment_method_id, payment_method_type,
                            amount,
                            currency)
              values(${rma.id}, ${creditCard.id}, ${PaymentMethod.Type.show(
                         PaymentMethod.CreditCard)}, 37,
               ${Currency.USD.toString})
            """
    } yield (order, orderPayment, customer2)).gimme
  }
}
