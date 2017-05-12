package payloads

import java.time.Instant
import org.json4s.JsonAST._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import payloads.EntityExportPayloads.FieldCalculation
import testutils.TestBase

class EntityExportPayloadsTest extends TestBase with PropertyChecks {
  "FieldCalculation.State" - {
    val Active   = "\"Active\""
    val Inactive = "\"Inactive\""
    val now      = Instant.now()

    "should be active if current date is between activeFrom and activeTo" in {
      val obj = JObject("activeFrom" → JString(now.minusSeconds(10).toString),
                        "activeTo"             → JString(now.plusSeconds(60).toString))
      FieldCalculation.State.calculate("state" → obj) must === (Active)
    }

    "should be active if current date is after activeFrom and activeTo is null" in {
      val obj = JObject("activeFrom" → JString(now.minusSeconds(10).toString), "activeTo" → JNull)
      FieldCalculation.State.calculate("state" → obj) must === (Active)
    }

    "should be inactive if archiveAt is defined" in {
      val obj = JObject("activeFrom" → JString(now.minusSeconds(10).toString),
                        "activeTo"             → JString(now.plusSeconds(60).toString),
                        "archivedAt"           → JString(now.toString))
      FieldCalculation.State.calculate("state" → obj) must === (Inactive)
    }

    "should be inactive if activeFrom is null" in {
      val obj1 = JObject("activeTo"              → JString(now.plusSeconds(60).toString))
      val obj2 = obj1.merge(JObject("activeFrom" → JNull))

      FieldCalculation.State.calculate("state" → obj1) must === (Inactive)
      FieldCalculation.State.calculate("state" → obj2) must === (Inactive)
    }

    "should react only to state field" in {
      forAll(Gen.alphaStr.suchThat(_ != "state")) { field ⇒
        FieldCalculation.State.calculate.isDefinedAt(field → JObject()) must === (false)
      }
    }
  }
}
