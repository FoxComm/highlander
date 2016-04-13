package seeds

import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.{CreateOrder, UpdateLineItemsPayload, UpdateShippingMethod}
import seeds.Auth._
import seeds.GatlingApp.dbFeeder

object Cart {

  val newCart = http("Create cart")
    .post("/v1/orders")
    .requireAdminAuth
    .body(StringBody(session ⇒ json(CreateOrder(customerId = Some(session.get("customerId").as[Integer])))))
    .check(status.is(200))
    .check(jsonPath("$..referenceNumber").ofType[String].saveAs("referenceNumber"))

  val addSkusToCart = http("Add SKUs to cart")
    .post("/v1/orders/${referenceNumber}/line-items")
    .requireAdminAuth
    .body(StringBody(session ⇒ session.get("skuPayload").as[String]))

  val pickRandomSkus = {
    val skuQty = Random.nextInt(5) + 1
    feed(dbFeeder("select code as sku from skus").random, _ ⇒ skuQty)
      .exec { session ⇒
        def newPayloadItem(skuCode: String) = UpdateLineItemsPayload(skuCode, Random.nextInt(10) + 1)

        val payload = if (skuQty == 1)
          Seq(newPayloadItem(session.get("sku").as[String]))
        else
          (1 until skuQty).map { i ⇒
            val skuCode = session.get(s"sku$i").as[String]
            newPayloadItem(skuCode)
          }
        session.set("skuPayload", json(payload))
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
      val shippingMethodIds = session.get("possibleShippingMethods").as[Seq[Int]]
      val randomShippingMethodId = shippingMethodIds(Random.nextInt(shippingMethodIds.size))
      json(UpdateShippingMethod(randomShippingMethodId))
    })

  val checkout = http("Checkout")
    .post("/v1/orders/${referenceNumber}/checkout")

  val placeOrder =
    exec(newCart)
      .exec(pickRandomSkus)
      .exec(addSkusToCart)
      .exec(setShippingAddress)
      .exec(findShippingMethods)
      .exec(setShippingMethod)
      .exec(CreditCards.payWithCc)
      .exec(checkout)

}
