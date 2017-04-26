package gatling.seeds.requests

import faker.Lorem
import io.circe.jackson.syntax._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.time.{Instant, LocalDateTime, ZoneId}
import payloads.PaymentPayloads._
import scala.util.Random
import utils.json.codecs._

object CreditCards {

  val createCreditCard = http("Create credit card")
    .post("/v1/customers/${customerId}/payment-methods/credit-cards")
    .body(StringBody { session ⇒

          CreateCreditCardFromSourcePayload(
              holderName = session.get("customerName").as[String],
              cardNumber = session.get("ccNumber").as[String],
              cvv = Lorem.numerify("###"),
              expYear =
                LocalDateTime.ofInstant(Instant.now, ZoneId.systemDefault).getYear +
                  2 + Random.nextInt(5),
              expMonth = Random.nextInt(12) + 1,
              addressId = Some(session.get("addressId").as[Int])).asJson.jacksonPrint
    })
    .check(jsonPath("$.id").ofType[Int].saveAs("creditCardId"))

  val payWithCreditCard = http("Pay with credit card")
    .post("/v1/orders/${referenceNumber}/payment-methods/credit-cards")
    .body(StringBody(session ⇒ CreditCardPayment(session.get("creditCardId").as[Int]).asJson.jacksonPrint))

  // If these is no credit card in session, create new
  // Otherwise create new credit card with probability of 30%
  // Otherwise pay with previously used credit card
  val payWithCc = feed(csv("data/credit_cards.csv").random)
    .doIfOrElse(session ⇒ session.contains("creditCardId")) {
      randomSwitch(
          30.0 → exec(createCreditCard)
      )
    } {
      exec(createCreditCard)
    }
    .exec(payWithCreditCard)
}
