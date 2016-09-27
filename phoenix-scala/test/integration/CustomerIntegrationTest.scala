import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import com.stripe.exception.CardException
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.CustomerFailures._
import failures.StripeFailures.StripeFailure
import failures.{GeneralFailure, NotFoundFailure400, NotFoundFailure404}
import models.cord.OrderPayments.scope._
import models.cord._
import models.customer._
import models.location.{Address, Addresses, Regions}
import models.payment.creditcard._
import models.shipping.Shipment.Shipped
import models.shipping.{Shipment, Shipments}
import models.traits.Originator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
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
import utils._
import utils.aliases.stripe.StripeCard
import utils.db._
import utils.jdbc._
import utils.seeds.Seeds.Factories

class CustomerIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockedApis
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
                          CreateCustomerPayload(email = "test@example.com", name = "test".some))

      response.status must === (StatusCodes.OK)

      val root    = response.as[CustomerResponse.Root]
      val created = Customers.findOneById(root.id).run().futureValue.value
      created.id must === (root.id)
    }

    "fails if email is already in use" in new Customer_Seed {
      val response = POST(s"v1/customers",
                          CreateCustomerPayload(email = customer.email.value, name = "test".some))

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
        CustomerResponse.build(customer, shippingRegion = region.some, lastOrderDays = 0l.some)

      val response = GET(s"v1/customers/${customer.id}")
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (expectedCustomer)

      Orders.update(order, order.copy(placedAt = Instant.now.minus(1, ChronoUnit.DAYS))).gimme

      val secondResponse = GET(s"v1/customers/${customer.id}")

      secondResponse.status must === (StatusCodes.OK)
      secondResponse.as[CustomerResponse.Root] must === (
          expectedCustomer.copy(lastOrderDays = 1l.some))
    }

    "ranking" - {
      "customer must be with rank" in new FixtureForRanking {

        // check that states used in sql still actual
        sqlu"UPDATE orders SET state = 'shipped' WHERE reference_number = ${order.refNum}".gimme
        sql"SELECT public.update_customers_ranking()".as[Boolean].gimme

        val response = GET(s"v1/customers/${customer.id}")
        response.status must === (StatusCodes.OK)
        response.as[CustomerResponse.Root].rank must === (2.some)
        val rank  = CustomersRanks.findById(customer.id).extract.result.head.gimme
        val rank2 = CustomersRanks.findById(customer2.id).extract.result.head.gimme
        rank.revenue must === (charge1.amount)
        rank2.revenue must === (charge2.amount)
        rank2.rank must === (1.some)
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
             CreateCustomerPayload(email = "test@example.com", name = "test".some))

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
             CreateCustomerPayload(email = customer.email.value, isGuest = true.some))

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
             CreateCustomerPayload(email = "guest@example.com", isGuest = true.some))

      newUserResponse.status must === (StatusCodes.OK)
      val root = newUserResponse.as[CustomerResponse.Root]

      val payload  = ActivateCustomerPayload(name = "test")
      val response = POST(s"v1/customers/${root.id}/activate", payload)
      response.status must === (StatusCodes.OK)

      val created = Customers.findOneById(root.id).gimme.value
      CustomerResponse.build(created) must === (root.copy(name = "test".some, isGuest = false))
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
        val payload = EditCreditCardPayload(holderName = "Bob".some)
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
        val payload = EditCreditCardPayload(holderName = "Bob".some)
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).gimme.toList

        response.status must === (StatusCodes.OK)
        newVersion.inWallet mustBe true
        newVersion.isDefault must === (creditCard.isDefault)
      }

      "updates the customer's cart to use the new version" in new CreditCardFixture {
        CartPaymentUpdater
          .addCreditCard(Originator(storeAdmin), creditCard.id, cart.refNum.some)
          .gimme

        val payload = EditCreditCardPayload(holderName = "Bob".some)
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

      "creates address and updates credit card if no address id provided" in new CreditCardFixture {
        val addressPayload = UpdateCcAddressPayload(
            address.toPayload.copy(regionId = address.regionId + 1, zip = "54321"),
            id = None)
        val payload = EditCreditCardPayload(address = addressPayload.some)
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        response.status must === (StatusCodes.OK)

        val newVersion = CreditCards.filter(_.parentId === creditCard.id).one.gimme.value
        val addresses  = Addresses.gimme
        val newAddress = addresses.last

        addresses must have size 2
        (newVersion.zip, newVersion.regionId) must === (("54321", address.regionId + 1))
        (newVersion.zip, newVersion.regionId) must === ((newAddress.zip, newAddress.regionId))
      }

      "updates address and credit card if address id provided" in new CreditCardFixture {
        val addressPayload = UpdateCcAddressPayload(
            address.toPayload.copy(regionId = address.regionId + 1, zip = "54321"),
            id = address.id.some)
        val payload = EditCreditCardPayload(address = addressPayload.some)
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        response.status must === (StatusCodes.OK)

        val newVersion = CreditCards.filter(_.parentId === creditCard.id).one.gimme.value
        val addresses  = Addresses.gimme
        val updAddress = addresses.last

        addresses must have size 1
        (newVersion.zip, newVersion.regionId) must === (("54321", address.regionId + 1))
        (newVersion.zip, newVersion.regionId) must === ((updAddress.zip, updAddress.regionId))
      }

      "rejects empty payload" in new CreditCardFixture {
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                EditCreditCardPayload(None, None, None, None))
        response.status must === (StatusCodes.BadRequest)
        response.error must === ("At least one of new values must not be empty")
      }

      "errors if no address to update found" in new Customer_Seed {
        val creditCard =
          CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme

        val addressPayload = UpdateCcAddressPayload(Factories.address.toPayload, id = 666.some)
        val payload        = EditCreditCardPayload(address = addressPayload.some)
        val response =
          PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
                payload)
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(Address, 666).description)
      }
    }

    "fails if the card cannot be found" in new CreditCardFixture {
      val payload  = EditCreditCardPayload(holderName = "Bob".some)
      val response = PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/99", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
    }

    "fails if the card is not inWallet" in new CreditCardFixture {
      CreditCardManager.deleteCreditCard(customer.id, creditCard.id, storeAdmin.some).gimme

      val response =
        PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
              EditCreditCardPayload(holderName = "Bob".some))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CannotUseInactiveCreditCard(creditCard).description)
    }

    "fails if the payload is invalid" in new CreditCardFixture {
      val payload = EditCreditCardPayload(holderName = "".some)
      val response =
        PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
              payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("holder name must not be empty")
    }

    "fails if stripe returns an error" in new CreditCardFixture {
      val exception = new CardException("Your card's expiration year is invalid",
                                        "X_REQUEST_ID: 1",
                                        "invalid_expiry_year",
                                        "exp_year",
                                        null,
                                        null,
                                        null,
                                        null)

      when(stripeWrapperMock.updateCard(any(), any()))
        .thenReturn(Result.failure[StripeCard](StripeFailure(exception)))

      // Cannot pass wrong year here, validation will get that
      val payload = EditCreditCardPayload(holderName = "Me".some)
      val response =
        PATCH(s"v1/customers/${customer.id}/payment-methods/credit-cards/${creditCard.id}",
              payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("Your card's expiration year is invalid")
    }
  }

  "POST /v1/public/send-password-reset" - {
    "Successfully creates password reset instance" in new Fixture {
      val email    = customer.email.value
      val response = POST(s"v1/public/send-password-reset", ResetPasswordSend(email))
      response.status must === (StatusCodes.OK)

      val resetPw = CustomerPasswordResets.filter(_.email === email).one.gimme.value
      resetPw.state must === (CustomerPasswordReset.Initial)
      resetPw.email must === (email)
    }

    "re-send with new code if phoenix already send it but customer not activated it" in new Fixture {
      val email = customer.email.value
      val oldResetPw = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      val response = POST(s"v1/public/send-password-reset", ResetPasswordSend(email))
      response.status must === (StatusCodes.OK)
      val resetPw = CustomerPasswordResets.findActiveByEmail(email).one.gimme.value
      oldResetPw.code must !==(resetPw.code)
    }
  }

  "POST /v1/public/reset-password" - {
    "Successfully reset password" in new Fixture {
      val resetPw = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      val response =
        POST(s"v1/public/reset-password", ResetPassword(code = resetPw.code, newPassword = "456"))
      response.status must === (StatusCodes.OK)
      val updatedCustomer = Customers.mustFindById404(customer.id).gimme
      updatedCustomer.hashedPassword must !==(customer.hashedPassword)

      val newResetPw = CustomerPasswordResets.mustFindById404(resetPw.id).gimme
      newResetPw.state must === (CustomerPasswordReset.PasswordRestored)
      newResetPw.activatedAt mustBe 'defined
    }

    "fails if customer reset code is already used" in new Fixture {
      val resetPw = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      val response =
        POST(s"v1/public/reset-password", ResetPassword(code = resetPw.code, newPassword = "456"))
      response.status must === (StatusCodes.OK)

      val response2 =
        POST(s"v1/public/reset-password", ResetPassword(code = resetPw.code, newPassword = "456"))
      response2.status must === (StatusCodes.BadRequest)
      response2.error must === (ResetPasswordCodeInvalid(resetPw.code).description)
    }
  }

  trait Fixture extends StoreAdmin_Seed with CustomerAddress_Baked

  trait CreditCardFixture extends Fixture with EmptyCustomerCart_Baked {
    val creditCard = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme
  }

  trait FixtureForRanking extends EmptyCustomerCart_Baked with CreditCardFixture {
    val (order, orderPayment, customer2, charge1, charge2) = (for {
      customer2 ← * <~ Customers.create(
                     Factories.customer.copy(email = "second@example.org".some,
                                             name = "second".some))
      cart2  ← * <~ Carts.create(Cart(customerId = customer2.id, referenceNumber = "ABC-456"))
      order  ← * <~ Orders.createFromCart(cart)
      order2 ← * <~ Orders.createFromCart(cart2)
      orderPayment ← * <~ OrderPayments.create(
                        Factories.orderPayment.copy(cordRef = order.refNum,
                                                    paymentMethodId = creditCard.id,
                                                    amount = None))
      creditCardCharge1 ← * <~ CreditCardCharges.create(
                             CreditCardCharge(
                                 creditCardId = creditCard.id,
                                 orderPaymentId = orderPayment.id,
                                 chargeId = "asd",
                                 state = CreditCardCharge.FullCapture,
                                 amount = 100
                             ))
      orderPayment2 ← * <~ OrderPayments.create(
                         Factories.orderPayment.copy(cordRef = order2.refNum,
                                                     paymentMethodId = creditCard.id,
                                                     amount = None))
      creditCardCharge2 ← * <~ CreditCardCharges.create(
                             CreditCardCharge(
                                 creditCardId = creditCard.id,
                                 orderPaymentId = orderPayment2.id,
                                 chargeId = "asd",
                                 state = CreditCardCharge.FullCapture,
                                 amount = 1000000
                             ))
      order  ← * <~ Orders.update(order, order.copy(state = Order.FulfillmentStarted))
      order  ← * <~ Orders.update(order, order.copy(state = Order.Shipped))
      order2 ← * <~ Orders.update(order2, order2.copy(state = Order.FulfillmentStarted))
      order2 ← * <~ Orders.update(order2, order2.copy(state = Order.Shipped))

    } yield (order, orderPayment, customer2, creditCardCharge1, creditCardCharge2)).gimme
  }
}
