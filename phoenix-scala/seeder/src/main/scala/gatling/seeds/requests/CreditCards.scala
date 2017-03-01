package gatling.seeds.requests

import java.time.LocalDateTime

import scala.util.Random

import faker.Lorem
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.PaymentPayloads._
import routes.admin.CreditCardDetailsPayload
import seeds._

object CreditCards {

  private val createCreditCardInStripe = http("Create credit card in Stripe")
    .post("/v1/credit-card-token")
    .body(StringBody { session ⇒
      json(
          CreditCardDetailsPayload(
              customerId = session.get("customerId").as[Int],
              cardNumber = session.get("ccNumber").as[String],
              expMonth = session.get("ccExpMonth").as[Int],
              expYear = session.get("ccExpYear").as[Int],
              cvv = Lorem.numerify("###").toInt,
              address = Addresses.getCustomerAddressFromSession(session)
          ))
    })
    .check(jsonPath("$.token").ofType[String].saveAs("creditCardToken"))
    .check(jsonPath("$.brand").ofType[String].saveAs("creditCardBrand"))

  private val createCreditCardInPhoenix = http("Create credit card")
    .post("/v1/customers/${customerId}/payment-methods/credit-cards")
    .body(StringBody { session ⇒
      json(
          CreateCreditCardFromTokenPayload(
              token = session.get("creditCardToken").as[String],
              brand = session.get("creditCardBrand").as[String],
              holderName = session.get("customerName").as[String],
              lastFour = session.get("ccNumber").as[String].takeRight(4),
              expYear = session.get("ccExpYear").as[Int],
              expMonth = session.get("ccExpMonth").as[Int],
              billingAddress = Addresses.getCustomerAddressFromSession(session),
              addressIsNew = false))
    })
    .check(jsonPath("$.id").ofType[Int].saveAs("creditCardId"))

  val payWithCreditCard = http("Pay with credit card")
    .post("/v1/orders/${referenceNumber}/payment-methods/credit-cards")
    .body(StringBody(session ⇒ json(CreditCardPayment(session.get("creditCardId").as[Int]))))

  val createCreditCard = exec(
      session ⇒
        session.setAll(("ccExpMonth", Random.nextInt(12) + 1),
                       ("ccExpYear", LocalDateTime.now.getYear + Random.nextInt(5) + 1)))
    .step(createCreditCardInStripe)
    .step(createCreditCardInPhoenix)

  // If these is no credit card in session, create new
  // Otherwise create new credit card with probability of 30%
  // Otherwise pay with previously used credit card
  val payWithCc = feed(csv("data/credit_cards.csv").random)
    .doIfOrElse(session ⇒ session.contains("creditCardId")) {
      randomSwitch(
          30.0 → createCreditCard
      )
    } {
      createCreditCard
    }
    .exec(payWithCreditCard)
}
