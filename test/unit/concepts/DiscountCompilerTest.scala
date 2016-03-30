package concepts

import concepts.discounts._
import failures.DiscountCompilerFailures._
import util.CustomMatchers._
import util.TestBase

class DiscountCompilerTest extends TestBase {

  "QualifierCompiler" - {
    ".compile" - {

      "returns error for unknown qualifier type" - {
        val qualifierType = "yolo"
        val attributes = "{}"
        val compiler = QualifierCompiler(qualifierType, attributes)
        leftValue(compiler.compile()) must === (UnknownQualifierFailure(qualifierType))
      }
    }
  }
}