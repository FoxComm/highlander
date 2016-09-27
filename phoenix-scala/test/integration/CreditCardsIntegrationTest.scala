import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures._
import models.customer.{Customer, Customers}
import models.location.{Addresses, Region}
import models.payment.creditcard.{CreditCard, CreditCards}
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers ⇒ m, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import payloads.AddressPayloads.CreateAddressPayload
import payloads.PaymentPayloads._
import responses.CreditCardsResponse
import services.Result
import shapeless._
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures
import utils._
import utils.aliases.stripe.StripeCustomer
import utils.seeds.Seeds.Factories

class CreditCardsIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockitoSugar
    with BakedFixtures
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    initStripeApiMock(stripeWrapperMock)
  }

  val theAddress = Factories.address.copy(id = 1, customerId = 1, isDefaultShipping = false)
  val expYear    = ZonedDateTime.now.getYear + 3

  val theAddressPayload = CreateCcAddressPayload(theAddress.toPayload)

  val tokenStripeId = s"tok_${TestStripeSupport.randomStripeishId}"

  val thePayload = CreateCreditCardFromTokenPayload(token = tokenStripeId,
                                                    lastFour = "1234",
                                                    expMonth = 1,
                                                    expYear = expYear,
                                                    brand = "Mona Visa",
                                                    holderName = "Leo",
                                                    billingAddress = theAddressPayload)

  val crookedAddressPayload = CreateCcAddressPayload(
      CreateAddressPayload(name = "",
                           address1 = "",
                           address2 = "".some,
                           zip = "",
                           regionId = -1,
                           city = "",
                           phoneNumber = "".some))
  val crookedPayload = CreateCreditCardFromTokenPayload(token = "",
                                                        lastFour = "",
                                                        expMonth = 666,
                                                        expYear = 777,
                                                        brand = "",
                                                        holderName = "",
                                                        billingAddress = crookedAddressPayload)

  val billingAddressLens = lens[CreateCreditCardFromTokenPayload].billingAddress
  val regionIdLens       = billingAddressLens.address.regionId
  val isNewLens          = billingAddressLens.isNew

  "POST /v1/customers/:id/payment-methods/credit-cards (admin auth)" - {
    "creates a new credit card" in new Customer_Seed {
      // No Stripe customer yet
      val response1 = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", thePayload)
      response1.status must === (StatusCodes.OK)
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer))

      val creditCards = CreditCards.result.gimme
      creditCards must have size 1

      val expected = CreditCard(id = 1,
                                gatewayCustomerId = stripeCustomer.getId,
                                gatewayCardId = stripeCard.getId,
                                customerId = customer.id,
                                addressName = theAddress.name,
                                address1 = theAddress.address1,
                                address2 = theAddress.address2,
                                city = theAddress.city,
                                zip = theAddress.zip,
                                regionId = theAddress.regionId,
                                brand = "Mona Visa",
                                holderName = "Leo",
                                lastFour = "1234",
                                expMonth = 1,
                                expYear = expYear)

      creditCards.head must === (expected)

      // With existing Stripe customer
      Mockito.clearInvocations(stripeWrapperMock)

      val response2 = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", thePayload)
      response2.status must === (StatusCodes.OK)

      Mockito.verify(stripeWrapperMock).findCustomer(stripeCustomer.getId)
      Mockito.verify(stripeWrapperMock).createCard(m.eq(stripeCustomer), m.any())
      Mockito.verify(stripeWrapperMock, never()).createCustomer(m.any())
    }

    "creates cards for different customers correctly" in {
      val customer1 = new Customer_Seed {}.customer
      val customer2 =
        Customers.create(Factories.customer.copy(email = "another@gmail.com".some)).gimme

      val response1 = POST(s"v1/customers/${customer1.id}/payment-methods/credit-cards",
                           thePayload.copy(token = "tok_1"))
      response1.status must === (StatusCodes.OK)

      val stripeCustomer2 = newStripeCustomer
      when(stripeWrapperMock.createCustomer(m.any())).thenReturn(Result.good(stripeCustomer2))
      val response2 = POST(s"v1/customers/${customer2.id}/payment-methods/credit-cards",
                           thePayload.copy(token = "tok_2"))
      response2.status must === (StatusCodes.OK)

      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer1, "tok_1"))
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer2, "tok_2"))

      val ccCustomerIds = CreditCards.map(_.customerId).result.gimme
      ccCustomerIds must contain allOf (customer1.id, customer2.id)
    }

    "does not create a new address if it isn't new" in new Customer_Seed {
      val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", thePayload)
      response.status must === (StatusCodes.OK)
      Addresses.result.headOption.gimme must not be defined
    }

    "creates address if it's new" in new Customer_Seed {
      val payload  = isNewLens.set(thePayload)(true)
      val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
      response.status must === (StatusCodes.OK)
      Addresses.result.headOption.gimme.value must === (theAddress)
    }

    "errors 404 if wrong customer id" in {
      val response = POST("v1/customers/666/payment-methods/credit-cards", thePayload)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 666).description)
    }

    "errors 400 if wrong credit card token" in new Customer_Seed {
      when(stripeWrapperMock.createCustomer(m.any()))
        .thenReturn(Result.failure[StripeCustomer](GeneralFailure("BAD-TOKEN")))
      val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", thePayload)
      response.status must === (StatusCodes.BadRequest)
      response.error must === ("BAD-TOKEN")
    }

    "errors 400 if wrong region id" in new Customer_Seed {
      val wrongRegionIdPayload = regionIdLens.set(thePayload)(-1)

      val response =
        POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", wrongRegionIdPayload)
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure400(Region, -1).description)
    }

    "validates payload" in {
      val response = POST("v1/customers/666/payment-methods/credit-cards", crookedPayload)
      response.status must === (StatusCodes.BadRequest)

      val validationErrors: List[String] =
        crookedPayload.validate.toXor.leftVal.toList.map(_.description)
      response.errors must contain theSameElementsAs validationErrors
    }
  }

  "DELETE /v1/customers/:custId/payment-methods/credit-cards/:cardId (admin auth)" - {
    "deletes specified card" in new Customer_Seed {
      val createResp1 =
        POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", thePayload)
      createResp1.status must === (StatusCodes.OK)
      val ccResp1 = createResp1.as[CreditCardsResponse.Root]

      val stripeCard2 = newStripeCard
      when(stripeWrapperMock.createCard(m.any(), m.any())).thenReturn(Result.good(stripeCard2))
      when(stripeWrapperMock.findCardByCustomerId(stripeCustomer.getId, stripeCard2.getId))
        .thenReturn(Result.good(stripeCard2))
      val createResp2 =
        POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", thePayload)
      createResp2.status must === (StatusCodes.OK)
      val ccResp2 = createResp2.as[CreditCardsResponse.Root]

      val getResp1 = GET(s"v1/customers/${customer.id}/payment-methods/credit-cards")
      getResp1.status must === (StatusCodes.OK)
      val allCcResps = Seq(ccResp1, ccResp2)
      getResp1.as[Seq[CreditCardsResponse.Root]] must contain theSameElementsAs allCcResps

      val deleteResp =
        DELETE(s"v1/customers/${customer.id}/payment-methods/credit-cards/${ccResp2.id}")
      deleteResp.status must === (StatusCodes.NoContent)
      verify(stripeWrapperMock).deleteCard(m.argThat(cardStripeIdMatches(stripeCard2.getId)))

      val getResp2 = GET(s"v1/customers/${customer.id}/payment-methods/credit-cards")
      getResp2.status must === (StatusCodes.OK)
      getResp2.as[Seq[CreditCardsResponse.Root]] must === (Seq(ccResp1))
    }

    "errors 404 if customer not found" in {
      val response = DELETE(s"v1/customers/666/payment-methods/credit-cards/777")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 666).description)
    }

    "errors 404 if card not found" in new Customer_Seed {
      val response = DELETE(s"v1/customers/${customer.id}/payment-methods/credit-cards/666")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(CreditCard, 666).description)
    }
  }

  "POST /v1/my/payment-methods/credit-cards (customer auth)" - {
    "creates a new credit card" in new Customer_Seed {
      // No Stripe customer yet
      val response1 = POST("v1/my/payment-methods/credit-cards", thePayload)
      response1.status must === (StatusCodes.OK)
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer))

      CreditCards.result.gimme must have size 1

      val expected = CreditCard(id = 1,
                                gatewayCustomerId = stripeCustomer.getId,
                                customerId = customer.id,
                                addressName = theAddress.name,
                                address1 = theAddress.address1,
                                address2 = theAddress.address2,
                                city = theAddress.city,
                                zip = theAddress.zip,
                                regionId = theAddress.regionId,
                                holderName = "Leo",
                                brand = "Mona Visa",
                                lastFour = "1234",
                                expMonth = 1,
                                expYear = expYear,
                                gatewayCardId = stripeCard.getId)

      CreditCards.result.gimme.head must === (expected)

      // With existing Stripe customer
      Mockito.clearInvocations(stripeWrapperMock)

      val response2 = POST("v1/my/payment-methods/credit-cards", thePayload)
      response2.status must === (StatusCodes.OK)

      Mockito.verify(stripeWrapperMock).findCustomer(stripeCustomer.getId)
      Mockito.verify(stripeWrapperMock).createCard(m.eq(stripeCustomer), m.any())
      Mockito.verify(stripeWrapperMock, never()).createCustomer(m.any())
    }

    // This test is pending because currently there is no way to auth as another customer midtest
    "creates cards for different customers correctly" in {
      pending

      val customer1 = new Customer_Seed {}.customer
      val customer2 =
        Customers.create(Factories.customer.copy(email = "another@gmail.com".some)).gimme

      val response1 = POST("v1/my/payment-methods/credit-cards", thePayload)
      response1.status must === (StatusCodes.OK)

      // TODO: auth as another customer here
      val response2 = POST("v1/my/payment-methods/credit-cards", thePayload)
      response2.status must === (StatusCodes.OK)

      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer1))
      Mockito.verify(stripeWrapperMock).createCustomer(customerSourceMap(customer2))

      val ccCustomerIds = CreditCards.map(_.customerId).result.gimme
      ccCustomerIds must contain allOf (customer1.id, customer2.id)
    }

    "does not create a new address if it isn't new" in new Customer_Seed {
      val response = POST("v1/my/payment-methods/credit-cards", thePayload)
      response.status must === (StatusCodes.OK)
      Addresses.result.headOption.gimme must not be defined
    }

    "creates address if it's new" in new Customer_Seed {
      val payload  = isNewLens.set(thePayload)(true)
      val response = POST("v1/my/payment-methods/credit-cards", payload)
      response.status must === (StatusCodes.OK)
      Addresses.result.headOption.gimme.value must === (theAddress)
    }

    "errors 400 if wrong credit card token" in new Customer_Seed {
      when(stripeWrapperMock.createCustomer(m.any()))
        .thenReturn(Result.failure[StripeCustomer](GeneralFailure("BAD-TOKEN")))

      val response = POST("v1/my/payment-methods/credit-cards", thePayload)
      response.status must === (StatusCodes.BadRequest)
      response.error must === ("BAD-TOKEN")
    }

    "errors 400 if wrong region id" in new Customer_Seed {
      val wrongRegionIdPayload = regionIdLens.set(thePayload)(-1)

      val response = POST("v1/my/payment-methods/credit-cards", wrongRegionIdPayload)
      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure400(Region, -1).description)
    }

    "validates payload" in {
      val response = POST("v1/my/payment-methods/credit-cards", crookedPayload)
      response.status must === (StatusCodes.BadRequest)

      val validationErrors = crookedPayload.validate.toXor.leftVal.toList.map(_.description)
      response.errors must contain theSameElementsAs validationErrors
    }
  }

  private def customerSourceMap(customer: Customer, token: String = tokenStripeId) =
    Map("description" → "FoxCommerce", "email" → customer.email.value, "source" → token)

}
