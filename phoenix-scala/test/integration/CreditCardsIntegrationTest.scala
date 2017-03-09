import java.time.ZonedDateTime

import cats.implicits._
import failures.{GeneralFailure, NotFoundFailure400, NotFoundFailure404}
import models.account._
import models.location.{Addresses, Region}
import models.payment.creditcard.{BillingAddress, CreditCard, CreditCards}
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers ⇒ m, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import payloads.AddressPayloads.CreateAddressPayload
import payloads.PaymentPayloads.CreateCreditCardFromTokenPayload
import responses.CreditCardsResponse
import responses.CreditCardsResponse.Root
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.TestStripeSupport
import utils.aliases.stripe.StripeCustomer
import utils.seeds.Factories
import utils.db._

class CreditCardsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    initStripeApiMock(stripeWrapperMock)
  }

  val theAddress = Factories.address.copy(id = 1, accountId = 2, isDefaultShipping = false)
  val expYear    = ZonedDateTime.now.getYear + 3

  val theAddressPayload = CreateAddressPayload(name = theAddress.name,
                                               address1 = theAddress.address1,
                                               address2 = theAddress.address2,
                                               zip = theAddress.zip,
                                               city = theAddress.city,
                                               regionId = theAddress.regionId,
                                               phoneNumber = theAddress.phoneNumber)

  val tokenStripeId = s"tok_${TestStripeSupport.randomStripeishId}"

  val thePayload = CreateCreditCardFromTokenPayload(token = tokenStripeId,
                                                    lastFour = "1234",
                                                    expMonth = 1,
                                                    expYear = expYear,
                                                    brand = "Mona Visa",
                                                    holderName = "Leo",
                                                    addressIsNew = false,
                                                    billingAddress = theAddressPayload)

  val crookedAddressPayload = CreateAddressPayload(name = "",
                                                   address1 = "",
                                                   address2 = "".some,
                                                   zip = "",
                                                   regionId = -1,
                                                   city = "",
                                                   phoneNumber = "".some)
  val crookedPayload = CreateCreditCardFromTokenPayload(token = "",
                                                        lastFour = "",
                                                        expMonth = 666,
                                                        expYear = 777,
                                                        brand = "",
                                                        holderName = "",
                                                        addressIsNew = false,
                                                        billingAddress = crookedAddressPayload)

  "POST /v1/customers/:id/payment-methods/credit-cards (admin auth)" - {
    "creates a new credit card" in new Customer_Seed {
      customersApi(customer.accountId).payments.creditCards.create(thePayload).mustBeOk()
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer))

      val cc = CreditCards.result.gimme.onlyElement

      val expected = CreditCard(id = 1,
                                gatewayCustomerId = stripeCustomer.getId,
                                gatewayCardId = stripeCard.getId,
                                accountId = customer.accountId,
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
                                createdAt = cc.createdAt)

      cc must === (expected)

      // With existing Stripe customer
      Mockito.clearInvocations(stripeWrapperMock)

      customersApi(customer.accountId).payments.creditCards.create(thePayload).mustBeOk()

      Mockito.verify(stripeWrapperMock).findCustomer(stripeCustomer.getId)
      Mockito.verify(stripeWrapperMock).createCard(m.eq(stripeCustomer), m.any())
      Mockito.verify(stripeWrapperMock, never()).createCustomer(m.any())
    }

    "creates cards for different customers correctly" in {
      val customer1 = new Customer_Seed {}.customer
      val account2  = Accounts.create(Account()).gimme
      val customer2 = Users
        .create(Factories.customer.copy(accountId = account2.id, email = "another@gmail.com".some))
        .gimme

      customersApi(customer1.accountId).payments.creditCards
        .create(thePayload.copy(token = "tok_1"))
        .mustBeOk()

      val stripeCustomer2 = newStripeCustomer
      when(stripeWrapperMock.createCustomer(m.any())).thenReturn(Result.good(stripeCustomer2))
      customersApi(customer2.id).payments.creditCards
        .create(thePayload.copy(token = "tok_2"))
        .mustBeOk()

      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer1, "tok_1"))
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer2, "tok_2"))

      val ccCustomerIds = CreditCards.map(_.accountId).result.gimme
      ccCustomerIds must contain allOf (customer1.id, customer2.id)
    }

    "does not create a new address if it isn't new" in new Customer_Seed {
      customersApi(customer.accountId).payments.creditCards.create(thePayload).mustBeOk()
      Addresses.result.headOption.gimme must not be defined
    }

    "creates address if it's new" in new StoreAdmin_Seed with Customer_Seed {
      customersApi(customer.accountId).payments.creditCards
        .create(thePayload.copy(addressIsNew = true))
        .mustBeOk()
      Addresses.result.headOption.gimme.value must === (theAddress)
    }

    "errors 404 if wrong customer.accountId" in {
      customersApi(666).payments.creditCards
        .create(thePayload)
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

    "errors 400 if wrong credit card token" in new Customer_Seed {
      when(stripeWrapperMock.createCustomer(m.any()))
        .thenReturn(Result.failure[StripeCustomer](GeneralFailure("BAD-TOKEN")))
      customersApi(customer.accountId).payments.creditCards
        .create(thePayload)
        .mustFailWithMessage("BAD-TOKEN")
    }

    "errors 400 if wrong region id" in new Customer_Seed {
      val wrongRegionIdPayload =
        thePayload.copy(billingAddress = theAddressPayload.copy(regionId = -1))

      customersApi(customer.accountId).payments.creditCards
        .create(wrongRegionIdPayload)
        .mustFailWith400(NotFoundFailure400(Region, -1))
    }

    "validates payload" in {
      val validationErrors = crookedPayload.validate.toXor.leftVal.toList.map(_.description)

      customersApi(666).payments.creditCards
        .create(crookedPayload)
        .mustFailWithMessage(validationErrors: _*)
    }
  }

  "DELETE /v1/customers/:custId/payment-methods/credit-cards/:cardId (admin auth)" - {
    "deletes specified card" in new Customer_Seed {
      val ccResp1 =
        customersApi(customer.accountId).payments.creditCards.create(thePayload).as[Root]

      val stripeCard2 = newStripeCard
      when(stripeWrapperMock.createCard(m.any(), m.any())).thenReturn(Result.good(stripeCard2))
      when(stripeWrapperMock.findCardByCustomerId(stripeCustomer.getId, stripeCard2.getId))
        .thenReturn(Result.good(stripeCard2))

      val ccResp2 =
        customersApi(customer.accountId).payments.creditCards.create(thePayload).as[Root]

      val allCcs = customersApi(customer.accountId).payments.creditCards.get().as[Seq[Root]]
      allCcs must contain theSameElementsAs Seq(ccResp1, ccResp2)

      customersApi(customer.accountId).payments.creditCard(ccResp2.id).delete().mustBeEmpty()
      verify(stripeWrapperMock).deleteCard(m.argThat(cardStripeIdMatches(stripeCard2.getId)))

      customersApi(customer.accountId).payments.creditCards.get().as[Seq[Root]] must === (
          Seq(ccResp1))
    }

    "errors 404 if customer not found" in {
      customersApi(666).payments
        .creditCard(777)
        .delete()
        .mustFailWith404(NotFoundFailure404(User, 666))
    }

    "errors 404 if card not found" in new Customer_Seed {
      customersApi(customer.accountId).payments
        .creditCard(666)
        .delete()
        .mustFailWith404(NotFoundFailure404(CreditCard, 666))
    }
  }

  "GET /v1/my/payment-methods/credit-cards" - {
    "returns valid phone number" in new StoreAdmin_Seed with Customer_Seed {
      val testPhoneNumber = "1234567890"
      val payloadWithPhoneNumber = thePayload.copy(
          billingAddress = thePayload.billingAddress.copy(phoneNumber = testPhoneNumber.some))
      POST("v1/my/payment-methods/credit-cards", payloadWithPhoneNumber).mustBeOk()

      val ccs = GET("v1/my/payment-methods/credit-cards").as[Seq[CreditCardsResponse.Root]]
      ccs.head.address.phoneNumber must === (testPhoneNumber.some)
    }

  }

  "POST /v1/my/payment-methods/credit-cards (customer auth)" - {
    "creates a new credit card" in new StoreAdmin_Seed with Customer_Seed {
      // No Stripe customer yet
      POST("v1/my/payment-methods/credit-cards", thePayload).mustBeOk()
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer))

      val cc: CreditCard = CreditCards.result.gimme.onlyElement

      val expected = CreditCard(id = 1,
                                gatewayCustomerId = stripeCustomer.getId,
                                accountId = customer.accountId,
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
                                createdAt = cc.createdAt)

      cc must === (expected)

      // With existing Stripe customer
      Mockito.clearInvocations(stripeWrapperMock)

      POST("v1/my/payment-methods/credit-cards", thePayload).mustBeOk()

      Mockito.verify(stripeWrapperMock).findCustomer(stripeCustomer.getId)
      Mockito.verify(stripeWrapperMock).createCard(m.eq(stripeCustomer), m.any())
      Mockito.verify(stripeWrapperMock, never()).createCustomer(m.any())
    }

    // This test is pending because currently there is no way to auth as another customer midtest
    "creates cards for different customers correctly" in {
      pending

      val customer1 = new Customer_Seed {}.customer
      val account2  = Accounts.create(Account()).gimme
      val customer2 = Users
        .create(Factories.customer.copy(accountId = account2.id, email = "another@gmail.com".some))
        .gimme

      POST("v1/my/payment-methods/credit-cards", thePayload).mustBeOk()

      // TODO: auth as another customer here
      POST("v1/my/payment-methods/credit-cards", thePayload).mustBeOk()

      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer1))
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer2))

      val ccCustomerIds = CreditCards.map(_.accountId).result.gimme
      ccCustomerIds must contain allOf (customer1.accountId, customer2.accountId)
    }

    "does not create a new address if it isn't new" in new StoreAdmin_Seed with Customer_Seed {
      POST("v1/my/payment-methods/credit-cards", thePayload).mustBeOk()
      Addresses.result.headOption.gimme must not be defined
    }

    "creates address if it's new" in new StoreAdmin_Seed with Customer_Seed {
      POST("v1/my/payment-methods/credit-cards", thePayload.copy(addressIsNew = true)).mustBeOk()
      Addresses.result.headOption.gimme.value must === (theAddress)
    }

    "errors 400 if wrong credit card token" in new StoreAdmin_Seed with Customer_Seed {
      when(stripeWrapperMock.createCustomer(m.any()))
        .thenReturn(Result.failure[StripeCustomer](GeneralFailure("BAD-TOKEN")))

      POST("v1/my/payment-methods/credit-cards", thePayload).mustFailWithMessage("BAD-TOKEN")
    }

    "errors 400 if wrong region id" in new Customer_Seed {
      val payload = thePayload.copy(billingAddress = theAddressPayload.copy(regionId = -1))
      POST("v1/my/payment-methods/credit-cards", payload).mustFailWith400(
          NotFoundFailure400(Region, -1))
    }

    "validates payload" in {
      val validationErrors = crookedPayload.validate.toXor.leftVal.toList.map(_.description)

      POST("v1/my/payment-methods/credit-cards", crookedPayload).mustFailWithMessage(
          validationErrors: _*)
    }
  }

  private def customerSourceMap(customer: User, token: String = tokenStripeId) =
    Map("description" → "FoxCommerce", "email" → customer.email.value, "source" → token)

}
