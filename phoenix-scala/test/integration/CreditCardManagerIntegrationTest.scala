import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import util.{Fixtures, IntegrationTestBase, StripeSupport}
import failures.CreditCardFailures.InvalidCvc
import failures.NotFoundFailure404
import models.customer.{Customer, Customers}
import models.location.{Address, Addresses}
import models.payment.creditcard.CreditCards
import payloads.AddressPayloads.CreateAddressPayload
import payloads.PaymentPayloads.CreateCreditCard
import util.{IntegrationTestBase, StripeSupport}
import utils.seeds.Seeds.Factories

class CreditCardManagerIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with Fixtures {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import slick.driver.PostgresDriver.api._

  "CreditCardManagerTest" - {
    "POST /v1/customers/:id/payment-methods/credit-cards" - {
      val tomorrow = ZonedDateTime.now().plusDays(1)
      val payloadStub = CreateCreditCard(holderName = "yax",
                                         cardNumber = StripeSupport.successfulCard,
                                         cvv = "123",
                                         expYear = tomorrow.getYear,
                                         expMonth = tomorrow.getMonthValue)

      def payloadWithFullAddress(p: CreateCreditCard, a: Address): CreateCreditCard = {
        p.copy(addressId = None,
               address = Some(
                   CreateAddressPayload(name = a.name,
                                        address1 = a.address1,
                                        address2 = a.address2,
                                        city = a.city,
                                        zip = a.zip,
                                        regionId = a.regionId)))
      }

      "successfully" - {
        "copies an existing address to the new creditCard" ignore new AddressFixture {
          val payload     = payloadStub.copy(addressId = Some(address.id), isDefault = true)
          val response    = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val (cc :: Nil) = CreditCards.filter(_.customerId === customer.id).gimme.toList

          val a = address
          val ccAddressVals = (cc.address1,
                               cc.address2,
                               cc.city,
                               cc.regionId,
                               cc.zip,
                               cc.addressName,
                               cc.phoneNumber)
          val addressVals =
            (a.address1, a.address2, a.city, a.regionId, a.zip, a.name, a.phoneNumber)

          response.status must === (StatusCodes.OK)
          ccAddressVals must === (addressVals)
          cc.isDefault mustBe true
          cc.inWallet mustBe true
          cc.deletedAt mustBe 'empty
          cc.lastFour must === (payload.lastFour)
          (cc.expYear, cc.expMonth) must === ((payload.expYear, payload.expMonth))
          cc.zipCheck mustBe 'defined
          cc.address1Check mustBe 'defined
        }

        "creates a new address in the book and copies it to the new creditCard" ignore new CustomerFixture {
          val a       = Factories.address
          val payload = payloadWithFullAddress(payloadStub.copy(isDefault = true), a)

          val response              = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val (cc :: Nil)           = CreditCards.filter(_.customerId === customer.id).gimme.toList
          val (savedAddress :: Nil) = Addresses.gimme.toList
          val s                     = savedAddress

          val ccAddressVals = (cc.address1,
                               cc.address2,
                               cc.city,
                               cc.regionId,
                               cc.zip,
                               cc.addressName,
                               cc.phoneNumber)
          val addressVals =
            (a.address1, a.address2, a.city, a.regionId, a.zip, a.name, a.phoneNumber)
          val savedVals =
            (s.address1, s.address2, s.city, s.regionId, s.zip, s.name, s.phoneNumber)

          response.status must === (StatusCodes.OK)
          ccAddressVals must === (addressVals)
          ccAddressVals must === (savedVals)
          cc.isDefault mustBe true
          cc.inWallet mustBe true
          cc.deletedAt mustBe 'empty
          cc.lastFour must === (payload.lastFour)
          (cc.expYear, cc.expMonth) must === ((payload.expYear, payload.expMonth))
          cc.zipCheck mustBe 'defined
          cc.address1Check mustBe 'defined
        }

        "uses an existing stripe customerId when it exists" ignore new AddressFixture {
          val payload = payloadStub.copy(addressId = address.id.some)
          val seed    = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)

          seed.status must === (StatusCodes.OK)

          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards    = CreditCards.filter(_.customerId === customer.id).gimme

          response.status must === (StatusCodes.OK)
          cards must have size (2)
          cards.map(_.gatewayCustomerId).toSet must have size (1)
          cards.map(_.gatewayCardId).toSet must have size (2)
        }
      }

      "fails" - {
        "if neither addressId nor full address was provided" ignore new CustomerFixture {
          val payload  = payloadStub.copy(address = None, addressId = None)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards    = CreditCards.gimme

          response.status must === (StatusCodes.BadRequest)
          response.error must contain("address or addressId must be defined")
          cards mustBe 'empty
        }

        "if the addressId cannot be found in address book" ignore new CustomerFixture {
          val payload  = payloadStub.copy(addressId = Some(1))
          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards    = CreditCards.gimme

          response.status must === (StatusCodes.BadRequest)
          response.error must contain(NotFoundFailure404(Address, 1).description)
          cards mustBe 'empty
        }

        "if card info is invalid" ignore new CustomerFixture {
          val payload = payloadWithFullAddress(
              payloadStub.copy(cardNumber = StripeSupport.incorrectNumberCard),
              Factories.address)
          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards    = CreditCards.gimme

          response.status must === (StatusCodes.BadRequest)
          response.error must contain("incorrect_number")
          cards mustBe 'empty
        }

        "if Stripe's CVC check fails" ignore new AddressFixture {
          val payload =
            payloadStub.copy(cardNumber = StripeSupport.incorrectCvc, addressId = Some(1))
          val response = POST(s"v1/customers/${customer.id}/payment-methods/credit-cards", payload)
          val cards    = CreditCards.gimme

          response.status must === (StatusCodes.BadRequest)
          response.error must === (InvalidCvc.description)
          cards mustBe 'empty
        }

        "if customer cannot be found" ignore {
          val payload  = payloadWithFullAddress(payloadStub, Factories.address)
          val response = POST(s"v1/customers/99/payment-methods/credit-cards", payload)
          val cards    = CreditCards.gimme

          response.status must === (StatusCodes.NotFound)
          response.error must === (NotFoundFailure404(Customer, 99).description)
          cards mustBe 'empty
        }
      }
    }
  }
}
