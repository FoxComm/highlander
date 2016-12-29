import akka.http.scaladsl.model.StatusCodes

import failures.LockFailures._
import failures._
import models.Reasons
import models.account._
import models.cord._
import models.cord.lineitems._
import models.inventory.ProductVariants
import models.payment.giftcard._
import models.product.Mvp
import models.returns.Return.{Canceled, Processing}
import models.returns._
import models.shipping.{Shipments, ShippingMethods}
import payloads.ReturnPayloads._
import responses.{AllReturns, ReturnLockResponse, ReturnResponse}
import services.returns.{ReturnLineItemUpdater, ReturnLockUpdater}
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "Returns" - {
    pending

    "GET /v1/returns" - {
      "should return list of Returns" in new Fixture {
        val response = GET(s"v1/returns")
        response.status must === (StatusCodes.OK)

        val root = response.asTheResult[Seq[AllReturns.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }
    }

    "GET /v1/returns/customer/:id" - {
      "should return list of Returns of existing customer" in new Fixture {
        val response = GET(s"v1/returns/customer/${customer.accountId}")
        response.status must === (StatusCodes.OK)

        val root = response.asTheResult[Seq[AllReturns.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }

      "should return failure for non-existing customer" in new Fixture {
        val response = GET(s"v1/returns/customer/255")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(User, 255).description)
      }
    }

    "GET /v1/returns/order/:refNum" - {
      "should return list of Returns of existing order" in new Fixture {
        val response = GET(s"v1/returns/order/${order.refNum}")
        response.status must === (StatusCodes.OK)

        val root = response.asTheResult[Seq[AllReturns.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }

      "should return failure for non-existing order" in new Fixture {
        val response = GET(s"v1/returns/order/ABC-666")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "GET /v1/returns/:refNum" - {
      "should return valid Return by referenceNumber" in new Fixture {
        val response = GET(s"v1/returns/${rma.refNum}")
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.referenceNumber must === (rma.refNum)
      }

      "should return 404 if invalid rma is returned" in new Fixture {
        val response = GET(s"v1/returns/ABC-666")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }
    }

    "PATCH /v1/returns/:refNum" - {
      "successfully changes status of Return" in new Fixture {
        val response =
          PATCH(s"v1/returns/${rma.referenceNumber}", ReturnUpdateStatePayload(state = Processing))
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.state must === (Processing)
      }

      "successfully cancels Return with valid reason" in new Fixture {
        val payload  = ReturnUpdateStatePayload(state = Canceled, reasonId = Some(reason.id))
        val response = PATCH(s"v1/returns/${rma.referenceNumber}", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.state must === (Canceled)
      }

      "fails to cancel Return if invalid reason provided" in new Fixture {
        val response = PATCH(s"v1/returns/${rma.referenceNumber}",
                             ReturnUpdateStatePayload(state = Canceled, reasonId = Some(999)))
        response.status must === (StatusCodes.BadRequest)
        response.error must === (InvalidCancellationReasonFailure.description)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = PATCH(s"v1/returns/ABC-666", ReturnUpdateStatePayload(state = Processing))
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }
    }

    "POST /v1/returns" - {
      "successfully creates new Return" in new Fixture {
        val response =
          POST(s"v1/returns",
               ReturnCreatePayload(cordRefNum = order.refNum, returnType = Return.Standard))
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.referenceNumber must === (s"${order.refNum}.2")
        root.customer.head.id must === (order.accountId)
        root.storeAdmin.head.id must === (storeAdmin.accountId)
      }

      "fails to create Return with invalid order refNum provided" in new Fixture {
        val response =
          POST(s"v1/returns",
               ReturnCreatePayload(cordRefNum = "ABC-666", returnType = Return.Standard))
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Order, "ABC-666").description)
      }
    }

    "POST /v1/returns/:refNum/message" - {
      "successfully manipulates with message to the customer" in new Fixture {
        // Creates message
        val payload  = ReturnMessageToCustomerPayload(message = "Hello!")
        val response = POST(s"v1/returns/${rma.referenceNumber}/message", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.messageToCustomer.head must === (payload.message)

        // Edits (cleans) message
        val responseClean = POST(s"v1/returns/${rma.referenceNumber}/message",
                                 ReturnMessageToCustomerPayload(message = ""))
        responseClean.status must === (StatusCodes.OK)

        val rootClean = responseClean.as[ReturnResponse.Root]
        rootClean.messageToCustomer must === (None)
      }

      "fails if Return not found" in new Fixture {
        val payload  = ReturnMessageToCustomerPayload(message = "Hello!")
        val response = POST(s"v1/returns/99/message", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "99").description)
      }

      "fails if message is too long" in new Fixture {
        val payload = ReturnMessageToCustomerPayload(
            message = List.fill(Return.messageToAccountMaxLength)("Yax").mkString)
        val response = POST(s"v1/returns/99/message", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Message length got 3000, expected 1000 or less")
      }
    }

    "GET /v1/returns/:refNum/lock" - {
      "returns lock info on locked Return" in new Fixture {
        ReturnLockUpdater.lock("ABC-123.1", storeAdmin).gimme

        val response = GET(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnLockResponse.Root]
        root.isLocked must === (true)
        root.lock.head.lockedBy.id must === (storeAdmin.accountId)
      }

      "returns negative lock status on unlocked Return" in new Fixture {
        val response = GET(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnLockResponse.Root]
        root.isLocked must === (false)
        root.lock.isEmpty must === (true)
      }
    }

    "POST /v1/returns/:refNum/lock" - {
      "successfully locks an Return" in new Fixture {
        val response = POST(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val lockedRma = Returns.findByRefNum(rma.referenceNumber).gimme.head
        lockedRma.isLocked must === (true)

        val locks = ReturnLockEvents.findByRma(rma.id).gimme
        locks.length must === (1)
        val lock = locks.head
        lock.lockedBy must === (1)
      }

      "refuses to lock an already locked Return" in new Fixture {
        val response = POST(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (LockedFailure(Return, rma.referenceNumber).description)
      }

      "avoids race condition" in new Fixture {
        pending // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587
        def request = POST(s"v1/returns/${rma.referenceNumber}/lock")

        val responses = Seq(0, 1).par.map(_ ⇒ request)
        responses.map(_.status) must contain allOf (StatusCodes.OK, StatusCodes.BadRequest)
        ReturnLockEvents.gimme.length mustBe 1
      }
    }

    "POST /v1/returns/:refNum/unlock" - {
      "unlocks an Return" in new Fixture {
        POST(s"v1/returns/${rma.referenceNumber}/lock")

        val response = POST(s"v1/returns/${rma.referenceNumber}/unlock")
        response.status must === (StatusCodes.OK)

        val unlockedRma = Returns.findByRefNum(rma.referenceNumber).gimme.head
        unlockedRma.isLocked must === (false)
      }

      "refuses to unlock an already unlocked Return" in new Fixture {
        val response = POST(s"v1/returns/${rma.referenceNumber}/unlock")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotLockedFailure(Return, rma.refNum).description)
      }
    }

    "GET /v1/returns/:refNum/expanded" - {
      "should return expanded Return by referenceNumber" in new Fixture {
        val response = GET(s"v1/returns/${rma.refNum}/expanded")
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.RootExpanded]
        root.referenceNumber must === (rma.refNum)
        root.order.head.referenceNumber must === (order.refNum)
      }

      "should return 404 if invalid rma is returned" in new Fixture {
        val response = GET(s"v1/returns/ABC-666/expanded")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }
    }
  }

  "Return Line Items" - {
    pending

    // SKU Line Items
    "POST /v1/returns/:refNum/line-items/skus" - {
      "successfully adds SKU line item" in new LineItemFixture {
        val payload = ReturnSkuLineItemsPayload(sku = sku.code,
                                                quantity = 1,
                                                reasonId = returnReason.id,
                                                isReturnItem = true,
                                                inventoryDisposition = ReturnLineItem.Putaway)
        val response = POST(s"v1/returns/${rma.referenceNumber}/line-items/skus", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.lineItems.skus.headOption.value.sku.sku must === (sku.code)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val payload = ReturnSkuLineItemsPayload(sku = "ABC-666",
                                                quantity = 1,
                                                reasonId = returnReason.id,
                                                isReturnItem = true,
                                                inventoryDisposition = ReturnLineItem.Putaway)
        val response = POST(s"v1/returns/ABC-666/line-items/skus", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload = ReturnSkuLineItemsPayload(sku = "ABC-666",
                                                quantity = 1,
                                                reasonId = 100,
                                                isReturnItem = true,
                                                inventoryDisposition = ReturnLineItem.Putaway)
        val response = POST(s"v1/returns/${rma.referenceNumber}/line-items/skus", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(ReturnReason, 100).description)
      }

      "fails if quantity is invalid" in new LineItemFixture {
        val payload = ReturnSkuLineItemsPayload(sku = "ABC-666",
                                                quantity = 0,
                                                reasonId = returnReason.id,
                                                isReturnItem = true,
                                                inventoryDisposition = ReturnLineItem.Putaway)
        val response = POST(s"v1/returns/${rma.referenceNumber}/line-items/skus", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === ("Quantity got 0, expected more than 0")
      }
    }

    "DELETE /v1/returns/:refNum/line-items/skus/:id" - {
      "successfully deletes SKU line item" in new LineItemFixture {
        // Create
        val payload = ReturnSkuLineItemsPayload(sku = sku.code,
                                                quantity = 1,
                                                reasonId = returnReason.id,
                                                isReturnItem = true,
                                                inventoryDisposition = ReturnLineItem.Putaway)
        val updatedRma =
          ReturnLineItemUpdater.addSkuLineItem(rma.referenceNumber, payload, ctx).gimme
        val lineItemId = updatedRma.lineItems.skus.headOption.value.lineItemId

        // Delete
        val response = DELETE(s"v1/returns/${rma.referenceNumber}/line-items/skus/$lineItemId")
        response.status must === (StatusCodes.OK)
        val root = response.as[ReturnResponse.Root]
        root.lineItems.skus mustBe 'empty
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = DELETE(s"v1/returns/ABC-666/line-items/skus/1")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }

      "fails if line item ID is not found" in new LineItemFixture {
        val response = DELETE(s"v1/returns/${rma.referenceNumber}/line-items/skus/666")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(ReturnLineItem, 666).description)
      }
    }

    // Gift Card Line Items
    "POST /v1/returns/:refNum/line-items/gift-cards" - {
      "successfully adds gift card line item" in new LineItemFixture {
        val payload =
          ReturnGiftCardLineItemsPayload(code = giftCard.code, reasonId = returnReason.id)
        val response = POST(s"v1/returns/${rma.referenceNumber}/line-items/gift-cards", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.lineItems.giftCards.headOption.value.giftCard.code must === (giftCard.code)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val payload  = ReturnGiftCardLineItemsPayload(code = "ABC-666", reasonId = returnReason.id)
        val response = POST(s"v1/returns/ABC-666/line-items/gift-cards", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload  = ReturnGiftCardLineItemsPayload(code = "ABC-666", reasonId = 100)
        val response = POST(s"v1/returns/${rma.referenceNumber}/line-items/gift-cards", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(ReturnReason, 100).description)
      }
    }

    // Shipping Costs Line Items
    "POST /v1/returns/:refNum/line-items/shipping-costs" - {
      "successfully adds shipping cost line item" in new LineItemFixture {
        val payload = ReturnShippingCostLineItemsPayload(reasonId = reason.id)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/line-items/shipping-costs", payload)
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnResponse.Root]
        root.lineItems.shippingCosts.headOption.value.shippingCost.id must === (shipment.id)
      }

      "fails if refNum is not found" in new LineItemFixture {
        val payload  = ReturnShippingCostLineItemsPayload(reasonId = returnReason.id)
        val response = POST(s"v1/returns/ABC-666/line-items/shipping-costs", payload)

        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload = ReturnShippingCostLineItemsPayload(reasonId = 100)
        val response =
          POST(s"v1/returns/${rma.referenceNumber}/line-items/shipping-costs", payload)

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(ReturnReason, 100).description)
      }
    }

    "DELETE /v1/returns/:refNum/line-items/shipping-costs/:id" - {
      "successfully deletes shipping cost line item" in new LineItemFixture {
        // Create
        val payload = ReturnShippingCostLineItemsPayload(reasonId = returnReason.id)
        val updatedRma =
          ReturnLineItemUpdater.addShippingCostItem(rma.referenceNumber, payload).gimme
        val lineItemId = updatedRma.lineItems.shippingCosts.headOption.value.lineItemId

        // Delete
        val response =
          DELETE(s"v1/returns/${rma.referenceNumber}/line-items/shipping-costs/$lineItemId")
        response.status must === (StatusCodes.OK)
        val root = response.as[ReturnResponse.Root]
        root.lineItems.shippingCosts mustBe 'empty
      }

      "fails if refNum is not found" in new LineItemFixture {
        val response = DELETE(s"v1/returns/ABC-666/line-items/shipping-costs/1")
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, "ABC-666").description)
      }

      "fails if line item ID is not found" in new LineItemFixture {
        val response = DELETE(s"v1/returns/${rma.referenceNumber}/line-items/shipping-costs/666")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotFoundFailure400(ReturnLineItem, 666).description)
      }
    }
  }

  trait Fixture extends Order_Baked with Reason_Baked {
    val rma = Returns
      .create(Factories.rma.copy(orderRef = order.refNum, accountId = customer.accountId))
      .gimme
  }

  trait LineItemFixture extends Fixture {
    val (returnReason, sku, giftCard, shipment) = (for {
      returnReason ← * <~ ReturnReasons.create(Factories.returnReasons.head)
      product      ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku          ← * <~ ProductVariants.mustFindById404(product.skuId)
      _            ← * <~ addSkusToOrder(Seq(sku.id), order.refNum, OrderLineItem.Cart)

      gcReason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      gcOrigin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.accountId, reasonId = gcReason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = gcOrigin.id,
                                            originType = GiftCard.RmaProcess))

      shippingAddress ← * <~ OrderShippingAddresses.create(
                           Factories.shippingAddress.copy(cordRef = order.refNum, regionId = 1))
      shippingMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      orderShippingMethod ← * <~ OrderShippingMethods.create(
                               OrderShippingMethod.build(cordRef = order.refNum,
                                                         method = shippingMethod))
      shipment ← * <~ Shipments.create(Factories.shipment)
    } yield (returnReason, sku, giftCard, shipment)).gimme
  }

  def addSkusToOrder(skuIds: Seq[Int],
                     cordRef: String,
                     state: OrderLineItem.State): DbResultT[Unit] = {
    val itemsToInsert =
      skuIds.map(skuId ⇒ CartLineItem(cordRef = cordRef, productVariantId = skuId))
    CartLineItems.createAll(itemsToInsert).map(_ ⇒ Unit)
  }

}
