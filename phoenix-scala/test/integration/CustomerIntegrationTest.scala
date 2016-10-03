import java.time.Instant
import java.time.temporal.ChronoUnit

import cats.implicits._
import com.stripe.exception.CardException
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.CustomerFailures._
import failures.StripeFailures.StripeFailure
import failures.{GeneralFailure, NotFoundFailure404}
import models.cord.OrderPayments.scope._
import models.cord._
import models.customer._
import models.location.{Addresses, Regions}
import models.payment.creditcard._
import models.shipping.Shipment.Shipped
import models.shipping.{Shipment, Shipments}
import models.traits.Originator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CustomerPayloads._
import payloads.PaymentPayloads._
import responses.CreditCardsResponse.{Root ⇒ CardResponse}
import responses.CustomerResponse.Root
import responses.cord.CartResponse
import responses.{CreditCardsResponse, CustomerResponse}
import services.Result
import services.carts.CartPaymentUpdater
import slick.driver.PostgresDriver.api._
import testutils.Extensions._
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixPublicApi}
import testutils.fixtures.BakedFixtures
import utils.MockedApis
import utils.aliases.stripe.StripeCard
import utils.db._
import utils.jdbc._
import utils.seeds.Seeds.Factories

class CustomerIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixPublicApi
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
      val root = customersApi
        .create(CreateCustomerPayload(email = "test@example.com", name = "test".some))
        .as[Root]

      val created = Customers.mustFindById400(root.id).gimme
      created.id must === (root.id)
    }

    "fails if email is already in use" in new Customer_Seed {
      customersApi
        .create(CreateCustomerPayload(email = customer.email.value, name = "test".some))
        .mustFailWith400(CustomerEmailNotUnique)
    }
  }

  "GET /v1/customers/:customerId" - {
    "fetches customer info" in new Fixture {
      val customerRoot = CustomerResponse.build(customer, shippingRegion = region.some)
      customersApi(customer.id).get().as[Root] must === (customerRoot)
    }

    "fetches customer info without default address" in new Fixture {
      Addresses.filter(_.id === address.id).map(_.isDefaultShipping).update(false).gimme

      val customerRoot = CustomerResponse.build(customer)
      customersApi(customer.id).get().as[Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion" in new CreditCardFixture {
      val billRegion = Regions.findOneById(creditCard.regionId).gimme
      val root =
        CustomerResponse.build(customer, shippingRegion = region.some, billingRegion = billRegion)
      customersApi(customer.id).get().as[Root] must === (root)
    }

    "customer info shows valid billingRegion without default CreditCard" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).gimme

      val customerRoot = CustomerResponse.build(customer, shippingRegion = region.some)
      customersApi(customer.id).get().as[Root] must === (customerRoot)
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

        val customerRoot =
          CustomerResponse.build(customer.copy(phoneNumber = defaultPhoneNumber.some),
                                 shippingRegion = region)
        customersApi(customer.id).get().as[Root] must === (customerRoot)
      }

      "last shipping address" in {

        val phoneNumbers = Seq("1111111111", "2222222222")

        val defaultAddress = Factories.address.copy(isDefaultShipping = true, phoneNumber = None)

        def shippingAddresses(orders: Seq[(Order, String)]) = orders.map {
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
          val customerResponse = customersApi(customer.id).get().as[Root]
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
      def expectedResponse(lastOrderDays: Long) =
        CustomerResponse.build(customer,
                               shippingRegion = region.some,
                               lastOrderDays = lastOrderDays.some)

      customersApi(customer.id).get().as[Root] must === (expectedResponse(0))

      Orders.update(order, order.copy(placedAt = Instant.now.minus(1, ChronoUnit.DAYS))).gimme

      customersApi(customer.id).get().as[Root] must === (expectedResponse(1))
    }

    "ranking" - {
      "customer must be with rank" in new FixtureForRanking {

        // check that states used in sql still actual
        sqlu"UPDATE orders SET state = 'shipped' WHERE reference_number = ${order.refNum}".gimme
        sql"SELECT public.update_customers_ranking()".as[Boolean].gimme

        customersApi(customer.id).get().as[Root].rank must === (2.some)
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
      val root = customersApi(customer.id).cart().as[CartResponse]
      root.referenceNumber must === (cart.referenceNumber)

      Carts.findByCustomer(customer).gimme must have size 1
    }

    "creates cart if no present" in new Fixture {
      customersApi(customer.id).cart().as[CartResponse]

      Carts.findByCustomer(customer).gimme must have size 1
    }

    "returns 404 if customer not found" in new EmptyCustomerCart_Baked {
      customersApi(999).cart().mustFailWith404(NotFoundFailure404(Customer, 999))
    }
  }

  "PATCH /v1/customers/:customerId" - {
    "successfully updates customer attributes" in new Fixture {
      val payload = UpdateCustomerPayload(name = "John Doe".some,
                                          email = "newemail@example.org".some,
                                          phoneNumber = "555 555 55".some)
      (payload.name, payload.email, payload.phoneNumber) must !==(
          (customer.name, customer.email, customer.phoneNumber))

      val updated = customersApi(customer.id).update(payload).as[Root]
      (updated.name, updated.email, updated.phoneNumber) must === (
          (payload.name, payload.email, payload.phoneNumber))
    }

    "fails if email is already in use" in new Fixture {
      val newCustomer = customersApi
        .create(CreateCustomerPayload(email = "test@example.com", name = "test".some))
        .as[Root]

      customersApi(newCustomer.id)
        .update(UpdateCustomerPayload(email = customer.email))
        .mustFailWith400(CustomerEmailNotUnique)
    }
  }

  "POST /v1/customers/:customerId/activate" - {
    "fails if email is already in use by non-guest user" in new Fixture {
      val newCustomer = customersApi
        .create(CreateCustomerPayload(email = customer.email.value, isGuest = true.some))
        .as[Root]

      customersApi(newCustomer.id)
        .activate(ActivateCustomerPayload(name = "test"))
        .mustFailWith400(CustomerEmailNotUnique)
    }

    "successfully activates non-guest user" in new Fixture {
      val newCustomer = customersApi
        .create(CreateCustomerPayload(email = "guest@example.com", isGuest = true.some))
        .as[Root]

      customersApi(newCustomer.id).activate(ActivateCustomerPayload(name = "test")).mustBeOk()

      val created = Customers.findOneById(newCustomer.id).gimme.value
      CustomerResponse.build(created) must === (
          newCustomer.copy(name = "test".some, isGuest = false))
      created.isGuest must === (false)
    }
  }

  "POST /v1/customers/:customerId/disable" - {
    "toggles the isDisabled flag on a customer account" in new Fixture {
      customer.isDisabled must === (false)

      customersApi(customer.id).disable(ToggleCustomerDisabled(true)).as[Root].disabled must === (
          true)

      customersApi(customer.id).disable(ToggleCustomerDisabled(false)).as[Root].disabled must === (
          false)
    }

    "fails if customer not found" in new Fixture {
      customersApi(999)
        .disable(ToggleCustomerDisabled(true))
        .mustFailWith404(NotFoundFailure404(Customer, 999))
    }

    "disable already disabled account is ok (overwrite behaviour)" in new Fixture {
      val updated = Customers.update(customer, customer.copy(isDisabled = true)).gimme
      updated.isDisabled must === (true)

      val disabled = customersApi(customer.id).disable(ToggleCustomerDisabled(true)).as[Root]
      disabled.disabled must === (true)
    }
  }

  "POST /v1/customers/:customerId/blacklist" - {
    "toggles the isBlacklisted flag on a customer account" in new Fixture {
      customer.isBlacklisted must === (false)

      customersApi(customer.id)
        .blacklist(ToggleCustomerBlacklisted(true))
        .as[Root]
        .isBlacklisted must === (true)

      customersApi(customer.id)
        .blacklist(ToggleCustomerBlacklisted(false))
        .as[Root]
        .isBlacklisted must === (false)
    }

    "fails if customer not found" in new Fixture {
      customersApi(999)
        .blacklist(ToggleCustomerBlacklisted(true))
        .mustFailWith404(NotFoundFailure404(Customer, 999))
    }

    "blacklist already blacklisted account is ok (overwrite behaviour)" in new Fixture {
      customersApi(customer.id)
        .blacklist(ToggleCustomerBlacklisted(true))
        .as[Root]
        .isBlacklisted must === (true)

      customersApi(customer.id)
        .blacklist(ToggleCustomerBlacklisted(true))
        .as[Root]
        .isBlacklisted must === (true)
    }
  }

  "GET /v1/customers/:customerId/payment-methods/credit-cards" - {
    "shows customer's credit cards only in their wallet" in new CreditCardFixture {
      val deleted = CreditCards.create(creditCard.copy(id = 0, inWallet = false)).gimme

      val creditCards = customersApi(customer.id).payments.creditCards.get().as[Seq[CardResponse]]
      val ccRegion    = Regions.findOneById(creditCard.regionId).gimme.value

      creditCards must have size 1
      creditCards.head must === (CreditCardsResponse.build(creditCard, ccRegion))
      creditCards.head.id must !==(deleted.id)
    }
  }

  "POST /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId/default" - {
    "sets the isDefault flag on a credit card" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).gimme

      val ccResp = customersApi(customer.id).payments
        .creditCard(creditCard.id)
        .toggleDefault(ToggleDefaultCreditCard(isDefault = true))
        .as[CardResponse]

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

      val ccResp = customersApi(customer.id).payments
        .creditCard(nonDefault.id)
        .toggleDefault(ToggleDefaultCreditCard(isDefault = true))
        .as[CardResponse]

      val (prevDefault, currDefault) =
        (CreditCards.refresh(default).gimme, CreditCards.refresh(nonDefault).gimme)

      ccResp.isDefault mustBe true
      ccResp.id must === (currDefault.id)
      prevDefault.isDefault mustBe false
    }

    "fails when the credit card doesn't exist" in new Fixture {
      customersApi(customer.id).payments
        .creditCard(99)
        .toggleDefault(ToggleDefaultCreditCard(isDefault = true))
        .mustFailWith404(NotFoundFailure404(CreditCard, 99))
    }
  }

  "DELETE /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId" - {
    "deletes successfully if the card exists" in new CreditCardFixture {
      customersApi(customer.id).payments.creditCard(creditCard.id).delete().mustBeEmpty()

      val deleted = CreditCards.findOneById(creditCard.id).gimme.value
      deleted.inWallet must === (false)
      deleted.deletedAt mustBe 'defined
    }
  }

  "PATCH /v1/customers/:customerId/payment-methods/credit-cards/:creditCardId" - {
    "when successful" - {
      "removes the original card from wallet" in new CreditCardFixture {
        val root = customersApi(customer.id).payments
          .creditCard(creditCard.id)
          .edit(EditCreditCard(holderName = "Bob".some))
          .as[CardResponse]

        root.id must !==(creditCard.id)
        root.inWallet mustBe true
      }

      "creates a new version of the edited card in the wallet" in new CreditCardFixture {
        val payload = EditCreditCard(holderName = "Bob".some)
        customersApi(customer.id).payments.creditCard(creditCard.id).edit(payload).mustBeOk()

        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).gimme.toList

        newVersion.inWallet mustBe true
        newVersion.isDefault must === (creditCard.isDefault)
      }

      "updates the customer's cart to use the new version" in new CreditCardFixture {
        CartPaymentUpdater
          .addCreditCard(Originator(storeAdmin), creditCard.id, Some(cart.refNum))
          .gimme

        val payload = EditCreditCard(holderName = "Bob".some)
        customersApi(customer.id).payments.creditCard(creditCard.id).edit(payload).mustBeOk()

        val (pmt :: Nil)        = OrderPayments.filter(_.cordRef === cart.refNum).creditCards.gimme.toList
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).gimme.toList

        pmt.amount mustBe 'empty
        pmt.isCreditCard mustBe true
        pmt.paymentMethodId must === (newVersion.id)
      }

      "copies an existing address book entry to the creditCard" in new CreditCardFixture {
        val payload = EditCreditCard(holderName = "Bob".some, addressId = address.id.some)
        customersApi(customer.id).payments.creditCard(creditCard.id).edit(payload).mustBeOk()

        val (newVersion :: Nil) =
          CreditCards.filter(_.parentId === creditCard.id).result.gimme.toList
        val numAddresses = Addresses.length.result.gimme

        numAddresses must === (1)
        (newVersion.zip, newVersion.regionId) must === ((address.zip, address.regionId))
      }

      "creates a new address book entry if a full address was given" in new CreditCardFixture {
        val addressPayload = CreateAddressPayload(name = "Home Office",
                                                  regionId = address.regionId + 1,
                                                  address1 = "3000 Coolio Dr",
                                                  city = "Seattle",
                                                  zip = "54321")
        val payload = EditCreditCard(holderName = "Bob".some, address = addressPayload.some)
        customersApi(customer.id).payments.creditCard(creditCard.id).edit(payload).mustBeOk()

        val (newVersion :: Nil) =
          CreditCards.filter(_.parentId === creditCard.id).result.gimme.toList
        val addresses  = Addresses.gimme
        val newAddress = addresses.last

        addresses must have size 2
        (newVersion.zip, newVersion.regionId) must === (("54321", address.regionId + 1))
        (newVersion.zip, newVersion.regionId) must === ((newAddress.zip, newAddress.regionId))
      }
    }

    "fails if the card cannot be found" in new CreditCardFixture {
      customersApi(customer.id).payments
        .creditCard(99)
        .edit(EditCreditCard())
        .mustFailWith404(NotFoundFailure404(CreditCard, 99))
    }

    "fails if the card is not inWallet" in new CreditCardFixture {
      customersApi(customer.id).payments.creditCard(creditCard.id).delete().mustBeEmpty()

      customersApi(customer.id).payments
        .creditCard(creditCard.id)
        .edit(EditCreditCard())
        .mustFailWith400(CannotUseInactiveCreditCard(creditCard))
    }

    "fails if the payload is invalid" in new CreditCardFixture {
      customersApi(customer.id).payments
        .creditCard(creditCard.id)
        .edit(EditCreditCard(holderName = "".some))
        .mustFailWithMessage("holderName must not be empty")
    }

    "fails if stripe returns an error" in new CreditCardFixture {
      val message = "Intentionally unrelated message"

      val exception = new CardException(message,
                                        "X_REQUEST_ID: 1",
                                        "invalid_expiry_year",
                                        "exp_year",
                                        null,
                                        null,
                                        null,
                                        null)

      when(stripeWrapperMock.updateCard(any(), any()))
        .thenReturn(Result.failure[StripeCard](StripeFailure(exception)))

      customersApi(customer.id).payments
        .creditCard(creditCard.id)
        .edit(EditCreditCard(expYear = 2000.some))
        .mustFailWithMessage(message)
    }
  }

  "POST /v1/public/send-password-reset" - {
    "Successfully creates password reset instance" in new Fixture {
      val email = customer.email.value

      publicApi.sendPasswordReset(ResetPasswordSend(email)).mustBeOk()

      val resetPw = CustomerPasswordResets.filter(_.email === email).one.gimme.value
      resetPw.state must === (CustomerPasswordReset.Initial)
      resetPw.email must === (email)
    }

    "re-send with new code if phoenix already send it but customer not activated it" in new Fixture {
      val email = customer.email.value
      val oldResetPw = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      publicApi.sendPasswordReset(ResetPasswordSend(email)).mustBeOk()
      val resetPw = CustomerPasswordResets.findActiveByEmail(email).one.gimme.value
      oldResetPw.code must !==(resetPw.code)
    }
  }

  "POST /v1/public/reset-password" - {
    "Successfully reset password" in new Fixture {
      val resetPw = CustomerPasswordResets
        .create(CustomerPasswordReset.optionFromCustomer(customer).value)
        .gimme

      publicApi.resetPassword(ResetPassword(code = resetPw.code, newPassword = "456")).mustBeOk()

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

      publicApi.resetPassword(ResetPassword(code = resetPw.code, newPassword = "456")).mustBeOk()

      publicApi
        .resetPassword(ResetPassword(code = resetPw.code, newPassword = "456"))
        .mustFailWith400(ResetPasswordCodeInvalid(resetPw.code))
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
