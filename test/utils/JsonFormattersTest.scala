package utils

import util.TestBase

import models.{Auth, InsufficientBalance, GiftCardPaymentStatus, CreditCardPaymentStatus, Order}

class JsonFormattersTest extends TestBase {
  import org.json4s.jackson.JsonMethods.parse
  import org.json4s.jackson.Serialization.write

  import utils.JsonFormatters._

  case class Test(order: Order.Status, gc: GiftCardPaymentStatus, cc: CreditCardPaymentStatus)

  "Adt serialization" - {
    "can (de-)serialize" in {
      val ast = parse(write(Test(order = Order.Cart, cc = Auth, gc = InsufficientBalance)))
      (ast \ "order").extract[Order.Status] mustBe Order.Cart
      (ast \ "gc").extract[GiftCardPaymentStatus] mustBe InsufficientBalance
      (ast \ "cc").extract[CreditCardPaymentStatus] mustBe Auth
    }
  }
}