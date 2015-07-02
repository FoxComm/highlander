package utils

import scalaz.Show

import com.pellucid.sealerate
import util.TestBase

import models.{Auth, InsufficientBalance, GiftCardPaymentStatus, CreditCardPaymentStatus, Order}

class JsonFormattersTest extends TestBase {
  import org.json4s.jackson.JsonMethods.parse
  import org.json4s.jackson.Serialization.write

  import utils.JsonFormatters._

  implicit val formats = phoenixFormats

  case class Test(order: Order.Status, gc: GiftCardPaymentStatus, cc: CreditCardPaymentStatus)

  "Adt serialization" - {
    "can (de-)serialize JSON" in {
      val ast = parse(write(Test(order = Order.Cart, cc = Auth, gc = InsufficientBalance)))
      (ast \ "order").extract[Order.Status] must === (Order.Cart)
      (ast \ "gc").extract[GiftCardPaymentStatus] must === (InsufficientBalance)
      (ast \ "cc").extract[CreditCardPaymentStatus] must === (Auth)
    }
  }
}
