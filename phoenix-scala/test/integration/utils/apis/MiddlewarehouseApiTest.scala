package utils.apis

import cats.data.NonEmptyList
import failures.Failures
import failures.MiddlewarehouseFailures.MiddlewarehouseError
import testutils.TestBase

class MiddlewarehouseApiTest extends TestBase {

  def MwhApiTest(message: String, errors: Failures): Unit = {
    val mwhApi = new Middlewarehouse("testURL")
    val failures = mwhApi.parseMwhErrors(message)
    failures must === (errors)
  }

  "MiddlewarehouseApi" - {
    "returns proper error message with sinle SKU that is out of stock" in {
      MwhApiTest(
        "{\"errors\":[{\"sku\":\"SKU\",\"debug\":\"Entry in table stock_items not found for sku=SKU.\"}]}",
        NonEmptyList.of((MiddlewarehouseError("Following SKUs are out of stock: SKU. Please remove them from your cart to complete checkout.")))
      )
    }
    "returns proper error message with list of SKUs that are out of stock" in {
      MwhApiTest(
        "{\"errors\":[{\"sku\":\"SKU1\",\"debug\":\"boom.\"},{\"sku\":\"SKU2\",\"debug\":\"boom.\"}]}",
        NonEmptyList.of(MiddlewarehouseError("Following SKUs are out of stock: SKU1, SKU2. Please remove them from your cart to complete checkout."))
      )
    }
    "returns proper errors with list of errors" in {
      MwhApiTest(
        "{\"errors\":[\"test1\", \"test2\"]}",
        NonEmptyList.of(MiddlewarehouseError("test1"),MiddlewarehouseError("test2"))
      )
    }
    "returns default error message with empty error list" in {
      val err = "{\"errors\":[]}"
      MwhApiTest(
        err,
        NonEmptyList.of(MiddlewarehouseError(err))
      )
    }
  }
}
