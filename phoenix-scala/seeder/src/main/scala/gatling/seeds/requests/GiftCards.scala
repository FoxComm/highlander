package gatling.seeds.requests

import gatling.seeds.dbFeeder
import io.circe.jackson.syntax._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import models.Reason
import payloads.GiftCardPayloads._
import payloads.PaymentPayloads.GiftCardPayment
import scala.util.Random
import utils.Money.Currency
import utils.Strings._

object GiftCards {

  private val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter
  private val reasonFeeder = dbFeeder(
      s"""select id as "reasonId" from reasons where reason_type = '$reasonType'""")

  val createGiftCard = http("Create gift card")
    .post("/v1/gift-cards")
    .body(
        StringBody(
            session ⇒
              GiftCardCreateByCsr(
                      reasonId = session.get("reasonId").as[Int],
                      currency = Currency.USD,
                      balance = Random.nextInt(9500) + 500 // from $5 to $100
                  ).asJson.jacksonPrint))
    .check(jsonPath("$.code").ofType[String].saveAs("giftCardCode"))

  val payWithGiftCard = http("Pay with gift card")
    .post("/v1/orders/${referenceNumber}/payment-methods/gift-cards")
    .body(StringBody(session ⇒
              GiftCardPayment(code = session.get("giftCardCode").as[String]).asJson.jacksonPrint))

  val payWithGc = feed(reasonFeeder.random).exec(createGiftCard).exec(payWithGiftCard)
}
