import akka.http.scaladsl.model.StatusCodes
import failures.LockFailures._
import failures._
import models.Reasons
import models.account._
import models.cord._
import models.cord.lineitems._
import models.inventory.Skus
import models.payment.giftcard._
import models.product.Mvp
import models.returns.Return._
import models.returns._
import models.shipping.{Shipments, ShippingMethods}
import payloads.ReturnPayloads._
import responses.ReturnResponse.Root
import responses.{ReturnLockResponse, ReturnResponse}
import services.returns.{ReturnLineItemUpdater, ReturnLockUpdater, ReturnService}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.ReturnSeeds
import utils.seeds.Seeds.Factories
import cats.implicits._

class ReturnIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "Returns header" - {
    val orderRefNotExist = "ABC-666"

    "should get rma from fixture" in new Fixture {
      val response = returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root]
      response.referenceNumber must === (rma.referenceNumber)
      response.id must === (rma.id)
    }

    "successfully creates new Return" in new Fixture {
      val rmaCreated = returnsApi
        .create(ReturnCreatePayload(cordRefNum = order.refNum, returnType = Standard))
        .as[ReturnResponse.Root]
      rmaCreated.referenceNumber must === (s"${order.refNum}.2")
      rmaCreated.customer.head.id must === (order.accountId)
      rmaCreated.storeAdmin.head.id must === (storeAdmin.accountId)

      val getRmaRoot = returnsApi(rmaCreated.referenceNumber).get().as[ReturnResponse.Root]
      getRmaRoot.referenceNumber must === (rmaCreated.referenceNumber)
      getRmaRoot.id must === (rmaCreated.id)

    }

    "fails to create Return with invalid order refNum provided" in new Fixture {
      private val payload =
        ReturnCreatePayload(cordRefNum = orderRefNotExist, returnType = Standard)
      returnsApi.create(payload).mustFailWith404(NotFoundFailure404(Order, orderRefNotExist))
    }

    "PATCH /v1/returns/:refNum" - {
      "successfully changes status of Return" in new Fixture {
        private val payload = ReturnUpdateStatePayload(state = Processing)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (
            Processing)
      }

      "successfully cancels Return with valid reason" in new Fixture {
        val payload = ReturnUpdateStatePayload(state = Canceled, reasonId = reason.id.some)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (
            Canceled)
      }

      "Cancel state should be final " in new Fixture {
        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Canceled, reasonId = reason.id.some))
          .as[ReturnResponse.Root]
          .state must === (Canceled)

        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Pending, reasonId = reason.id.some))
          .mustFailWith400(
              StateTransitionNotAllowed(Return, "Canceled", "Pending", rma.referenceNumber))
      }

      "Returns should be fine with state transition " in new Fixture {
        returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root].state must === (Pending)

        private def state(s: State) = {
          ReturnUpdateStatePayload(state = s, reasonId = reason.id.some)
        }

        returnsApi(rma.referenceNumber)
          .update(state(Processing))
          .as[ReturnResponse.Root]
          .state must === (Processing)

        returnsApi(rma.referenceNumber)
          .update(state(Review))
          .as[ReturnResponse.Root]
          .state must === (Review)

        returnsApi(rma.referenceNumber)
          .update(state(Complete))
          .as[ReturnResponse.Root]
          .state must === (Complete)

        returnsApi(rma.referenceNumber)
          .update(state(Pending))
          .mustFailWith400(
              StateTransitionNotAllowed(Return, "Complete", "Pending", rma.referenceNumber))
      }

      "fails to cancel Return if invalid reason provided" in new Fixture {
        private val payload = ReturnUpdateStatePayload(state = Canceled, reasonId = 999.some)
        returnsApi(rma.referenceNumber)
          .update(payload)
          .mustFailWith400(InvalidCancellationReasonFailure)
      }

      "fails if Return is not found" in new Fixture {
        private val payload = ReturnUpdateStatePayload(state = Processing)
        returnsApi(orderRefNotExist)
          .update(payload)
          .mustFailWith404(NotFoundFailure404(Return, orderRefNotExist))
      }
    }

    "GET /v1/returns" - {
      "should return list of Returns" in new Fixture {
        returnsApi.get().as[Seq[Root]].size must === (1)
      }
    }

    "GET /v1/returns/customer/:id" - {
      "should return list of Returns of existing customer" in new Fixture {
        val root = returnsApi.getByCustomer(customer.accountId).as[Seq[ReturnResponse.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }

      "should return failure for non-existing customer" in new Fixture {
        private val accountId = 255
        returnsApi.getByCustomer(accountId).mustFailWith404(NotFoundFailure404(Account, accountId))
      }
    }

    "GET /v1/returns/order/:refNum" - {
      "should return list of Returns of existing order" in new Fixture {
        val root = returnsApi.getByOrder(order.referenceNumber).as[Seq[ReturnResponse.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }

      "should return failure for non-existing order" in new Fixture {
        returnsApi
          .getByOrder(orderRefNotExist)
          .mustFailWith404(NotFoundFailure404(Order, orderRefNotExist))
      }
    }

    "POST /v1/returns/:refNum/message" - {
      "successfully sends message to the customer" in new Fixture {
        val payload = ReturnMessageToCustomerPayload(message = "Hello!")
        returnsApi(rma.refNum)
          .message(payload)
          .as[ReturnResponse.Root]
          .messageToCustomer
          .head must === (payload.message)

        returnsApi(rma.refNum)
          .message(ReturnMessageToCustomerPayload(message = ""))
          .as[ReturnResponse.Root]
          .messageToCustomer must === (None)
      }

      "fails if Return not found" in new Fixture {
        private val rmaId = "99"
        val payload       = ReturnMessageToCustomerPayload(message = "Hello!")
        returnsApi(rmaId).message(payload).mustFailWith404(NotFoundFailure404(Return, rmaId))
      }

      "fails if message is too long" in new Fixture {
        val payload = ReturnMessageToCustomerPayload(
            message = List.fill(messageToAccountMaxLength)("Yax").mkString)
        returnsApi(rma.refNum)
          .message(payload)
          .mustFailWith400(GeneralFailure("Message length got 3000, expected 1000 or less"))
      }
    }
  }

  "Return locks" - { // todo implement later, not critical for mvp
    pending

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
        pending

        // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587
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
      sku          ← * <~ Skus.mustFindById404(product.skuId)
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
    val itemsToInsert = skuIds.map(skuId ⇒ CartLineItem(cordRef = cordRef, skuId = skuId))
    CartLineItems.createAll(itemsToInsert).map(_ ⇒ Unit)
  }

}
