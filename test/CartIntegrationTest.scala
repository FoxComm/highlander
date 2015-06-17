import akka.http.scaladsl.model.StatusCodes
import models._
import org.joda.time.DateTime
import payloads.{CreateAddressPayload, CreditCardPayload}
import responses.FullCart

import org.json4s.DefaultFormats
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import util.{StripeSupport, DbTestSupport}

/**
 * The Server is shut down by shutting down the ActorSystem
 */
class CartIntegrationTest extends FreeSpec
  with MustMatchers
  with DbTestSupport
  with HttpSupport
  with AutomaticAuth
  with ScalaFutures {

  import concurrent.ExecutionContext.Implicits.global

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = Span(5, Seconds),
    interval = Span(20, Milliseconds)
  )

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "returns new items" in {
    val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = None)).futureValue

    val response = POST(
      s"v1/carts/$cartId/line-items",
       """
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val responseBody = response.bodyText
    val cart = parse(responseBody).extract[FullCart.Root]

    cart.lineItems.map(_.skuId).sortBy(identity) mustBe List(1, 5, 5)
  }

  "deletes line items" in {
    val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = None)).futureValue
    val seedLineItems = (1 to 2).map { _ => CartLineItem(id = 0, cartId = cartId, skuId = 1) }
    db.run(CartLineItems.returningId ++= seedLineItems.toSeq).futureValue

    val response = DELETE(s"v1/carts/$cartId/line-items/1")
    val responseBody = response.bodyText
    val cart = parse(responseBody).extract[FullCart.Root]

    cart.lineItems mustBe List(CartLineItem(id = 2, cartId = cartId, skuId = 1))
  }

  "handles credit cards" - {
    val today = new DateTime
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val payload = CreditCardPayload(holderName = "Jax", number = StripeSupport.successfulCard,
                                    cvv = "123", expYear = today.getYear + 1, expMonth = today.getMonthOfYear)

    "fails if the cart is not found" in {
      val response = POST(
        s"v1/carts/5/payment-methods/credit-card",
        payload)

      response.status mustBe StatusCodes.NotFound
    }

    "fails if the payload is invalid" in {
      val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = Some(1))).futureValue
      val response = POST(
        s"v1/carts/$cartId/payment-methods/credit-card",
        payload.copy(cvv = "", holderName = ""))

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]

      errors mustBe Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression '[0-9]{3,4}'"))
      response.status mustBe StatusCodes.BadRequest
    }

    "fails if the card is invalid according to Stripe" in {
      val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = Some(1))).futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val response = POST(
        s"v1/carts/$cartId/payment-methods/credit-card",
        payload.copy(number = StripeSupport.declinedCard))

      val body = response.bodyText
      val errors = parse(body).extract[Map[String, Seq[String]]]

      errors mustBe Map("errors" -> Seq("Your card was declined."))
      response.status mustBe StatusCodes.BadRequest
    }

    "successfully creates records" in {
      val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = Some(1))).futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val customer = customerStub.copy(id = customerId)
      val addressPayload = CreateAddressPayload(name = "Home", stateId = 46, state = Some("VA"), street1 = "500 Blah",
                                                city = "Richmond", zip = "50000")
      val payloadWithAddress = payload.copy(address = Some(addressPayload))

      val response = POST(
        s"v1/carts/$cartId/payment-methods/credit-card",
        payloadWithAddress)

      val body = response.bodyText

      val cc = CreditCardGateways.findById(1).futureValue.get
      val numAddresses = Addresses.count().futureValue
      val numBillingAddress = BillingAddresses.count().futureValue
      //val address = Addresses.findAllByCustomer(customer).futureValue.head
      val payment = AppliedPayments.findAllByCartId(cartId).futureValue.head

      val cart = parse(body).extract[FullCart.Root]

      cc.customerId mustBe customerId
      cc.lastFour mustBe payload.lastFour
      cc.expMonth mustBe payload.expMonth
      cc.expYear mustBe payload.expYear

      payment.appliedAmount mustBe 0
      payment.cartId mustBe cartId
      payment.status mustBe "auth"

      response.status mustBe StatusCodes.OK

      numBillingAddress mustBe 1
      numAddresses mustBe 1
    }
  }
}

