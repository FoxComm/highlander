package utils

import java.time.{Instant, ZonedDateTime}

import models.admin.AdminData
import models.cord.Order
import models.payment.creditcard.CreditCardCharge
import models.payment.giftcard.GiftCard
import org.json4s.Formats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import testutils.TestBase
import utils.JsonFormatters._
import utils.Money.Currency

class JsonFormattersTest extends TestBase {

  implicit val formats: Formats = phoenixFormats

  case class Test(order: Order.State,
                  gc: GiftCard.State,
                  cc: CreditCardCharge.State,
                  sas: AdminData.State)
  case class Product(price: Int, currency: Currency)

  "Adt serialization" - {
    "can (de-)serialize JSON" in {
      val ast = parse(
        write(
          Test(order = Order.ManualHold,
               cc = CreditCardCharge.Auth,
               gc = GiftCard.OnHold,
               sas = AdminData.Invited)))
      (ast \ "order").extract[Order.State] mustBe Order.ManualHold
      (ast \ "gc").extract[GiftCard.State] mustBe GiftCard.OnHold
      (ast \ "cc").extract[CreditCardCharge.State] mustBe CreditCardCharge.Auth
      (ast \ "sas").extract[AdminData.State] mustBe AdminData.Invited
    }
  }

  "Can JSON (de-)serialize Currency" in {
    val ast = parse(write(Product(price = 50, currency = Currency.USD)))
    (ast \ "price").extract[Int] must === (50)
    (ast \ "currency").extract[Currency] must === (Currency.USD)
  }

  "(de)serializes java.time.Instant" in {
    val instant = ZonedDateTime.of(2015, 9, 14, 15, 38, 46, 0, time.UTC).toInstant

    write("hello" → instant) must === ("""{"hello":"2015-09-14T15:38:46Z"}""")
    (parse("""{"hello":"2015-09-14T15:38:46Z"}""") \ "hello").extract[Instant] must === (instant)
  }
}
