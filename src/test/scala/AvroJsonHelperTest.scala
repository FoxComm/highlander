import org.scalatest._
import org.json4s.jackson.JsonMethods.{compact, parse}
import consumer.AvroJsonHelper._

class AvroJsonHelperTest extends FlatSpec with Matchers {

  "transformJson method" should "rename all nested fields after unescaping" in {
    val inputJson =
      """
        | {
        |   "is_blacklisted": false,
        |   "orders": "[{\"reference_number\": \"BR10005\",\"grand_total\": 5040}]"
        | }""".stripMargin

    val result = transformJson(inputJson, List("orders"))

    result contains "is_blacklisted"    shouldBe false
    result contains "isBlacklisted"     shouldBe true

    result contains "reference_number"  shouldBe false
    result contains "referenceNumber"   shouldBe true

    result contains "grand_total"       shouldBe false
    result contains "grandTotal"        shouldBe true    
  }
}