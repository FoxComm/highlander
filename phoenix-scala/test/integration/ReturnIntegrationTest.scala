import akka.http.scaladsl.model.StatusCodes
import failures.LockFailures._
import failures.ReturnFailures._
import failures._
import models.Reasons
import models.account._
import models.cord._
import models.inventory.Skus
import models.payment.giftcard._
import models.product.Mvp
import models.returns.Return.{Canceled, Processing}
import models.returns._
import models.shipping.{Shipments, ShippingMethods}
import org.scalatest.prop.PropertyChecks
import payloads.ReturnPayloads._
import responses.ReturnResponse.Root
import responses.{AllReturns, ReturnLockResponse, ReturnResponse}
import services.returns.{ReturnLineItemUpdater, ReturnLockUpdater, ReturnService}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
    with PropertyChecks {

  // I'm gonna move tests here once they're good
  "Revive Retuns" - {
    val orderRefNotExist = "ABC-666"

    "should get rma from fixture" in new Fixture {
      val response = returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root]
      response.referenceNumber must === (rma.referenceNumber)
      response.id must === (rma.id)
    }

    "successfully creates new Return" in new Fixture {
      val rmaCreated = returnsApi
        .create(ReturnCreatePayload(cordRefNum = order.refNum, returnType = Return.Standard))
        .as[ReturnResponse.Root]
      rmaCreated.referenceNumber must === (s"${order.refNum}.2")
      rmaCreated.customer.head.id must === (order.accountId)
      rmaCreated.storeAdmin.head.id must === (storeAdmin.accountId)
      println("in test rmaCreated.referenceNumber " + rmaCreated.referenceNumber)

      val getRmaRoot = returnsApi(rmaCreated.referenceNumber).get().as[ReturnResponse.Root]
      getRmaRoot.referenceNumber must === (rmaCreated.referenceNumber)
      getRmaRoot.id must === (rmaCreated.id)

    }

    "fails to create Return with invalid order refNum provided" in new Fixture {
      val response = returnsApi.create(
          ReturnCreatePayload(cordRefNum = orderRefNotExist, returnType = Return.Standard))
      response.mustFailWith404(NotFoundFailure404(Order, orderRefNotExist))
    }

    "PATCH /v1/returns/:refNum" - {
      "successfully changes status of Return" in new Fixture {
        private val payload = ReturnUpdateStatePayload(state = Processing)
        val response        = returnsApi(rma.referenceNumber).update(payload)
        val root            = response.as[ReturnResponse.Root]
        root.state must === (Processing)
      }

      "successfully cancels Return with valid reason" in new Fixture {
        val payload  = ReturnUpdateStatePayload(state = Canceled, reasonId = Some(reason.id))
        val response = returnsApi(rma.referenceNumber).update(payload)
        val root     = response.as[ReturnResponse.Root]
        root.state must === (Canceled)
      }

      "Cancel state should be final " in new Fixture {
        val response = returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Return.Canceled, reasonId = Some(reason.id)))
          .as[ReturnResponse.Root]
        response.state must === (Canceled)

        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Return.Pending, reasonId = Some(reason.id)))
          .mustFailWith400(StateTransitionNotAllowed(
                  "Transition from Canceled to Pending is not allowed for return with referenceNumber=" + rma.referenceNumber))
      }

      "Returns should be fine with state transition " in new Fixture {
        // start as pending
        returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root].state must === (
            Return.Pending)

        private def state(s: Return.State) = {
          ReturnUpdateStatePayload(state = s, reasonId = Some(reason.id))
        }

        returnsApi(rma.referenceNumber)
          .update(state(Return.Processing))
          .as[ReturnResponse.Root]
          .state must === (Return.Processing)

        returnsApi(rma.referenceNumber)
          .update(state(Return.Review))
          .as[ReturnResponse.Root]
          .state must === (Return.Review)

        returnsApi(rma.referenceNumber)
          .update(state(Return.Complete))
          .as[ReturnResponse.Root]
          .state must === (Return.Complete)

        returnsApi(rma.referenceNumber)
          .update(state(Return.Pending))
          .mustFailWith400(StateTransitionNotAllowed(
                  "Transition from Complete to Pending is not allowed for return with referenceNumber=" + rma.referenceNumber))
      }

      "fails to cancel Return if invalid reason provided" in new Fixture {
        private val payload = ReturnUpdateStatePayload(state = Canceled, reasonId = Some(999))
        val response        = returnsApi(rma.referenceNumber).update(payload)
        response.status must === (StatusCodes.BadRequest)
        response.error must === (InvalidCancellationReasonFailure.description)
      }

      "fails if Return is not found" in new Fixture {
        private val payload = ReturnUpdateStatePayload(state = Processing)
        val response        = returnsApi(orderRefNotExist).update(payload)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Return, orderRefNotExist).description)
      }
    }

    "GET /v1/returns" - {
      "should return list of Returns" in new Fixture {
        private val roots = returnsApi.get().as[Seq[Root]]
        roots.size must === (1)
      }
    }

    "GET /v1/returns/customer/:id" - {
      "should return list of Returns of existing customer" in new Fixture {
        val root = returnsApi.getByCustomer(customer.accountId).as[Seq[AllReturns.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }

      "should return failure for non-existing customer" in new Fixture {
        private val accId = 255
        val response      = returnsApi.getByCustomer(accId)
        response.status must === (StatusCodes.NotFound)
        response.error must === (NotFoundFailure404(Account, accId).description)
      }
    }

    "GET /v1/returns/order/:refNum" - {
      "should return list of Returns of existing order" in new Fixture {
        val root = returnsApi.getByOrder(order.referenceNumber).as[Seq[AllReturns.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (rma.refNum)
      }

      "should return failure for non-existing order" in new Fixture {
        val root = returnsApi.getByOrder(orderRefNotExist)
        root.status must === (StatusCodes.NotFound)
        root.error must === (NotFoundFailure404(Order, orderRefNotExist).description)
      }
    }
  }

  "Returns" - {
    pending

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

  "line-items" - {
    "POST /v1/returns/:refNum/line-items" - {
      "successfully adds gift card line item" in new LineItemFixture {
        pending

        val response =
          returnsApi(rma.referenceNumber).lineItems.add(giftCardPayload).as[ReturnResponse.Root]
        response.lineItems.giftCards.headOption.value.giftCard.code must === (giftCard.code)
      }

      "successfully adds shipping cost line item" in new LineItemFixture {
        val response = returnsApi(rma.referenceNumber).lineItems
          .add(shippingCostPayload)
          .as[ReturnResponse.Root]
        response.lineItems.shippingCosts.headOption.value.shippingCost.id must === (shipment.id)
      }

      "successfully adds SKU line item" in new LineItemFixture {
        val response =
          returnsApi(rma.referenceNumber).lineItems.add(skuPayload).as[ReturnResponse.Root]
        response.lineItems.skus.headOption.value.sku.sku must === (sku.code)
      }

      "fails if refNum is not found" in new LineItemFixture {
        returnsApi("ABC-666").lineItems
          .add(giftCardPayload)
          .mustFailWith404(NotFoundFailure404(Return, "ABC-666"))
      }

      "fails if reason is not found" in new LineItemFixture {
        val payload = shippingCostPayload.copy(reasonId = 666)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(ReturnReasonNotFoundFailure(666))
      }

      "fails if quantity is invalid" in new LineItemFixture {
        val payload = skuPayload.copy(quantity = -42)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Quantity got -42, expected more than 0")
      }
    }

    "DELETE /v1/returns/:refNum/line-items/:id" - {

      "successfully deletes gift card line item" in new LineItemFixture {
        pending

        val lineItemId = returnsApi(rma.referenceNumber).lineItems
          .add(giftCardPayload)
          .as[ReturnResponse.Root]
          .lineItems
          .giftCards
          .headOption
          .value
          .lineItemId

        val response =
          returnsApi(rma.referenceNumber).lineItems.remove(lineItemId).as[ReturnResponse.Root]
        response.lineItems.giftCards mustBe 'empty
      }

      "successfully deletes shipping cost line item" in new LineItemFixture {
        val lineItemId = returnsApi(rma.referenceNumber).lineItems
          .add(shippingCostPayload)
          .as[ReturnResponse.Root]
          .lineItems
          .shippingCosts
          .headOption
          .value
          .lineItemId

        val response =
          returnsApi(rma.referenceNumber).lineItems.remove(lineItemId).as[ReturnResponse.Root]
        response.lineItems.shippingCosts mustBe 'empty
      }

      "successfully deletes SKU line item" in new LineItemFixture {
        val lineItemId = returnsApi(rma.referenceNumber).lineItems
          .add(skuPayload)
          .as[ReturnResponse.Root]
          .lineItems
          .skus
          .headOption
          .value
          .lineItemId

        val response =
          returnsApi(rma.referenceNumber).lineItems.remove(lineItemId).as[ReturnResponse.Root]
        response.lineItems.skus mustBe 'empty
      }

      "fails if refNum is not found" in new LineItemFixture {
        returnsApi("ABC-666").lineItems
          .remove(1)
          .mustFailWith404(NotFoundFailure404(Return, "ABC-666"))
      }

      "fails if line item ID is not found" in new LineItemFixture {
        returnsApi(rma.referenceNumber).lineItems
          .remove(666)
          .mustFailWith404(NotFoundFailure404(ReturnLineItem, 666))
      }
    }
  }

  trait Fixture extends Order_Baked with Reason_Baked {
    def freshRma =
      Returns
        .create(Factories.rma.copy(orderRef = order.refNum, accountId = customer.accountId))
        .gimme

    val rma = freshRma
  }

  trait LineItemFixture extends Fixture {
    val (returnReason, sku, giftCard, shipment) = (for {
      returnReason ← * <~ ReturnReasons.create(Factories.returnReasons.head)
      product      ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku          ← * <~ Skus.mustFindById404(product.skuId)
      gcReason     ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      gcOrigin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.accountId, reasonId = gcReason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = gcOrigin.id,
                                            originType = GiftCard.RmaProcess))
      shippingMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      orderShippingMethod ← * <~ OrderShippingMethods.create(
                               OrderShippingMethod.build(cordRef = order.refNum,
                                                         method = shippingMethod))
      shipment ← * <~ Shipments.create(Factories.shipment.copy(cordRef = order.refNum))
    } yield (returnReason, sku, giftCard, shipment)).gimme

    val giftCardPayload =
      ReturnGiftCardLineItemPayload(code = giftCard.code, reasonId = returnReason.id)

    val shippingCostPayload = ReturnShippingCostLineItemPayload(reasonId = reason.id)

    val skuPayload = ReturnSkuLineItemPayload(sku = sku.code,
                                              quantity = 1,
                                              reasonId = returnReason.id,
                                              isReturnItem = true,
                                              inventoryDisposition = ReturnLineItem.Putaway)

    val returnLineItemPayloadTable =
      Table("returnLineItemPayload", giftCardPayload, shippingCostPayload, skuPayload)
  }

}
