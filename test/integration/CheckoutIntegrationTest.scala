import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.NotFoundFailure404
import models.customer.Customers
import models.inventory.InventoryAdjustment._
import models.inventory._
import models.inventory.summary.SellableInventorySummaries
import models.location.Addresses
import models.objects._
import models.order.Order.Cart
import models.order.{Order, Orders}
import models.product.{Mvp, SimpleContext}
import models.shipping.ShippingMethods
import models.{Reasons, StoreAdmins}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateOrder
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.order.FullOrder
import responses.order.FullOrder.Root
import services.inventory.InventoryAdjustmentManager
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.seeds.generators.InventorySummaryGenerator

class CheckoutIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      // Create cart
      val createCart = POST("v1/orders", CreateOrder(Some(customer.id)))
      createCart.status must ===(StatusCodes.OK)
      val refNum = createCart.as[FullOrder.Root].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must ===(
          StatusCodes.OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must ===(StatusCodes.OK)
      // Set shipping method
      val setShipMethod = PATCH(s"v1/orders/$refNum/shipping-method",
                                UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must ===(StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[FullOrder.Root].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must ===(StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must ===(
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must ===(StatusCodes.OK)
      checkout.as[Root].orderState must ===(Order.RemorseHold)
      Orders.findOneByRefNum(refNum).run().futureValue.value.placedAt.value

      // Must adjust inventory on order placement
      val summary =
        SellableInventorySummaries.findOneById(sellableSummary.id).run().futureValue.value
      summary.onHand must ===(sellableSummary.onHand)
      summary.onHold must ===(sellableSummary.onHold + 2)
      summary.reserved must ===(sellableSummary.reserved)
      summary.safetyStock must ===(sellableSummary.safetyStock)

      val adjustments =
        InventoryAdjustments.findSellableBySummaryId(sellableSummary.id).result.run().futureValue
      adjustments must have size 1
      adjustments.filterNot(_.state == OnHold) mustBe empty
      val onHoldAdj = adjustments.headOption.value
      onHoldAdj.state must ===(OnHold)
      onHoldAdj.change must ===(2)
      onHoldAdj.newAfs must ===(sellableSummary.availableForSale - 2)
      onHoldAdj.newQuantity must ===(sellableSummary.onHold + 2)
    }

    "fails if AFS is zero" in new Fixture {

      //Create cart
      val refNum = POST("v1/orders", CreateOrder(Some(customer.id))).as[Root].referenceNumber

      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must ===(
          StatusCodes.OK)

      // Adjust inventory. Make AFS == 0
      private val wmsAdjustment: WmsOverride =
        WmsOverride(sku.id,
                    warehouse.id,
                    sellableSummary.onHand,
                    sellableSummary.onHold + sellableSummary.availableForSale,
                    sellableSummary.reserved)
      InventoryAdjustmentManager.applyWmsAdjustment(wmsAdjustment).runTxn().futureValue.rightVal

      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must ===(StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must ===(StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[Root].totals.total

      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must ===(StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must ===(
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must ===(StatusCodes.BadRequest)

      val order = Orders.findOneByRefNum(refNum).run().futureValue.value
      order.state must ===(Cart)
    }

    "errors 404 if no cart found by reference number" in {
      val response = POST("v1/orders/NOPE/checkout")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Order, "NOPE").description)
    }
  }

  trait Fixture extends InventorySummaryGenerator {
    val (customer, address, shipMethod, product, sku, reason, sellableSummary, warehouse) = (for {
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customer   ← * <~ Customers.create(Factories.customer)
      address    ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      product    ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku        ← * <~ Skus.mustFindById404(product.skuId)
      admin      ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason     ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      warehouse  ← * <~ Warehouses.create(Factories.warehouse)
      inventory  ← * <~ generateInventory(sku.id, warehouse.id)
    } yield (customer, address, shipMethod, product, sku, reason, inventory._1, warehouse))
      .run()
      .futureValue
      .rightVal
  }
}
