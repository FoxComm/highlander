package testutils.fixtures

import java.time.ZonedDateTime

import cats.implicits._
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.PaymentPayloads.CreateCreditCardFromTokenPayload
import phoenix.utils.TestStripeSupport
import phoenix.utils.seeds.Factories

object PaymentFixtures {

  trait CreditCardsFixture {

    val theAddress = Factories.address.copy(id = 1, accountId = 2, isDefaultShipping = false)
    val expYear    = ZonedDateTime.now.getYear + 3

    val theAddressPayload = CreateAddressPayload(
      name = theAddress.name,
      address1 = theAddress.address1,
      address2 = theAddress.address2,
      zip = theAddress.zip,
      city = theAddress.city,
      regionId = theAddress.regionId,
      phoneNumber = theAddress.phoneNumber
    )

    val tokenStripeId = s"tok_${TestStripeSupport.randomStripeishId}"

    val ccPayload = CreateCreditCardFromTokenPayload(token = tokenStripeId,
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

  }

}
