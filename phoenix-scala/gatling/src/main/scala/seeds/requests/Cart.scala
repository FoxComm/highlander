package seeds.requests

import java.time.Instant

import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.{CreateCart, OrderTimeMachine}
import payloads.UpdateShippingMethod
import seeds.dbFeeder
import seeds.requests.Auth._
import seeds.requests.Payments._

object Cart {

  val newCart = http("Create cart")
    .post("/v1/orders")
    .requireAdminAuth
    .body(StringBody { session ⇒
      json(CreateCart(customerId = Some(session.get("customerId").as[Integer])))
    })
    .check(jsonPath("$.referenceNumber").ofType[String].saveAs("referenceNumber"))

  val addProductVariantsToCart = http("Add variants to cart")
    .post("/v1/orders/${referenceNumber}/line-items")
    .requireAdminAuth
    .body(StringBody(session ⇒ session.get("variantPayload").as[String]))

  // TODO ask #middlewarehouse if SKUs are available
  val pickRandomProductVariants = {
    val differentVariantsQty = Random.nextInt(5) + 1
    val numberOfItems        = Random.nextInt(3) + 1
    feed(dbFeeder("select form_id as variant_id from product_variants").random,
         _ ⇒ differentVariantsQty).exec { session ⇒
      def newPayloadItem(variantId: Int) =
        UpdateLineItemsPayload(variantId, numberOfItems, None)

      val payload =
        if (differentVariantsQty == 1)
          Seq(newPayloadItem(session.get("variant_id").as[Int]))
        else
          (1 until differentVariantsQty).map { i ⇒
            val variantId = session.get(s"variant_id$i").as[Int]
            newPayloadItem(variantId)
          }
      session.set("variantPayload", json(payload))
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
      json(UpdateShippingMethod(randomShippingMethodId))
    })

  val checkout = http("Checkout").post("/v1/orders/${referenceNumber}/checkout")

  val placeOrder = exec(newCart)
    .exec(pickRandomProductVariants)
    .exec(addProductVariantsToCart)
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
        json(
            OrderTimeMachine(
                referenceNumber = session.get("referenceNumber").as[String],
                placedAt = Instant.now.minusSeconds(
                    (Random.nextInt(15) * 60 * 60 * 24 * 30).toLong) // Minus ~15 months
            ))
      })
}
