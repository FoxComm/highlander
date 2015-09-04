package utils

import scalaz.Show

import com.pellucid.sealerate
import org.json4s.Formats
import util.TestBase

import models.{GiftCard, CreditCardCharge, Order}
import utils.Money.Currency

class JsonFormattersTest extends TestBase {
  import org.json4s.jackson.JsonMethods.parse
  import org.json4s.jackson.Serialization.write

  import utils.JsonFormatters._

  implicit val formats: Formats = phoenixFormats

  case class Test(order: Order.Status, gc: GiftCard.Status, cc: CreditCardCharge.Status)
  case class Product(price: Int, currency: Currency)

  "Adt serialization" - {
    "can (de-)serialize JSON" in {
      val ast = parse(write(Test(order = Order.Cart, cc = CreditCardCharge.Auth, gc = GiftCard.OnHold)))
      (ast \ "order").extract[Order.Status] mustBe Order.Cart
      (ast \ "gc").extract[GiftCard.Status] mustBe GiftCard.OnHold
      (ast \ "cc").extract[CreditCardCharge.Status] mustBe CreditCardCharge.Auth
    }
  }

  "Can JSON (de-)serialize Currency" in {
    val ast = parse(write(Product(price = 50, currency = Currency.USD)))
    (ast \ "price").extract[Int] === (50)
    (ast \ "currency").extract[Currency] === (Currency.USD)
  }
}
