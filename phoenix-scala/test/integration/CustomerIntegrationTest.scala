import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import com.stripe.exception.CardException
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.CustomerFailures._
import failures.StripeFailures.StripeFailure
import failures.{GeneralFailure, NotFoundFailure404}
import models.cord.OrderPayments.scope._
import models.cord._
import models.account._
import models.customer._
import models.location.{Addresses, Regions}
import models.payment.creditcard._
import models.shipping.Shipment.Shipped
import models.shipping.{Shipment, Shipments}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import payloads.AddressPayloads.CreateAddressPayload
import payloads.UserPayloads._
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
import utils.MockedApis
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

  "POST /v1/customers" - {
    "successfully creates customer from payload" in {
      val response = POST(s"v1/customers",
                          CreateCustomerPayload(email = "test@example.com", name = Some("test")))

      response.status must === (StatusCodes.OK)

      val root    = response.as[CustomerResponse.Root]
      val created = Users.findOneByAccountId(root.id).run().futureValue.value
      created.id must === (root.id)
    }

    "fails if email is already in use" in new Customer_Seed {
      val response = POST(s"v1/customers",
                          CreateCustomerPayload(email = customer.email.value, name = Some("test")))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CustomerEmailNotUnique.description)
    }
  }

  "GET /v1/customers/:accountId" - {
    "fetches customer info" in new Fixture {
      val response = GET(s"v1/customers/${customer.accountId}")
      val customerRoot =
        CustomerResponse.build(customer, customerUser, shippingRegion = region.some)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "fetches customer info without default address" in new Fixture {
      Addresses.filter(_.id === address.id).map(_.isDefaultShipping).update(false).gimme
      val response     = GET(s"v1/customers/${customer.accountId}")
      val customerRoot = CustomerResponse.build(customer, customerUser)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion" in new CreditCardFixture {
      val billRegion = Regions.findOneById(creditCard.regionId).gimme

      val response = GET(s"v1/customers/${customer.accountId}")
      val customerRoot = CustomerResponse
        .build(customer, customerUser, shippingRegion = region.some, billingRegion = billRegion)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "customer info shows valid billingRegion without default CreditCard" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).gimme
      val response = GET(s"v1/customers/${customer.accountId}")
      val customerRoot =
        CustomerResponse.build(customer, customerUser, shippingRegion = region.some)

      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (customerRoot)
    }

    "empty phone number is resolved from" - {
      "default shipping address" in {
        val defaultPhoneNumber: String = "1111111111"

        val (customer, customerUser, region) = (for {
          account ← * <~ Accounts.create(Account())
          customer ← * <~ Users.create(
                        Factories.customer.copy(accountId = account.id, phoneNumber = None))
          customerUser ← * <~ CustomerUsers.create(
                            CustomerUser(accountId = account.id, userId = customer.id))
          address ← * <~ Addresses.create(
                       Factories.address.copy(accountId = customer.accountId,
                                              isDefaultShipping = true,
                                              phoneNumber = defaultPhoneNumber.some))
          region ← * <~ Regions.findOneById(address.regionId)
        } yield (customer, customerUser, region)).gimme

        val response = GET(s"v1/customers/${customer.accountId}")
        val customerRoot =
          CustomerResponse.build(customer.copy(phoneNumber = defaultPhoneNumber.some),
                                 customerUser,
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
          account ← * <~ Accounts.create(Account())
          customer ← * <~ Users.create(
                        Factories.customer.copy(accountId = account.id, phoneNumber = None))
          custUser ← * <~ CustomerUsers.create(
                        CustomerUser(userId = customer.id, accountId = account.id))
          address ← * <~ Addresses.create(defaultAddress.copy(accountId = customer.accountId))
          region  ← * <~ Regions.findOneById(address.regionId)
          cart1 ← * <~ Carts.create(
                     Cart(referenceNumber = "ABC-1", accountId = customer.accountId))
          order1 ← * <~ Orders.createFromCart(cart1)
          order1 ← * <~ Orders.update(order1, order1.copy(state = Order.FulfillmentStarted))
          order1 ← * <~ Orders.update(order1, order1.copy(state = Order.Shipped))
          cart2 ← * <~ Carts.create(
                     Cart(referenceNumber = "ABC-2", accountId = customer.accountId))
          order2 ← * <~ Orders.createFromCart(cart2)
          order2 ← Orders.update(order2, order2.copy(state = Order.FulfillmentStarted))
          order2 ← Orders.update(order2, order2.copy(state = Order.Shipped))
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
          val response = GET(s"v1/customers/${customer.accountId}")
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
      val expectedCustomer = CustomerResponse
        .build(customer, customerUser, shippingRegion = region.some, lastOrderDays = Some(0))

      val response = GET(s"v1/customers/${customer.accountId}")
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root] must === (expectedCustomer)

      Orders.update(order, order.copy(placedAt = Instant.now.minus(1, ChronoUnit.DAYS))).gimme

      val secondResponse = GET(s"v1/customers/${customer.accountId}")

      secondResponse.status must === (StatusCodes.OK)
      secondResponse.as[CustomerResponse.Root] must === (
          expectedCustomer.copy(lastOrderDays = Some(1l)))
    }

    "ranking" - {
      "customer must be with rank" in new FixtureForRanking {

        // check that states used in sql still actual
        sqlu"UPDATE orders SET state = 'shipped' WHERE reference_number = ${order.refNum}".gimme
        sql"SELECT public.update_customers_ranking()".as[Boolean].gimme

        val response = GET(s"v1/customers/${customer.accountId}")
        response.status must === (StatusCodes.OK)
        response.as[CustomerResponse.Root].rank must === (Some(2))
        val rank  = CustomersRanks.findById(customer.accountId).extract.result.head.gimme
        val rank2 = CustomersRanks.findById(customer2.accountId).extract.result.head.gimme
        rank.revenue must === (charge1.amount)
        rank2.revenue must === (charge2.amount)
        rank2.rank must === (Some(1))
      }
    }
  }

  "GET /v1/customers/:accountId/cart" - {
    "returns customer cart" in new EmptyCustomerCart_Baked {
      val response = GET(s"v1/customers/${customer.accountId}/cart")
      response.status must === (StatusCodes.OK)

      val root = response.as[CartResponse]
      root.referenceNumber must === (cart.referenceNumber)

      Carts.findByAccountId(customer.accountId).gimme must have size 1
    }

    "creates cart if no present" in new Fixture {
      val response = GET(s"v1/customers/${customer.accountId}/cart")
      response.status must === (StatusCodes.OK)

      val root = response.as[CartResponse]

      Carts.findByAccountId(customer.accountId).gimme must have size 1
    }

    "returns 404 if customer not found" in new EmptyCustomerCart_Baked {
      val response = GET(s"v1/customers/999/cart")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, 999).description)
    }
  }

  "PATCH /v1/customers/:accountId" - {
    "successfully updates customer attributes" in new Fixture {
      val payload = UpdateCustomerPayload(name = "John Doe".some,
                                          email = "newemail@example.org".some,
                                          phoneNumber = "555 555 55".some)
      (payload.name, payload.email, payload.phoneNumber) must !==(
          (customer.name, customer.email, customer.phoneNumber))

      val response = PATCH(s"v1/customers/${customer.accountId}", payload)
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

  "POST /v1/customers/:accountId/activate" - {
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

      val created         = Users.findOneByAccountId(root.id).gimme.value
      val createdCustUser = CustomerUsers.findOneByAccountId(root.id).gimme.value

      CustomerResponse.build(created, createdCustUser) must === (
          root.copy(name = Some("test"), isGuest = false))
      createdCustUser.isGuest must === (false)
    }
  }

  "POST /v1/customers/:accountId/disable" - {
    "toggles the isDisabled flag on a customer account" in new Fixture {
      customer.isDisabled must === (false)

      val disableResp =
        POST(s"v1/customers/${customer.accountId}/disable", ToggleUserDisabled(true))
      disableResp.status must === (StatusCodes.OK)
      disableResp.as[CustomerResponse.Root].disabled must === (true)

      val enableResp =
        POST(s"v1/customers/${customer.accountId}/disable", ToggleUserDisabled(false))
      enableResp.status must === (StatusCodes.OK)
      enableResp.as[CustomerResponse.Root].disabled must === (false)
    }

    "fails if customer not found" in new Fixture {
      val response = POST(s"v1/customers/999/disable", ToggleUserDisabled(true))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, 999).description)
    }

    "disable already disabled account is ok (overwrite behaviour)" in new Fixture {
      val updated = Users.update(customer, customer.copy(isDisabled = true)).gimme
      updated.isDisabled must === (true)

      val response = POST(s"v1/customers/${customer.accountId}/disable", ToggleUserDisabled(true))
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root].disabled must === (true)
    }
  }

  "POST /v1/customers/:accountId/blacklist" - {
    "toggles the isBlacklisted flag on a customer account" in new Fixture {
      customer.isBlacklisted must === (false)

      val responseAdd =
        POST(s"v1/customers/${customer.accountId}/blacklist", ToggleUserBlacklisted(true))
      responseAdd.status must === (StatusCodes.OK)
      responseAdd.as[CustomerResponse.Root].isBlacklisted must === (true)

      val responseRemove =
        POST(s"v1/customers/${customer.accountId}/blacklist", ToggleUserBlacklisted(false))
      responseRemove.status must === (StatusCodes.OK)
      responseRemove.as[CustomerResponse.Root].isBlacklisted must === (false)
    }

    "fails if customer not found" in new Fixture {
      val response = POST(s"v1/customers/999/blacklist", ToggleUserBlacklisted(true))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(User, 999).description)
    }

    "blacklist already blacklisted account is ok (overwrite behaviour)" in new Fixture {
      val updated = Users.update(customer, customer.copy(isBlacklisted = true)).gimme
      updated.isBlacklisted must === (true)

      val response =
        POST(s"v1/customers/${customer.accountId}/blacklist", ToggleUserBlacklisted(true))
      response.status must === (StatusCodes.OK)
      response.as[CustomerResponse.Root].isBlacklisted must === (true)
    }
  }

  "GET /v1/customers/:accountId/payment-methods/credit-cards" - {
    "shows customer's credit cards only in their wallet" in new CreditCardFixture {
      val deleted = CreditCards.create(creditCard.copy(id = 0, inWallet = false)).gimme

      val response = GET(s"v1/customers/${customer.accountId}/payment-methods/credit-cards")
      val cc       = response.as[Seq[CardResponse]]
      val ccRegion = Regions.findOneById(creditCard.regionId).gimme.value

      response.status must === (StatusCodes.OK)
      cc must have size 1
      cc.head must === (responses.CreditCardsResponse.build(creditCard, ccRegion))
      cc.head.id must !==(deleted.id)
    }
  }

  "POST /v1/customers/:accountId/payment-methods/credit-cards/:creditCardId/default" - {
    "sets the isDefault flag on a credit card" in new CreditCardFixture {
      CreditCards.filter(_.id === creditCard.id).map(_.isDefault).update(false).gimme

      val payload = ToggleDefaultCreditCard(isDefault = true)
      val response = POST(
          s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}/default",
          payload)

      response.status must === (StatusCodes.OK)
      val ccResp = response.as[CardResponse]
      ccResp.isDefault mustBe true
      ccResp.id must === (creditCard.id)
    }

    "successfully replaces an existing default credit card" in new Fixture {
      val default = CreditCards
        .create(Factories.creditCard.copy(isDefault = true, accountId = customer.accountId))
        .gimme
      val nonDefault = CreditCards
        .create(Factories.creditCard.copy(isDefault = false, accountId = customer.accountId))
        .gimme

      val payload = ToggleDefaultCreditCard(isDefault = true)
      val response = POST(
          s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${nonDefault.id}/default",
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
        POST(s"v1/customers/${customer.accountId}/payment-methods/credit-cards/99/default",
             payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
    }
  }

  "DELETE /v1/customers/:accountId/payment-methods/credit-cards/:creditCardId" - {
    "deletes successfully if the card exists" in new CreditCardFixture {
      val response =
        DELETE(s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}")
      val deleted = CreditCards.findOneById(creditCard.id).gimme.value

      response.status must === (StatusCodes.NoContent)
      deleted.inWallet must === (false)
      deleted.deletedAt mustBe 'defined
    }
  }

  "PATCH /v1/customers/:accountId/payment-methods/credit-cards/:creditCardId" - {
    "when successful" - {
      "removes the original card from wallet" in new CreditCardFixture {
        val payload = EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(
            s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
            payload)
        val inactive = CreditCards.findOneById(creditCard.id).gimme.value

        response.status must === (StatusCodes.OK)

        val root = response.as[CardResponse]
        root.id must !==(creditCard.id)
        root.inWallet mustBe true
      }

      "creates a new version of the edited card in the wallet" in new CreditCardFixture {
        val payload = EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(
            s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
            payload)
        val (newVersion :: Nil) = CreditCards.filter(_.parentId === creditCard.id).gimme.toList

        response.status must === (StatusCodes.OK)
        newVersion.inWallet mustBe true
        newVersion.isDefault must === (creditCard.isDefault)
      }

      "updates the customer's cart to use the new version" in new CreditCardFixture {
        CartPaymentUpdater.addCreditCard(storeAdmin, creditCard.id, Some(cart.refNum)).gimme

        val payload = EditCreditCard(holderName = Some("Bob"))
        val response = PATCH(
            s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
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
        val response = PATCH(
            s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
            payload)
        val (newVersion :: Nil) =
          CreditCards.filter(_.parentId === creditCard.id).result.gimme.toList
        val numAddresses = Addresses.length.result.gimme

        response.status must === (StatusCodes.OK)
        numAddresses must === (1)
        (newVersion.zip, newVersion.regionId) must === ((address.zip, address.regionId))
      }

      "creates a new address book entry if a full address was given" in new CreditCardFixture {
        val payload = EditCreditCard(holderName = Some("Bob"),
                                     address =
                                       CreateAddressPayload(name = "Home Office",
                                                            regionId = address.regionId + 1,
                                                            address1 = "3000 Coolio Dr",
                                                            city = "Seattle",
                                                            zip = "54321").some)
        val response = PATCH(
            s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
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
      val payload = EditCreditCard
      val response =
        PATCH(s"v1/customers/${customer.accountId}/payment-methods/credit-cards/99", payload)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 99).description)
    }

    "fails if the card is not inWallet" in new CreditCardFixture {
      CreditCardManager.deleteCreditCard(customer.accountId, creditCard.id, Some(storeAdmin)).gimme

      val response =
        PATCH(s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
              EditCreditCard)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (CannotUseInactiveCreditCard(creditCard).description)
    }

    "fails if the payload is invalid" in new CreditCardFixture {
      val payload = EditCreditCard(holderName = "".some)
      val response =
        PATCH(s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
              payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === ("holderName must not be empty")
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

      val payload = EditCreditCard(expYear = Some(2000))
      val response =
        PATCH(s"v1/customers/${customer.accountId}/payment-methods/credit-cards/${creditCard.id}",
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

      val resetPw = UserPasswordResets.filter(_.email === email).one.gimme.value
      resetPw.state must === (UserPasswordReset.Initial)
      resetPw.email must === (email)
    }

    "re-send with new code if phoenix already send it but customer not activated it" in new Fixture {
      val email = customer.email.value
      val oldResetPw =
        UserPasswordResets.create(UserPasswordReset.optionFromUser(customer).value).gimme

      val response = POST(s"v1/public/send-password-reset", ResetPasswordSend(email))
      response.status must === (StatusCodes.OK)
      val resetPw = UserPasswordResets.findActiveByEmail(email).one.gimme.value
      oldResetPw.code must !==(resetPw.code)
    }
  }

  "POST /v1/public/reset-password" - {
    "Successfully reset password" in new Fixture {
      val resetPw =
        UserPasswordResets.create(UserPasswordReset.optionFromUser(customer).value).gimme

      val response =
        POST(s"v1/public/reset-password", ResetPassword(code = resetPw.code, newPassword = "456"))
      response.status must === (StatusCodes.OK)
      val updatedAccessMethod =
        AccountAccessMethods.findOneByAccountIdAndName(customer.accountId, "login").gimme
      updatedAccessMethod.value.hashedPassword must !==(accessMethod.hashedPassword)

      val newResetPw = UserPasswordResets.mustFindById404(resetPw.id).gimme
      newResetPw.state must === (UserPasswordReset.PasswordRestored)
      newResetPw.activatedAt mustBe 'defined
    }

    "fails if customer reset code is already used" in new Fixture {
      val resetPw =
        UserPasswordResets.create(UserPasswordReset.optionFromUser(customer).value).gimme

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
    val creditCard =
      CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId)).gimme
  }

  trait FixtureForRanking extends EmptyCustomerCart_Baked with CreditCardFixture {
    val (order, orderPayment, customer2, charge1, charge2) = (for {
      account ← * <~ Accounts.create(Account())
      customer2 ← * <~ Users.create(
                     Factories.customer.copy(accountId = account.id,
                                             email = "second@example.org".some,
                                             name = "second".some))
      custUser2 ← * <~ CustomerUsers.create(
                     CustomerUser(userId = customer2.id, accountId = account.id))
      cart2  ← * <~ Carts.create(Cart(accountId = customer2.accountId, referenceNumber = "ABC-456"))
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
