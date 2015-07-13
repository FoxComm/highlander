import akka.http.scaladsl.model.StatusCodes
import models._
import org.joda.time.DateTime
import org.scalatest.time.{Milliseconds, Seconds, Span}
import payloads.{CreateAddressPayload, CreditCardPayload}
import responses.FullOrder
import util.{IntegrationTestBase, StripeSupport}

/**
 * The Server is shut down by shutting down the ActorSystem
 */
class OrderIntegrationTest extends IntegrationTestBase
  with HttpSupport /** FIXME: Add to IntegrationTestBase once they no longer live in the root package */
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global
  import org.json4s.jackson.JsonMethods._
  import Extensions._

  "returns new items" in {
    pending
    val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue

    val response = POST(
      s"v1/orders/$orderId/line-items",
       """
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val order = parse(response.bodyText).extract[FullOrder.Root]
    order.lineItems.map(_.skuId).sortBy(identity) must === (List(1, 5, 5))
  }

  "handles credit cards" - {
    val today = new DateTime
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val payload = CreditCardPayload(holderName = "Jax", number = StripeSupport.successfulCard, cvv = "123", expYear = today.getYear + 1, expMonth = today.getMonthOfYear)

    "fails if the order is not found" in {
      val response = POST(
        s"v1/orders/5/payment-methods/credit-card",
        payload)

      response.status must === (StatusCodes.NotFound)
    }

    "fails if the payload is invalid" in {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payload.copy(cvv = "", holderName = ""))

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]

      errors must === (Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression " +
        "'[0-9]{3,4}'")))
      response.status must === (StatusCodes.BadRequest)
    }

    "fails if the card is invalid according to Stripe" ignore {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payload.copy(number = StripeSupport.declinedCard))

      val body = response.bodyText
      val errors = parse(body).extract[Map[String, Seq[String]]]

      errors must === (Map("errors" -> Seq("Your card was declined.")))
      response.status must === (StatusCodes.BadRequest)
    }

    "successfully creates records" ignore {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val customer = customerStub.copy(id = customerId)
      val addressPayload = CreateAddressPayload(name = "Home", stateId = 46, state = Some("VA"), street1 = "500 Blah",
                                                city = "Richmond", zip = "50000")
      val payloadWithAddress = payload.copy(address = Some(addressPayload))

      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payloadWithAddress)

      val body = response.bodyText

      val cc = CreditCardGateways.findById(1).futureValue.get
      val payment = OrderPayments.findAllByOrderId(orderId).futureValue.head
      val (address, billingAddress) = BillingAddresses.findByPaymentId(payment.id).futureValue.get

      val order = parse(body).extract[FullOrder.Root]

      cc.customerId must === (customerId)
      cc.lastFour must === (payload.lastFour)
      cc.expMonth must === (payload.expMonth)
      cc.expYear must === (payload.expYear)

      payment.appliedAmount must === (0)
      payment.orderId must === (orderId)
      payment.status must === ("auth")

      response.status must === (StatusCodes.OK)

      address.stateId must === (addressPayload.stateId)
      address.customerId must === (customerId)
    }
  }
}

