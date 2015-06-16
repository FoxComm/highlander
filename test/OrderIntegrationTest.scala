import akka.http.scaladsl.model.StatusCodes
import models._
import payloads.CreditCardPayload
import responses.FullOrder

import org.json4s.DefaultFormats
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import util.DbTestSupport

/**
 * The Server is shut down by shutting down the ActorSystem
 */
class OrderIntegrationTest extends FreeSpec
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
    val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue

    val response = POST(
      s"v1/orders/$orderId/line-items",
       """
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val responseBody = response.bodyText
    val order = parse(responseBody).extract[FullOrder.Root]

    order.lineItems.map(_.skuId).sortBy(identity) mustBe List(1, 5, 5)
  }

  "deletes line items" in {
    val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
    val seedLineItems = (1 to 2).map { _ => OrderLineItem(id = 0, orderId = orderId, skuId = 1) }
    db.run(OrderLineItems.returningId ++= seedLineItems.toSeq).futureValue

    val response = DELETE(s"v1/orders/$orderId/line-items/1")
    val responseBody = response.bodyText
    val order = parse(responseBody).extract[FullOrder.Root]

    order.lineItems mustBe List(OrderLineItem(id = 2, orderId = orderId, skuId = 1))
  }

  "handles credit cards" - {
    val payload = CreditCardPayload(holderName = "Jax", number = "1234123412341234",
                                    cvv = "123", expYear = 2017, expMonth = 2)

    "fails if the order is not found" in {
      val response = POST(
        s"v1/orders/5/payment-methods/credit-card",
        payload)

      response.status mustBe (StatusCodes.NotFound)
    }

    "fails if the payload is invalid" in {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payload.copy(cvv = "", holderName = ""))

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]

      errors mustBe Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression '[0-9]{3,4}'"))
      response.status mustBe (StatusCodes.BadRequest)
    }

    "successfully creates records" in {
      val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue
      val response = POST(
        s"v1/orders/$orderId/payment-methods/credit-card",
        payload)

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]

      errors mustBe Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression '[0-9]{3,4}'"))
      response.status mustBe (StatusCodes.BadRequest)
    }
  }
}

