package gatling.seeds.requests

import java.time.{Instant, LocalDateTime, ZoneId}

import scala.util.Random

import faker.Lorem
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.PaymentPayloads._

object CreditCards {

  val createCreditCard = http("Create credit card")
    .post("/v1/customers/${customerId}/payment-methods/credit-cards")
    .body(StringBody { session ⇒
      json(
          CreateCreditCardFromSourcePayload(
              holderName = session.get("customerName").as[String],
              cardNumber = session.get("ccNumber").as[String],
              cvv = Lorem.numerify("###"),
              expYear =
                LocalDateTime.ofInstant(Instant.now, ZoneId.systemDefault).getYear +
                  2 + Random.nextInt(5),
              expMonth = Random.nextInt(12) + 1,
              addressId = Some(session.get("addressId").as[Int])))
    })
    .check(jsonPath("$.id").ofType[Int].saveAs("creditCardId"))

  val payWithCreditCard = http("Pay with credit card")
    .post("/v1/orders/${referenceNumber}/payment-methods/credit-cards")
    .body(StringBody(session ⇒ json(CreditCardPayment(session.get("creditCardId").as[Int]))))

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
