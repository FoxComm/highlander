package utils

import scalaz.Show

import com.pellucid.sealerate
import util.TestBase

import models.{GiftCard, Auth, CreditCardPaymentStatus, Order}

class JsonFormattersTest extends TestBase {
  import org.json4s.jackson.JsonMethods.parse
  import org.json4s.jackson.Serialization.write

  import utils.JsonFormatters._

  implicit val formats = phoenixFormats

  case class Test(order: Order.Status, gc: GiftCard.Status, cc: CreditCardPaymentStatus)

  "Adt serialization" - {
    "can (de-)serialize JSON" in {
      val ast = parse(write(Test(order = Order.Cart, cc = Auth, gc = GiftCard.Hold)))
      (ast \ "order").extract[Order.Status] mustBe Order.Cart
      (ast \ "gc").extract[GiftCard.Status] mustBe GiftCard.Hold
      (ast \ "cc").extract[CreditCardPaymentStatus] mustBe Auth
    }
  }
}