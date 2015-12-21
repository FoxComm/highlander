package utils

import util.TestBase

import payloads.{GiftCardCreateByCsr, CreateNote, Watchers}

class UtilsTest extends TestBase {

  "camelToUnderscores" - {
    "should convert camelCase string to snake_case string" in {
      camelToUnderscores("order")                 must === ("order")
      camelToUnderscores("orderShipping")         must === ("order_shipping")
      camelToUnderscores("orderShippingAddress")  must === ("order_shipping_address")
    }
  }

  "camelToUnderscores" - {
    "should convert camelCase class to snake_case string" in {
      snakeCaseName(Watchers(watchers = Seq(1, 2, 3)))                must === ("watchers")
      snakeCaseName(CreateNote(body = "test"))                        must === ("create_note")
      snakeCaseName(GiftCardCreateByCsr(balance = 10, reasonId = 1))  must === ("gift_card_create_by_csr")
    }
  }
}
