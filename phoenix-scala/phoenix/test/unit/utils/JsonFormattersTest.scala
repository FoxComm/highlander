package utils

import java.time.{Instant, ZonedDateTime}

import core.utils.Money.Currency
import core.utils.time.UTC
import org.json4s.Formats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import phoenix.models.admin.AdminData
import phoenix.models.cord.Order
import phoenix.models.payment.creditcard.CreditCardCharge
import phoenix.models.payment.giftcard.GiftCard
import phoenix.utils.JsonFormatters._
import testutils.TestBase

class JsonFormattersTest extends TestBase {

  implicit val formats: Formats = phoenixFormats

  case class Test(order: Order.State,
                  gc: GiftCard.State,
                  cc: CreditCardCharge.State,
                  sas: AdminData.State)
  case class Product(price: Long, currency: Currency)

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
    (ast \ "price").extract[Long] must === (50L)
    (ast \ "currency").extract[Currency] must === (Currency.USD)
  }

  "(de)serializes java.time.Instant" in {
    val instant = ZonedDateTime.of(2015, 9, 14, 15, 38, 46, 0, UTC).toInstant

    write("hello" → instant) must === ("""{"hello":"2015-09-14T15:38:46Z"}""")
    (parse("""{"hello":"2015-09-14T15:38:46Z"}""") \ "hello").extract[Instant] must === (instant)
  }
}
