import java.time.Instant

import Extensions._
import akka.http.scaladsl.model.StatusCodes

import models.customer.{Customer, Customers}
import models.inventory.Skus
import models.order.lineitems._
import models.order._
import models.payment.giftcard.{GiftCard, GiftCardManual, GiftCardManuals, GiftCards}
import models.rma._
import Rma.{Canceled, Processing}
import models.shipping.{Shipments, ShippingMethods}
import models.{Reasons, StoreAdmins}
import models.product.{Mvp, ProductContexts, SimpleContext}
import payloads.{RmaCreatePayload, RmaGiftCardLineItemsPayload, RmaMessageToCustomerPayload, RmaShippingCostLineItemsPayload, RmaSkuLineItemsPayload}
import responses.{AllRmas, RmaLockResponse, RmaResponse}
import services.rmas.{RmaLineItemUpdater, RmaLockUpdater}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories
import utils.time._
import scala.concurrent.ExecutionContext.Implicits.global

import failures.LockFailures.{LockedFailure, NotLockedFailure}
import failures.{InvalidCancellationReasonFailure, NotFoundFailure400, NotFoundFailure404}

class RmaIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  "RMAs" - {
    pending

    "GET /v1/rmas" - {
      "should return list of RMAs" in new Fixture {
        val response = GET(s"v1/rmas")
        response.status must ===(StatusCodes.OK)

        val root = response.ignoreFailuresAndGiveMe[Seq[AllRmas.Root]]
        root.size must === (1)
        root.head.referenceNumber must ===(rma.refNum)
      }
    }

    "GET /v1/rmas/customer/:id" - {
      "should return list of RMAs of existing customer" in new Fixture {
        val response = GET(s"v1/rmas/customer/${customer.id}")
        response.status must ===(StatusCodes.OK)

        val root = response.ignoreFailuresAndGiveMe[Seq[AllRmas.Root]]
        root.size must === (1)
        root.head.referenceNumber must ===(rma.refNum)
      }

      "should return failure for non-existing customer" in new Fixture {
        val response = GET(s"v1/rmas/customer/255")
        response.status must ===(StatusCodes.NotFound)
        response.error must ===(NotFoundFailure404(Customer, 255).description)
      }
    }

    "GET /v1/rmas/order/:refNum" - {
      "should return list of RMAs of existing order" in new Fixture {
        val response = GET(s"v1/rmas/order/${order.refNum}")
        response.status must ===(StatusCodes.OK)

        val root = response.ignoreFailuresAndGiveMe[Seq[AllRmas.Root]]
        root.size must === (1)
        root.head.referenceNumber must ===(rma.refNum)
      }

      "should return failure for non-existing order" in new Fixture {
        val response = GET(s"v1/rmas/order/ABC-666")
        response.status must ===(StatusCodes.NotFound)
        response.error must ===(NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "GET /v1/rmas/:refNum" - {
      "should return valid RMA by referenceNumber" in new Fixture {
        val response = GET(s"v1/rmas/${rma.refNum}")
        response.status must ===(StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.referenceNumber must ===(rma.refNum)
      }

      "should return 404 if invalid rma is returned" in new Fixture {
        val response = GET(s"v1/rmas/ABC-666")
        response.status must ===(StatusCodes.NotFound)
        response.error must ===(NotFoundFailure404(Rma, "ABC-666").description)
      }
    }

    "PATCH /v1/rmas/:refNum" - {
      "successfully changes status of RMA" in new Fixture {
        val response = PATCH(s"v1/rmas/${rma.referenceNumber}", payloads.RmaUpdateStatePayload(state = Processing))
        response.status must ===(StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.state must ===(Processing)
      }

      "successfully cancels RMA with valid reason" in new Fixture {
        val payload = payloads.RmaUpdateStatePayload(state = Canceled, reasonId = Some(reason.id))
        val response = PATCH(s"v1/rmas/${rma.referenceNumber}", payload)
        response.status must ===(StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.state must ===(Canceled)
      }

      "fails to cancel RMA if invalid reason provided" in new Fixture {
        val response = PATCH(s"v1/rmas/${rma.referenceNumber}", payloads.RmaUpdateStatePayload(state = Canceled,
          reasonId = Some(999)))
        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(InvalidCancellationReasonFailure.description)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = PATCH(s"v1/rmas/ABC-666", payloads.RmaUpdateStatePayload(state = Processing))
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }
    }

    "POST /v1/rmas" - {
      "successfully creates new RMA" in new Fixture {
        val response = POST(s"v1/rmas", RmaCreatePayload(orderRefNum = order.refNum, rmaType = Rma.Standard))
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.referenceNumber must === (s"${order.refNum}.2")
        root.customer.head.id must === (order.customerId)
        root.storeAdmin.head.id must === (storeAdmin.id)
      }

      "fails to create RMA with invalid order refNum provided" in new Fixture {
        val response = POST(s"v1/rmas", RmaCreatePayload(orderRefNum = "ABC-666", rmaType = Rma.Standard))
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "POST /v1/rmas/:refNum/message" - {
      "successfully manipulates with message to the customer" in new Fixture {
        // Creates message
        val payload = RmaMessageToCustomerPayload(message = "Hello!")
        val response = POST(s"v1/rmas/${rma.referenceNumber}/message", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.messageToCustomer.head must === (payload.message)

        // Edits (cleans) message
        val responseClean = POST(s"v1/rmas/${rma.referenceNumber}/message", RmaMessageToCustomerPayload(message = ""))
        responseClean.status must === (StatusCodes.OK)

        val rootClean = responseClean.as[RmaResponse.Root]
        rootClean.messageToCustomer must === (None)
      }

      "fails if RMA not found" in new Fixture {
        val payload = RmaMessageToCustomerPayload(message = "Hello!")
        val response = POST(s"v1/rmas/99/message", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "99").description)
      }

      "fails if message is too long" in new Fixture {
        val payload = RmaMessageToCustomerPayload(message = List.fill(Rma.messageToCustomerMaxLength)("Yax").mkString)
        val response = POST(s"v1/rmas/99/message", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Message length got 3000, expected 1000 or less")
      }
    }

    "GET /v1/rmas/:refNum/lock" - {
      "returns lock info on locked RMA" in {
        Customers.create(Factories.customer).run().futureValue.rightVal
        Orders.create(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue.rightVal
        val rma = Rmas.create(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue.rightVal
        val admin = StoreAdmins.create(Factories.storeAdmin).run().futureValue.rightVal

        RmaLockUpdater.lock("ABC-123.1", admin).futureValue

        val response = GET(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaLockResponse.Root]
        root.isLocked must === (true)
        root.lock.head.lockedBy.id must === (admin.id)
      }

      "returns negative lock status on unlocked RMA" in {
        Customers.create(Factories.customer).run().futureValue.rightVal
        Orders.create(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue.rightVal
        val rma = Rmas.create(Factories.rma.copy(referenceNumber = "ABC-123.1")).run().futureValue.rightVal

        val response = GET(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaLockResponse.Root]
        root.isLocked must === (false)
        root.lock.isEmpty must === (true)
      }
    }

    "POST /v1/rmas/:refNum/lock" - {
      "successfully locks an RMA" in new Fixture {
        val response = POST(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val lockedRma = Rmas.findByRefNum(rma.referenceNumber).result.run().futureValue.head
        lockedRma.isLocked must === (true)

        val locks = RmaLockEvents.findByRma(rma.id).result.run().futureValue
        locks.length must === (1)
        val lock = locks.head
        lock.lockedBy must === (1)
      }

      "refuses to lock an already locked RMA" in {
        Customers.create(Factories.customer).run().futureValue.rightVal
        Orders.create(Factories.order.copy(referenceNumber = "ABC-123")).run().futureValue.rightVal
        val rma = Rmas.create(Factories.rma.copy(referenceNumber = "ABC-123.1", isLocked = true)).run().futureValue.rightVal

        val response = POST(s"v1/rmas/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (LockedFailure(Rma, rma.referenceNumber).description)
      }

      "avoids race condition" in new Fixture {
        pending // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587
        def request = POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val responses = Seq(0, 1).par.map(_ ⇒ request)
        responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
        RmaLockEvents.result.run().futureValue.length mustBe 1
      }
    }

    "POST /v1/rmas/:refNum/unlock" - {
      "unlocks an RMA" in new Fixture {
        POST(s"v1/rmas/${rma.referenceNumber}/lock")

        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")
        response.status must ===(StatusCodes.OK)

        val unlockedRma = Rmas.findByRefNum(rma.referenceNumber).result.run().futureValue.head
        unlockedRma.isLocked must ===(false)
      }

      "refuses to unlock an already unlocked RMA" in new Fixture {
        val response = POST(s"v1/rmas/${rma.referenceNumber}/unlock")

        response.status must ===(StatusCodes.BadRequest)
        response.error must ===(NotLockedFailure(Rma, rma.refNum).description)
      }
    }

    "GET /v1/rmas/:refNum/expanded" - {
      "should return expanded RMA by referenceNumber" in new Fixture {
        val response = GET(s"v1/rmas/${rma.refNum}/expanded")
        response.status must ===(StatusCodes.OK)

        val root = response.as[RmaResponse.RootExpanded]
        root.referenceNumber must ===(rma.refNum)
        root.order.head.referenceNumber must ===(order.refNum)
      }

      "should return 404 if invalid rma is returned" in new Fixture {
        val response = GET(s"v1/rmas/ABC-666/expanded")
        response.status must ===(StatusCodes.NotFound)
        response.error must ===(NotFoundFailure404(Rma, "ABC-666").description)

      }
    }
  }

  "RMA Line Items" - {
    pending

    // SKU Line Items
    "POST /v1/rmas/:refNum/line-items/skus" - {
      "successfully adds SKU line item" in new LineItemFixture {
        val payload = RmaSkuLineItemsPayload(sku = sku.code, quantity = 1, reasonId = rmaReason.id,
          isReturnItem = true, inventoryDisposition = RmaLineItem.Putaway)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/skus", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.lineItems.skus.headOption.value.sku.sku must === (sku.code)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val payload = RmaSkuLineItemsPayload(sku = "ABC-666", quantity = 1, reasonId = rmaReason.id,
          isReturnItem = true, inventoryDisposition = RmaLineItem.Putaway)
        val response = POST(s"v1/rmas/ABC-666/line-items/skus", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload = RmaSkuLineItemsPayload(sku = "ABC-666", quantity = 1, reasonId = 100,
          isReturnItem = true, inventoryDisposition = RmaLineItem.Putaway)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/skus", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(RmaReason, 100).description)
      }

      "fails if quantity is invalid" in new LineItemFixture {
        val payload = RmaSkuLineItemsPayload(sku = "ABC-666", quantity = 0, reasonId = rmaReason.id,
          isReturnItem = true, inventoryDisposition = RmaLineItem.Putaway)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/skus", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Quantity got 0, expected more than 0")
      }
    }

    "DELETE /v1/rmas/:refNum/line-items/skus/:id" - {
      "successfully deletes SKU line item" in new LineItemFixture {
        // Create
        val payload = RmaSkuLineItemsPayload(sku = sku.code, quantity = 1, reasonId = rmaReason.id,
          isReturnItem = true, inventoryDisposition = RmaLineItem.Putaway)
        val updatedRma = RmaLineItemUpdater.addSkuLineItem(rma.referenceNumber, payload, productContext).futureValue.rightVal
        val lineItemId = updatedRma.lineItems.skus.headOption.value.lineItemId

        // Delete
        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/line-items/skus/$lineItemId")
        response.status must === (StatusCodes.OK)
        val root = response.as[RmaResponse.Root]
        root.lineItems.skus mustBe 'empty
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = DELETE(s"v1/rmas/ABC-666/line-items/skus/1")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }

      "fails if line item ID is not found" in new LineItemFixture {
        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/line-items/skus/666")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(RmaLineItem, 666).description)
      }
    }

    // Gift Card Line Items
    "POST /v1/rmas/:refNum/line-items/gift-cards" - {
      "successfully adds gift card line item" in new LineItemFixture {
        val payload = RmaGiftCardLineItemsPayload(code = giftCard.code, reasonId = rmaReason.id)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/gift-cards", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.lineItems.giftCards.headOption.value.giftCard.code must === (giftCard.code)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val payload = RmaGiftCardLineItemsPayload(code = "ABC-666", reasonId = rmaReason.id)
        val response = POST(s"v1/rmas/ABC-666/line-items/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload = RmaGiftCardLineItemsPayload(code = "ABC-666", reasonId = 100)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(RmaReason, 100).description)
      }
    }

    "DELETE /v1/rmas/:refNum/line-items/gift-cards/:id" - {
      "successfully deletes gift card line item" in new LineItemFixture {
        // Create
        val payload = RmaGiftCardLineItemsPayload(code = giftCard.code, reasonId = rmaReason.id)
        val updatedRma = RmaLineItemUpdater.addGiftCardLineItem(rma.referenceNumber, payload).futureValue.rightVal
        val lineItemId = updatedRma.lineItems.giftCards.headOption.value.lineItemId

        // Delete
        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/line-items/gift-cards/$lineItemId")
        response.status must === (StatusCodes.OK)
        val root = response.as[RmaResponse.Root]
        root.lineItems.giftCards mustBe 'empty
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = DELETE(s"v1/rmas/ABC-666/line-items/gift-cards/1")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }

      "fails if line item ID is not found" in new LineItemFixture {
        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/line-items/gift-cards/666")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(RmaLineItem, 666).description)
      }
    }

    // Shipping Costs Line Items
    "POST /v1/rmas/:refNum/line-items/shipping-costs" - {
      "successfully adds shipping cost line item" in new LineItemFixture {
        val payload = RmaShippingCostLineItemsPayload(reasonId = reason.id)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/shipping-costs", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[RmaResponse.Root]
        root.lineItems.shippingCosts.headOption.value.shippingCost.id must === (shipment.id)
      }


      "fails if refNum is not found" in new LineItemFixture {
        val payload = RmaShippingCostLineItemsPayload(reasonId = rmaReason.id)
        val response = POST(s"v1/rmas/ABC-666/line-items/shipping-costs", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload = RmaShippingCostLineItemsPayload(reasonId = 100)
        val response = POST(s"v1/rmas/${rma.referenceNumber}/line-items/shipping-costs", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(RmaReason, 100).description)
      }
    }

    "DELETE /v1/rmas/:refNum/line-items/shipping-costs/:id" - {
      "successfully deletes shipping cost line item" in new LineItemFixture {
        // Create
        val payload = RmaShippingCostLineItemsPayload(reasonId = rmaReason.id)
        val updatedRma = RmaLineItemUpdater.addShippingCostItem(rma.referenceNumber, payload).futureValue.rightVal
        val lineItemId = updatedRma.lineItems.shippingCosts.headOption.value.lineItemId

        // Delete
        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/line-items/shipping-costs/$lineItemId")
        response.status must === (StatusCodes.OK)
        val root = response.as[RmaResponse.Root]
        root.lineItems.shippingCosts mustBe 'empty
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = DELETE(s"v1/rmas/ABC-666/line-items/shipping-costs/1")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Rma, "ABC-666").description)
      }

      "fails if line item ID is not found" in new LineItemFixture {
        val response = DELETE(s"v1/rmas/${rma.referenceNumber}/line-items/shipping-costs/666")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(RmaLineItem, 666).description)
      }
    }
  }

  trait Fixture {
    val (storeAdmin, customer, order, rma, reason) = (for {
      storeAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order ← * <~ Orders.create(Factories.order.copy(
        state = Order.RemorseHold,
        customerId = customer.id,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
      rma ← * <~ Rmas.create(Factories.rma.copy(
        orderId = order.id,
        orderRefNum = order.referenceNumber,
        customerId = customer.id))
      reason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = storeAdmin.id))
    } yield (storeAdmin, customer, order, rma, reason)).runTxn().futureValue.rightVal
  }

  trait LineItemFixture extends Fixture {
    val (productContext, rmaReason, sku, giftCard, shipment) = (for {
      rmaReason ← * <~ RmaReasons.create(Factories.rmaReasons.head)
      productContext ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
      product     ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      sku ← * <~ Skus.mustFindById404(product.skuId)
      _ ← * <~ Factories.addSkusToOrder(Seq(sku.id), order.id, OrderLineItem.Cart)

      gcReason ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = storeAdmin.id))
      gcOrigin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = storeAdmin.id, reasonId = gcReason.id))
      giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = gcOrigin.id, originType = GiftCard.RmaProcess))

      gcLineItem ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(orderId = order.id, giftCardId = giftCard.id))
      lineItem2 ← * <~ OrderLineItems.create(OrderLineItem(originId = gcLineItem.id,
        originType = OrderLineItem.GiftCardItem, orderId = order.id))

      shippingAddress ← * <~ OrderShippingAddresses.create(Factories.shippingAddress.copy(orderId = order.id,
        regionId = 1))
      shippingMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      orderShippingMethod ← * <~ OrderShippingMethods.create(
        OrderShippingMethod.build(order = order, method = shippingMethod))
      shipment ← * <~ Shipments.create(Factories.shipment)
    } yield (productContext, rmaReason, sku, giftCard, shipment)).runTxn().futureValue.rightVal
  }
}
