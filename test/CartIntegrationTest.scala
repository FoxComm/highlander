import akka.http.scaladsl.model.StatusCodes
import models._
import payloads.CreditCardPayload
import responses.FullCart

import org.json4s.DefaultFormats
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import util.DbTestSupport

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
    val today = new Date
    val payload = CreditCardPayload(holderName = "Jax", number = "4242424242424242",
                                    cvv = "123", expYear = 2017, expMonth = today.getMonth)

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

    "successfully creates records" in {
      val cartId = db.run(Carts.returningId += Cart(id = 0, accountId = Some(1))).futureValue
      val response = POST(
        s"v1/carts/$cartId/payment-methods/credit-card",
        payload)

      val errors = parse(response.bodyText).extract[Map[String, Seq[String]]]

      errors mustBe Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression '[0-9]{3,4}'"))
      response.status mustBe StatusCodes.BadRequest
    }
  }
}

