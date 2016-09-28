package seeds.requests

import java.time.{Instant, LocalDateTime, ZoneId}

import scala.util.Random

import faker.Lorem
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.AddressPayloads.CreateAddressPayload
import payloads.PaymentPayloads._
import routes.admin.CreditCardDetailsPayload
import seeds.requests.Auth._

object CreditCards {

  private val getStripeCcToken = http("Get Stripe credit card token")
    .post("/v1/credit-card-token")
    .requireAdminAuth
    .body(StringBody { session ⇒
      json(
          CreditCardDetailsPayload(customerId = session("customerId").as[Int],
                                   cardNumber = session("ccNumber").as[String],
                                   expMonth = Random.nextInt(12) + 1,
                                   expYear = LocalDateTime.now.getYear + Random.nextInt(5),
                                   cvv = Random.nextInt(899) + 100,
                                   address = addressPayload(session)))
    })
    .check(jsonPath("$.token").ofType[String].saveAs("creditCardStripeToken"),
           jsonPath("$.brand").ofType[String].saveAs("creditCardBrand"),
           jsonPath("$.lastFour").ofType[String].saveAs("creditCardLastFour"),
           jsonPath("$.expYear").ofType[Int].saveAs("creditCardExpYear"),
           jsonPath("$.expMonth").ofType[Int].saveAs("creditCardExpMonth"))

  private val createCreditCard = http("Create credit card")
    .post("/v1/customers/${customerId}/payment-methods/credit-cards")
    .requireAdminAuth
    .body(StringBody { session ⇒
      val address = CreateCcAddressPayload(addressPayload(session))
      json(
          CreateCreditCardFromTokenPayload(token = session.get("creditCardStripeToken").as[String],
                                           holderName = session.get("customerName").as[String],
                                           lastFour = session("creditCardLastFour").as[String],
                                           brand = session("creditCardBrand").as[String],
                                           expYear = session("creditCardExpYear").as[Int],
                                           expMonth = session("creditCardExpMonth").as[Int],
                                           billingAddress = address))
    })
    .check(jsonPath("$.id").ofType[Int].saveAs("creditCardId"))

  val createCreditCardWithStripe = exec(getStripeCcToken).exec(createCreditCard)

  val payWithCreditCard = http("Pay with credit card")
    .post("/v1/orders/${referenceNumber}/payment-methods/credit-cards")
    .body(StringBody(session ⇒ json(CreditCardPayment(session.get("creditCardId").as[Int]))))

  // If these is no credit card in session, create new
  // Otherwise create new credit card with probability of 30%
  // Otherwise pay with previously used credit card
  val payWithCc = feed(csv("data/credit_cards.csv").random)
    .doIfOrElse(session ⇒ session.contains("creditCardId")) {
      randomSwitch(
          30.0 → exec(createCreditCardWithStripe)
      )
    } {
      exec(createCreditCardWithStripe)
    }
    .exec(payWithCreditCard)

  private def addressPayload(session: Session): CreateAddressPayload =
    session("customerAddressPayload").as[CreateAddressPayload]
}
