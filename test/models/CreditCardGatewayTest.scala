package models

import util.TestBase

class CreditCardGatewayTest extends TestBase {
  val Template = CreditCardGateway(
    customerId = 1,
    gatewayCustomerId = "abcdef",
    lastFour = "4242",
    expMonth = 11,
    expYear  = 2018)

  "CreditCardGateway" - {
    "validations" - {
      "disallows cards with expiration in the past" in {
        val result = Template.copy(expYear = 2015, expMonth = 4).validate

        result.messages.size must === (1)
        result.messages.head must include ("expiration")
      }
    }
  }
}
