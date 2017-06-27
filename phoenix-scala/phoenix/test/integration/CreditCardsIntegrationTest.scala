import java.time.ZonedDateTime

import cats.implicits._
import core.failures.{GeneralFailure, NotFoundFailure400, NotFoundFailure404}
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers ⇒ m, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import phoenix.models.account._
import phoenix.models.location.{Addresses, Region}
import phoenix.models.payment.creditcard.{BillingAddress, CreditCard, CreditCards}
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.PaymentPayloads.CreateCreditCardFromTokenPayload
import phoenix.responses.CreditCardsResponse
import phoenix.responses.CreditCardsResponse.Root
import phoenix.utils.TestStripeSupport
import phoenix.utils.aliases.stripe.StripeCustomer
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.apis._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.PaymentFixtures.CreditCardsFixture
import testutils.fixtures.api.ApiFixtureHelpers
import core.db.{when => ifM, _}

class CreditCardsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with ApiFixtureHelpers
    with DefaultJwtAdminAuth
    with MockitoSugar
    with BakedFixtures
    with BeforeAndAfterEach
    with CreditCardsFixture {

  override def beforeEach(): Unit =
    initStripeApiMock(stripeWrapperMock)

  "POST /v1/customers/:id/payment-methods/credit-cards (admin auth)" - {
    "creates a new credit card" in {
      val customer = api_newCustomer()

      customersApi(customer.id).payments.creditCards.create(ccPayload).mustBeOk()
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer.email.value))

      val cc = CreditCards.result.gimme.onlyElement

      val expected = CreditCard(
        id = 0,
        gatewayCustomerId = stripeCustomer.getId,
        gatewayCardId = stripeCard.getId,
        accountId = customer.id,
        address = BillingAddress(name = theAddress.name,
                                 address1 = theAddress.address1,
                                 address2 = theAddress.address2,
                                 city = theAddress.city,
                                 zip = theAddress.zip,
                                 regionId = theAddress.regionId),
        brand = "Mona Visa",
        holderName = "Leo",
        lastFour = "1234",
        expMonth = 1,
        expYear = expYear,
        createdAt = cc.createdAt
      )

      cc.copy(id = expected.id) must === (expected)

      // With existing Stripe customer
      Mockito.clearInvocations(stripeWrapperMock)

      customersApi(customer.id).payments.creditCards.create(ccPayload).mustBeOk()

      Mockito.verify(stripeWrapperMock).findCustomer(stripeCustomer.getId)
      Mockito.verify(stripeWrapperMock).createCard(m.eq(stripeCustomer), m.any())
      Mockito.verify(stripeWrapperMock, never()).createCustomer(m.any())
    }

    "creates cards for different customers correctly" in {
      val (customer1, customer2) = (api_newCustomer(), api_newCustomer())

      customersApi(customer1.id).payments.creditCards
        .create(ccPayload.copy(token = "tok_1"))
        .mustBeOk()

      val stripeCustomer2 = newStripeCustomer
      when(stripeWrapperMock.createCustomer(m.any())).thenReturn(Result.good(stripeCustomer2))
      customersApi(customer2.id).payments.creditCards
        .create(ccPayload.copy(token = "tok_2"))
        .mustBeOk()

      Mockito
        .verify(stripeWrapperMock)
        .createCustomer(customerSourceMap(customer1.email.value, "tok_1"))
      Mockito
        .verify(stripeWrapperMock)
        .createCustomer(customerSourceMap(customer2.email.value, "tok_2"))

      CreditCards.map(_.accountId).result.gimme must contain allOf (customer1.id, customer2.id)
    }

    "does not create a new address if it isn't new" in {
      customersApi(api_newCustomer().id).payments.creditCards.create(ccPayload).mustBeOk()
      Addresses.result.headOption.gimme must not be defined
    }

    "creates address if it's new" in {
      val accountId = api_newCustomer().id
      customersApi(accountId).payments.creditCards
        .create(ccPayload.copy(addressIsNew = true))
        .mustBeOk()
      Addresses.result.headOption.gimme.value.copy(id = theAddress.id) must === (
        theAddress.copy(accountId = accountId))
    }

    "errors 404 if wrong customer.accountId" in {
      customersApi(666).payments.creditCards
        .create(ccPayload)
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

    "errors 400 if wrong credit card token" in {
      when(stripeWrapperMock.createCustomer(m.any()))
        .thenReturn(Result.failure[StripeCustomer](GeneralFailure("BAD-TOKEN")))

      customersApi(api_newCustomer().id).payments.creditCards
        .create(ccPayload)
        .mustFailWithMessage("BAD-TOKEN")
    }

    "errors 400 if wrong region id" in new Customer_Seed {
      val wrongRegionIdPayload =
        ccPayload.copy(billingAddress = theAddressPayload.copy(regionId = -1))

      customersApi(customer.accountId).payments.creditCards
        .create(wrongRegionIdPayload)
        .mustFailWith400(NotFoundFailure400(Region, -1))
    }

    "validates payload" in {
      val validationErrors = crookedPayload.validate.toEither.leftVal.toList.map(_.description)

      customersApi(666).payments.creditCards
        .create(crookedPayload)
        .mustFailWithMessage(validationErrors: _*)
    }
  }

  "DELETE /v1/customers/:custId/payment-methods/credit-cards/:cardId (admin auth)" - {
    "deletes specified card" in {
      val customer = api_newCustomer()

      val ccResp1 = customersApi(customer.id).payments.creditCards.create(ccPayload).as[Root]

      val stripeCard2 = newStripeCard
      when(stripeWrapperMock.createCard(m.any(), m.any())).thenReturn(Result.good(stripeCard2))
      when(stripeWrapperMock.findCardByCustomerId(stripeCustomer.getId, stripeCard2.getId))
        .thenReturn(Result.good(stripeCard2))

      val ccResp2 = customersApi(customer.id).payments.creditCards.create(ccPayload).as[Root]

      val allCcs = customersApi(customer.id).payments.creditCards.get().as[Seq[Root]]
      allCcs must contain theSameElementsAs Seq(ccResp1, ccResp2)

      customersApi(customer.id).payments.creditCard(ccResp2.id).delete().mustBeEmpty()
      verify(stripeWrapperMock).deleteCard(m.argThat(cardStripeIdMatches(stripeCard2.getId)))

      customersApi(customer.id).payments.creditCards.get().as[Seq[Root]] must === (Seq(ccResp1))
    }

    "errors 404 if customer not found" in {
      customersApi(666).payments
        .creditCard(777)
        .delete()
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

    "errors 404 if card not found" in {
      customersApi(api_newCustomer().id).payments
        .creditCard(666)
        .delete()
        .mustFailWith404(NotFoundFailure404(CreditCard, 666))
    }
  }

  "GET /v1/my/payment-methods/credit-cards" - {
    "returns valid phone number" in withRandomCustomerAuth { implicit auth ⇒
      val testPhoneNumber = "1234567890"
      val payloadWithPhoneNumber =
        ccPayload.copy(billingAddress = ccPayload.billingAddress.copy(phoneNumber = testPhoneNumber.some))
      storefrontPaymentsApi.creditCards.create(payloadWithPhoneNumber).mustBeOk()

      storefrontPaymentsApi.creditCards
        .get()
        .as[Seq[CreditCardsResponse.Root]]
        .head
        .address
        .phoneNumber must === (testPhoneNumber.some)
    }
  }

  "POST /v1/my/payment-methods/credit-cards (customer auth)" - {
    "creates a new credit card" in withRandomCustomerAuth { implicit auth ⇒
      // No Stripe customer yet
      storefrontPaymentsApi.creditCards.create(ccPayload).mustBeOk()
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(auth.loginData.email))

      val cc: CreditCard = CreditCards.result.gimme.onlyElement

      val expected = CreditCard(
        id = 0,
        gatewayCustomerId = stripeCustomer.getId,
        accountId = auth.customerId,
        address = BillingAddress(name = theAddress.name,
                                 address1 = theAddress.address1,
                                 address2 = theAddress.address2,
                                 city = theAddress.city,
                                 zip = theAddress.zip,
                                 regionId = theAddress.regionId),
        holderName = "Leo",
        brand = "Mona Visa",
        lastFour = "1234",
        expMonth = 1,
        expYear = expYear,
        gatewayCardId = stripeCard.getId,
        createdAt = cc.createdAt
      )

      cc.copy(id = expected.id) must === (expected)

      // With existing Stripe customer
      Mockito.clearInvocations(stripeWrapperMock)

      storefrontPaymentsApi.creditCards.create(ccPayload).mustBeOk()

      Mockito.verify(stripeWrapperMock).findCustomer(stripeCustomer.getId)
      Mockito.verify(stripeWrapperMock).createCard(m.eq(stripeCustomer), m.any())
      Mockito.verify(stripeWrapperMock, never()).createCustomer(m.any())
    }

    "creates cards for different customers correctly" in {
      val (id1, email1) = withRandomCustomerAuth { implicit auth ⇒
        storefrontPaymentsApi.creditCards.create(ccPayload).mustBeOk()
        (auth.customerId, auth.loginData.email)
      }

      val (id2, email2) = withRandomCustomerAuth { implicit auth ⇒
        storefrontPaymentsApi.creditCards.create(ccPayload).mustBeOk()
        (auth.customerId, auth.loginData.email)
      }

      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(email1))
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(email2))

      CreditCards.map(_.accountId).result.gimme must contain allOf (id1, id2)
    }

    "does not create a new address if it isn't new" in withRandomCustomerAuth { implicit auth ⇒
      storefrontPaymentsApi.creditCards.create(ccPayload).mustBeOk()
      Addresses.result.headOption.gimme must not be defined
    }

    "creates address if it's new" in withRandomCustomerAuth { implicit auth ⇒
      storefrontPaymentsApi.creditCards.create(ccPayload.copy(addressIsNew = true)).mustBeOk()
      Addresses.result.headOption.gimme.value.copy(id = theAddress.id) must === (
        theAddress.copy(accountId = auth.customerId))
    }

    "errors 400 if wrong credit card token" in withRandomCustomerAuth { implicit auth ⇒
      when(stripeWrapperMock.createCustomer(m.any()))
        .thenReturn(Result.failure[StripeCustomer](GeneralFailure("BAD-TOKEN")))

      storefrontPaymentsApi.creditCards.create(ccPayload).mustFailWithMessage("BAD-TOKEN")
    }

    "errors 400 if wrong region id" in withRandomCustomerAuth { implicit auth ⇒
      val payload = ccPayload.copy(billingAddress = theAddressPayload.copy(regionId = -1))
      storefrontPaymentsApi.creditCards
        .create(payload)
        .mustFailWith400(NotFoundFailure400(Region, -1))
    }

    "validates payload" in withRandomCustomerAuth { implicit auth ⇒
      val validationErrors = crookedPayload.validate.toEither.leftVal.toList.map(_.description)

      storefrontPaymentsApi.creditCards
        .create(crookedPayload)
        .mustFailWithMessage(validationErrors: _*)
    }
  }

  private def customerSourceMap(customerEmail: String, token: String = tokenStripeId) =
    Map("description" → "FoxCommerce", "email" → customerEmail, "source" → token)

}
