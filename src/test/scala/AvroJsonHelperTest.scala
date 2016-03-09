import org.scalatest._
import consumer.AvroJsonHelper._

class AvroJsonHelperTest extends FlatSpec with Matchers {

  "transformJson method" should "rename all nested fields after unescaping" in {
    val inputJson =
      """
        | {
        |   "is_blacklisted": false,
        |   "orders": "[{\"reference_number\": \"BR10005\",\"grand_total\": 5040}]",
        |   "images": "[\"BR10005\", \"BR10006\"]"
        | }""".stripMargin

    val result = transformJson(inputJson, List("orders", "images"))

    result contains "is_blacklisted"    shouldBe false
    result contains "isBlacklisted"     shouldBe true

    result contains "reference_number"  shouldBe false
    result contains "referenceNumber"   shouldBe true

    result contains "grand_total"       shouldBe false
    result contains "grandTotal"        shouldBe true
    
    result contains """["BR10005","BR10006"]""" shouldBe true
  } 
}