package utils

import util.TestBase
import utils.Strings._

import payloads.{GiftCardCreateByCsr, CreateNote}
import models.activity.Trail

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
      snakeCaseName(Trail(dimensionId = 1, objectId = "1"))           must === ("trail")
      snakeCaseName(CreateNote(body = "test"))                        must === ("create_note")
      snakeCaseName(GiftCardCreateByCsr(balance = 10, reasonId = 1))  must === ("gift_card_create_by_csr")
    }
  }

  "singularize" - {
    "should singularize table names properly" in {
      "orders".tableNameToCamel must === ("order")
      "order_line_items".tableNameToCamel must === ("orderLineItem")
      "activities".tableNameToCamel must === ("activity")
    }
  }
}
