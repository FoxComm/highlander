import org.scalatest._
import org.json4s.jackson.JsonMethods.parse
import consumer.utils.JsonTransformers.{extractBigIntSeq, extractStringSeq}

class JsonTransformersTest extends FlatSpec with Matchers {

  "extractStringSeq method" should "properly extract string lists" in {
    val inputJsonString =
      """
        | {
        |   "giftCardCodes": ["ABC", "DEF", "GHI"],
        |   "storeCreditIds": [1, 2, 3, 4, 5]
        | }""".stripMargin

    val inputJson = parse(inputJsonString)

    extractStringSeq(inputJson, "giftCardCodes") shouldBe Seq("ABC", "DEF", "GHI")
    extractBigIntSeq(inputJson, "storeCreditIds") shouldBe Seq(1, 2, 3, 4, 5).map(_.toString)
  }
}