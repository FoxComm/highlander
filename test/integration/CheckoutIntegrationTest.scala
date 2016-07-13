import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.NotFoundFailure404
import models.cord.Order.RemorseHold
import models.cord.{Cart, Carts, Order, Orders}
import models.customer.Customers
import models.inventory._
import models.location.Addresses
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.shipping.ShippingMethods
import models.{Reasons, StoreAdmins}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cart.FullCart
import responses.order.FullOrder
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      // Create cart
      val createCart = POST("v1/orders", CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[FullCart.Root].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[FullCart.Root].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.OK)
      checkout.as[FullOrder.Root].orderState must === (Order.RemorseHold)
      Orders.findOneByRefNum(refNum).gimme mustBe defined
      Carts.findOneByRefNum(refNum).gimme must not be defined
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      //Create cart
      val refNum =
        POST("v1/orders", CreateCart(Some(customer.id))).as[FullOrder.Root].referenceNumber

      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)

      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[FullOrder.Root].totals.total

      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.OK)

      val order = Orders.findOneByRefNum(refNum).gimme.value
      order.state must === (RemorseHold)
    }

    "errors 404 if no cart found by reference number" in {
      val response = POST("v1/orders/NOPE/checkout")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
    }
  }

  trait Fixture {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customer   ← * <~ Customers.create(Factories.customer)
      address    ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      product    ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku        ← * <~ Skus.mustFindById404(product.skuId)
      admin      ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason     ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }
}
