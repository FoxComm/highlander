import akka.http.scaladsl.model.StatusCodes

import models.{Orders, Customer, CreditCards, CreditCard, Customers, Addresses, StoreAdmins, OrderPayments}
import models.OrderPayments.scope._
import org.joda.time.DateTime
import payloads.CreateAddressPayload
import services.{CannotUseInactiveCreditCard, CustomerManager, NotFoundFailure}
import util.{StripeSupport, IntegrationTestBase}
import utils.Seeds.Factories
import utils.RunOnDbIO

class CreditCardManagerIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._
  import slick.driver.PostgresDriver.api._

  "CreditCardManagerTest" - {
    "when creating a credit card" - {
      "successfully" - {
        val tomorrow = DateTime.now().plusDays(1)
        val payloadStub = payloads.CreateCreditCard(holderName = "yax", number = StripeSupport.successfulCard,
          cvv = "123", expYear = tomorrow.getYear, expMonth = tomorrow.getMonthOfYear)

        "copies an existing address to the new creditCard" ignore new AddressFixture {
          val payload = payloadStub.copy(addressId = Some(address.id), isDefault = true)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val (cc :: Nil) = CreditCards.filter(_.customerId === customer.id).result.run().futureValue.toList

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
          val payload = payloadStub.copy(address = Some(CreateAddressPayload(
            name = a.name, street1 = a.street1, street2 = a.street2,
            city = a.city, zip = a.zip, regionId = a.regionId)))

          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val (cc :: Nil) = CreditCards.filter(_.customerId === customer.id).result.run().futureValue.toList
          val (savedAddress :: Nil) = Addresses.result.run().futureValue.toList
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
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).run().futureValue
  }
}

