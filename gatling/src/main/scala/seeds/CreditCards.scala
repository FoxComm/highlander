package seeds

import java.time.{Instant, LocalDateTime, ZoneId}

import scala.util.Random

import faker.Lorem
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.{CreateCreditCard, CreditCardPayment}

object CreditCards {

  val createCreditCard = http("Create credit card")
    .post("/v1/customers/${customerId}/payment-methods/credit-cards")
    .body(StringBody { session ⇒ json(CreateCreditCard(
      holderName = session.get("customerName").as[String],
      number = session.get("ccNumber").as[String],
      cvv = Lorem.numerify("###"),
      expYear = LocalDateTime.ofInstant(Instant.now, ZoneId.systemDefault).getYear + 2 + Random.nextInt(5),
      expMonth = Random.nextInt(12) + 1,
      addressId = Some(session.get("addressId").as[Int])))
    })
    .check(jsonPath("$.id").ofType[Int].saveAs("ccId"))

  val payWithCreditCard = http("Pay with credit card")
    .post("/v1/orders/${referenceNumber}/payment-methods/credit-cards")
    .body(StringBody(session ⇒ json(CreditCardPayment(session.get("ccId").as[Int]))))

  implicit class CreditCard(builder: ScenarioBuilder) {
    def createCcAndPay = builder
      .feed(csv("data/credit_cards.csv").random)
      .exec(createCreditCard)
      .exec(payWithCreditCard)
  }

}
