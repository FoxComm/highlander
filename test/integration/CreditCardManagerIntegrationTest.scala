import akka.http.scaladsl.model.StatusCodes

import models.{Address, Customer, CreditCards, CreditCard, Customers, Addresses}
import org.joda.time.DateTime
import payloads.CreateAddressPayload
import services.{CVCFailure, NotFoundFailure}
import util.{StripeSupport, IntegrationTestBase}
import utils.Seeds.Factories
import utils.Slick.implicits._

class CreditCardManagerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  import slick.driver.PostgresDriver.api._
  import util.SlickSupport.implicits._

  "CreditCardManagerTest" - {
    "POST /v1/customers/:id/payment-methods/credit-cards" - {
      val tomorrow = DateTime.now().plusDays(1)
      val payloadStub = payloads.CreateCreditCard(holderName = "yax", number = StripeSupport.successfulCard,
        cvv = "123", expYear = tomorrow.getYear, expMonth = tomorrow.getMonthOfYear)

      def payloadWithFullAddress(p: payloads.CreateCreditCard, a: Address): payloads.CreateCreditCard = {
        p.copy(address = Some(CreateAddressPayload(
          name = a.name, street1 = a.street1, street2 = a.street2,
          city = a.city, zip = a.zip, regionId = a.regionId)))
      }

      "successfully" - {
        "copies an existing address to the new creditCard" ignore new AddressFixture {
          val payload = payloadStub.copy(addressId = Some(address.id), isDefault = true)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val (cc :: Nil) = CreditCards.filter(_.customerId === customer.id).futureValue.toList

          val a             = address
          val ccAddressVals = (cc.street1, cc.street2, cc.city, cc.regionId, cc.zip, cc.addressName, cc.phoneNumber)
          val addressVals   = (a.street1, a.street2, a.city, a.regionId, a.zip, a.name, a.phoneNumber)

          response.status must ===(StatusCodes.OK)
          ccAddressVals must === (addressVals)
          cc.isDefault mustBe true
          cc.inWallet mustBe true
          cc.deletedAt mustBe 'empty
          cc.lastFour must === (payload.lastFour)
          (cc.expYear, cc.expMonth) must === ((payload.expYear, payload.expMonth))
        }

        "creates a new address in the book and copies it to the new creditCard" ignore new Fixture {
          val a = Factories.address
          val payload = payloadWithFullAddress(payloadStub, a)

          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val (cc :: Nil) = CreditCards.filter(_.customerId === customer.id).futureValue.toList
          val (savedAddress :: Nil) = Addresses.futureValue.toList
          val s = savedAddress

          val ccAddressVals = (cc.street1, cc.street2, cc.city, cc.regionId, cc.zip, cc.addressName, cc.phoneNumber)
          val addressVals   = (a.street1, a.street2, a.city, a.regionId, a.zip, a.name, a.phoneNumber)
          val savedVals     = (s.street1, s.street2, s.city, s.regionId, s.zip, s.name, s.phoneNumber)

          response.status must ===(StatusCodes.OK)
          ccAddressVals must === (addressVals)
          ccAddressVals must === (savedVals)
          cc.isDefault mustBe true
          cc.inWallet mustBe true
          cc.deletedAt mustBe 'empty
          cc.lastFour must === (payload.lastFour)
          (cc.expYear, cc.expMonth) must === ((payload.expYear, payload.expMonth))
        }
      }

      "fails" - {
        "if neither addressId nor full address was provided" ignore new Fixture {
          val payload   = payloadStub.copy(address = None, addressId = None)
          val response  = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards     = CreditCards.futureValue

          response.status must ===(StatusCodes.BadRequest)
          response.errors must contain ("address or addressId must be defined")
          cards mustBe 'empty
        }

        "if the addressId cannot be found in address book" ignore new Fixture {
          val payload   = payloadStub.copy(addressId = Some(1))
          val response  = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards     = CreditCards.futureValue

          response.status must ===(StatusCodes.BadRequest)
          response.errors must contain (NotFoundFailure(Address, 1).description)
          cards mustBe 'empty
        }

        "if card info is invalid" ignore new Fixture {
          val payload   = payloadWithFullAddress(payloadStub.copy(number = StripeSupport.incorrectNumberCard),
            Factories.address)
          val response  = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards     = CreditCards.futureValue

          response.status must ===(StatusCodes.BadRequest)
          response.errors must contain ("incorrect_number")
          cards mustBe 'empty
        }

        "if Stripe's CVC check fails" ignore new AddressFixture {
          val payload   = payloadStub.copy(number = StripeSupport.incorrectCVC, addressId = Some(1))
          val response  = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards     = CreditCards.futureValue

          response.status must ===(StatusCodes.BadRequest)
          response.errors must ===(CVCFailure.description)
          cards mustBe 'empty
        }

        "if customer cannot be found" ignore {
          val payload   = payloadWithFullAddress(payloadStub, Factories.address)
          val response  = POST(s"v1/customers/99/payment-methods/credit-cards", payload)
          val cards     = CreditCards.futureValue

          response.status must ===(StatusCodes.NotFound)
          response.errors must ===(NotFoundFailure(Customer, 99).description)
          cards mustBe 'empty
        }
      }
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).futureValue
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).futureValue
  }
}

