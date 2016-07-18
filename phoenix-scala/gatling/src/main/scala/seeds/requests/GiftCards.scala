package seeds.requests

import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import models.Reason
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.GiftCardPayloads._
import payloads.PaymentPayloads.GiftCardPayment
import seeds.dbFeeder
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
              json(GiftCardCreateByCsr(
                      reasonId = session.get("reasonId").as[Int],
                      currency = Currency.USD,
                      balance = Random.nextInt(9500) + 500 // from $5 to $100
                  ))))
    .check(jsonPath("$.code").ofType[String].saveAs("giftCardCode"))

  val payWithGiftCard = http("Pay with gift card")
    .post("/v1/orders/${referenceNumber}/payment-methods/gift-cards")
    .body(StringBody(session ⇒
              json(GiftCardPayment(code = session.get("giftCardCode").as[String]))))

  val payWithGc = feed(reasonFeeder.random).exec(createGiftCard).exec(payWithGiftCard)
}
