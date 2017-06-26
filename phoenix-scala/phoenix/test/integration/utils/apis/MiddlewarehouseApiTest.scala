package utils.apis

import cats.data.NonEmptyList
import core.failures.Failures
import phoenix.failures.MiddlewarehouseFailures._
import phoenix.utils.apis.{Middlewarehouse, MwhErrorInfo}
import testutils.TestBase

class MiddlewarehouseApiTest extends TestBase {

  private def mwhApiTest(message: String, errors: Failures) = {
    val mwhApi = new Middlewarehouse("testURL")
    val result = mwhApi.parseMwhErrors(message)
    result must === (errors)
  }

  "MiddlewarehouseApi" - {
    "returns proper error message with single SKU that is out of stock" in {
      mwhApiTest(
        """{
            |   "errors":[
            |      {
            |         "sku":"SKU",
            |         "afs":0,
            |         "debug":"Entry in table stock_items not found for sku=SKU."
            |      }
            |   ]
            |}""".stripMargin,
        NonEmptyList.of(
          SkusOutOfStockFailure(
            List(MwhErrorInfo("SKU", 0, "Entry in table stock_items not found for sku=SKU."))))
      )
    }
    "returns proper error message with list of SKUs that are out of stock" in {
      mwhApiTest(
        """{
            |   "errors":[
            |      {
            |         "sku":"SKU1",
            |         "afs":0,
            |         "debug":"boom."
            |      },
            |      {
            |         "sku":"SKU2",
            |         "afs":0,
            |         "debug":"boom."
            |      }
            |   ]
            |}""".stripMargin,
        NonEmptyList.of(
          SkusOutOfStockFailure(List(MwhErrorInfo("SKU1", 0, "boom."), MwhErrorInfo("SKU2", 0, "boom."))))
      )
    }
    "proper error description from SkusOutOfStockFailure. Only out of stock" in {
      val f = SkusOutOfStockFailure(List(MwhErrorInfo("SKU1", 0, ""), MwhErrorInfo("SKU2", 0, "")))
      f.description must === (
        "Following SKUs are out of stock: SKU1, SKU2. " +
          "Update your cart in order to complete checkout.")
    }
    "proper error description from SkusOutOfStockFailure. Only not enough items" in {
      val f = SkusOutOfStockFailure(List(MwhErrorInfo("SKU1", 10, ""), MwhErrorInfo("SKU2", 10, "")))
      f.description must === (
        "There is not enough items in inventory for SKUs: SKU1, SKU2. " +
          "Update your cart in order to complete checkout.")
    }
    "proper error description from SkusOutOfStockFailure. All items" in {
      val f = SkusOutOfStockFailure(List(MwhErrorInfo("SKU1", 0, ""), MwhErrorInfo("SKU2", 10, "")))
      f.description must === (
        "There is not enough items in inventory for SKUs: SKU2. " +
          "Following SKUs are out of stock: SKU1. " +
          "Update your cart in order to complete checkout.")
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
