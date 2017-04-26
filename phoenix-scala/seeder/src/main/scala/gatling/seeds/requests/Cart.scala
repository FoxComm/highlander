package gatling.seeds.requests

import gatling.seeds.dbFeeder
import gatling.seeds.requests.Payments._
import io.circe.jackson.syntax._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.time.Instant
import payloads.CartPayloads.CreateCart
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.OrderTimeMachine
import payloads.UpdateShippingMethod
import scala.util.Random
import utils.json.codecs._

object Cart {

  val newCart = http("Create cart")
    .post("/v1/orders")
    .requireAdminAuth
    .body(StringBody { session ⇒
      CreateCart(customerId = Some(session.get("customerId").as[Integer])).asJson.jacksonPrint
    })
    .check(jsonPath("$.referenceNumber").ofType[String].saveAs("referenceNumber"))

  val addSkusToCart = http("Add SKUs to cart")
    .post("/v1/orders/${referenceNumber}/line-items")
    .requireAdminAuth
    .body(StringBody(session ⇒ session.get("skuPayload").as[String]))

  // TODO ask #middlewarehouse if SKUs are available
  val pickRandomSkus = {
    val skusInOrder   = Random.nextInt(5) + 1
    val numberOfItems = Random.nextInt(3) + 1
    feed(dbFeeder("select code as sku from skus").random, _ ⇒ skusInOrder).exec { session ⇒
      def newPayloadItem(skuCode: String) = UpdateLineItemsPayload(skuCode, numberOfItems, None)

      val payload =
        if (skusInOrder == 1)
          Seq(newPayloadItem(session.get("sku").as[String]))
        else
          (1 until skusInOrder).map { i ⇒
            val skuCode = session.get(s"sku$i").as[String]
            newPayloadItem(skuCode)
          }
      session.set("skuPayload", payload.asJson.jacksonPrint)
    }
  }

  val setShippingAddress = http("Set shipping address")
    .patch("/v1/orders/${referenceNumber}/shipping-address/${addressId}")
    .requireAdminAuth

  val findShippingMethods = http("Get shipping methods for order")
    .get("/v1/shipping-methods/${referenceNumber}")
    .requireAdminAuth
    .check(jsonPath("$..id").ofType[Int].findAll.saveAs("possibleShippingMethods"))

  val setShippingMethod = http("Set shipping method")
    .patch("/v1/orders/${referenceNumber}/shipping-method")
    .requireAdminAuth
    .body(StringBody { session ⇒
      val shippingMethodIds      = session.get("possibleShippingMethods").as[Seq[Int]]
      val randomShippingMethodId = shippingMethodIds(Random.nextInt(shippingMethodIds.size))
      UpdateShippingMethod(randomShippingMethodId).asJson.jacksonPrint
    })

  val checkout = http("Checkout").post("/v1/orders/${referenceNumber}/checkout")

  val placeOrder = exec(newCart)
    .exec(pickRandomSkus)
    .exec(addSkusToCart)
    .exec(setShippingAddress)
    .exec(findShippingMethods)
    .exec(setShippingMethod)
    .exec(pay)
    .exec(checkout)

  val ageOrder =
    http("Age order")
      .post("/v1/order-time-machine")
      .requireAdminAuth
      .body(StringBody { session ⇒

            OrderTimeMachine(
                referenceNumber = session.get("referenceNumber").as[String],
                placedAt = Instant.now.minusSeconds(
                    (Random.nextInt(15) * 60 * 60 * 24 * 30).toLong) // Minus ~15 months
            ).asJson.jacksonPrint
      })
}
