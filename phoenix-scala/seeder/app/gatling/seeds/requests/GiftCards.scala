package gatling.seeds.requests

import gatling.seeds.dbFeeder
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import phoenix.models.Reason
import phoenix.payloads.GiftCardPayloads._
import phoenix.payloads.PaymentPayloads.GiftCardPayment
import core.utils.Money.Currency
import core.utils.Strings._

import scala.util.Random

object GiftCards {

  private val reasonType = Reason.GiftCardCreation.toString.lowerCaseFirstLetter
  private val reasonFeeder = dbFeeder(
    s"""select id as "reasonId" from reasons where reason_type = '$reasonType'""")

  val createGiftCard = http("Create gift card")
    .post("/v1/gift-cards")
    .body(
      StringBody(
        session ⇒
          json(
            GiftCardCreateByCsr(
              reasonId = session.get("reasonId").as[Int],
              currency = Currency.USD,
              balance = Random.nextInt(9500).toLong + 500 // from $5 to $100
            ))))
    .check(jsonPath("$.code").ofType[String].saveAs("giftCardCode"))

  val payWithGiftCard = http("Pay with gift card")
    .post("/v1/orders/${referenceNumber}/payment-methods/gift-cards")
    .body(StringBody(session ⇒ json(GiftCardPayment(code = session.get("giftCardCode").as[String]))))

  val payWithGc = feed(reasonFeeder.random).exec(createGiftCard).exec(payWithGiftCard)
}
