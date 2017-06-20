package utils.apis

import cats.data.NonEmptyList
import core.failures.Failures
import phoenix.failures.MiddlewarehouseFailures._
import phoenix.utils.apis.Middlewarehouse
import testutils.TestBase

class MiddlewarehouseApiTest extends TestBase {

  private def mwhApiTest(message: String, errors: Failures) = {
    val mwhApi = new Middlewarehouse("testURL")
    val result = mwhApi.parseMwhErrors(message)
    result must === (errors)
  }

  "MiddlewarehouseApi" - {
    "returns proper error message with sinle SKU that is out of stock" in {
      mwhApiTest(
        """{
            |   "errors":[
            |      {
            |         "sku":"SKU",
            |         "debug":"Entry in table stock_items not found for sku=SKU."
            |      }
            |   ]
            |}""".stripMargin,
        NonEmptyList.of(SkusOutOfStockFailure(List[String]("SKU")))
      )
    }
    "returns proper error message with list of SKUs that are out of stock" in {
      mwhApiTest(
        """{
            |   "errors":[
            |      {
            |         "sku":"SKU1",
            |         "debug":"boom."
            |      },
            |      {
            |         "sku":"SKU2",
            |         "debug":"boom."
            |      }
            |   ]
            |}""".stripMargin,
        NonEmptyList.of(SkusOutOfStockFailure(List[String]("SKU1", "SKU2")))
      )
    }
    "returns proper errors with list of errors" in {
      mwhApiTest(
        """{
            |   "errors":[
            |      "test1",
            |      "test2"
            |   ]
            |}""".stripMargin,
        NonEmptyList.of(MiddlewarehouseError("test1"), MiddlewarehouseError("test2"))
      )
    }
    "returns default error message with empty error list" in {
      val err = """{
                  |   "errors":[]
                  |}""".stripMargin
      mwhApiTest(err, NonEmptyList.of(MiddlewarehouseError(err)))
    }
  }
}
