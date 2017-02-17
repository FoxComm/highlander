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
import models.returns.Return._
import models.returns._
import org.scalatest.prop.PropertyChecks
import payloads.ReturnPayloads._
import responses.ReturnResponse.Root
import responses.{ReturnLockResponse, ReturnReasonsResponse, ReturnResponse}
import services.returns.{ReturnLineItemUpdater, ReturnLockUpdater, ReturnService}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories
import cats.implicits._
import models.Reason.Cancellation
import models.shipping.ShippingMethods

class ReturnIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
    with PropertyChecks {

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
        val payload =
          ReturnUpdateStatePayload(state = Canceled, reasonId = cancellationReason.id.some)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (
            Canceled)
      }

      "fail if return reason has wrong type" in new Fixture {
        assert(reason.reasonType != Cancellation)
        val payload = ReturnUpdateStatePayload(state = Canceled, reasonId = reason.id.some)
        returnsApi(rma.referenceNumber)
          .update(payload)
          .mustFailWith400(InvalidCancellationReasonFailure)
      }

      "Cancel state should be final " in new Fixture {
        private val canceled = returnsApi(rma.referenceNumber)
          .update(
              ReturnUpdateStatePayload(state = Canceled, reasonId = cancellationReason.id.some))
          .as[Root]

        canceled.state must === (Canceled)
        canceled.canceledReasonId must === (cancellationReason.id.some)

        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Pending, reasonId = cancellationReason.id.some))
          .mustFailWith400(
              StateTransitionNotAllowed(Return, "Canceled", "Pending", rma.referenceNumber))
      }

      "Returns should be fine with state transition " in new Fixture {
        returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root].state must === (Pending)

        private def state(s: State) = {
          ReturnUpdateStatePayload(state = s, reasonId = cancellationReason.id.some)
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

  "Return reasons" - {
    "get list of return reasons" in new LineItemFixture {
      val response = returnsApi.getReasonsList.as[Seq[ReturnReasonsResponse.Root]]
      response.nonEmpty must === (true)
      response.head.name must === (returnReason.name)
    }

    "add new return reason" in new LineItemFixture {
      val rr       = ReturnReasonPayload(name = "Simple reason")
      val response = returnsApi.addReturnReason(rr).as[ReturnReasonsResponse.Root]
      response.name must === (rr.name)
    }

    "remove return reason by id" in new LineItemFixture {
      returnsApi.removeReturnReason(returnReason.id)
      returnsApi.getReasonsList.as[Seq[ReturnReasonsResponse.Root]] mustBe 'empty

      info("must fail if returnReason was already deleted")
      returnsApi
        .removeReturnReason(returnReason.id)
        .mustFailWith404(NotFoundFailure404(ReturnReasons, returnReason.id))
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

  "Return line items" - {
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

        response.lineItems.shippingCosts.value.amount must === (orderShippingMethod.price)
      }

      "successfully adds SKU line item" in new LineItemFixture {
        val response =
          returnsApi(rma.referenceNumber).lineItems.add(skuPayload).as[ReturnResponse.Root]
        response.lineItems.skus.headOption.value.sku.sku must === (sku.code)
      }

      "overwrite existing shipping cost" in new LineItemFixture {
        returnsApi(rma.referenceNumber).lineItems
          .add(shippingCostPayload.copy(amount = 42))
          .as[ReturnResponse.Root]
          .lineItems
          .shippingCosts
          .value
          .amount must === (42)

        val response = returnsApi(rma.referenceNumber).lineItems
          .add(shippingCostPayload.copy(amount = 25))
          .as[ReturnResponse.Root]

        response.lineItems.shippingCosts.value.amount must === (25)
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

      "fails if quantity for sku is invalid" in new LineItemFixture {
        val payload = skuPayload.copy(quantity = -42)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Quantity got -42, expected more than 0")
      }

      "fails if amount for shipping cost is less then 0" in new LineItemFixture {
        val payload = shippingCostPayload.copy(amount = -666)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Amount got -666, expected more than 0")
      }

      "fails if amount for shipping cost is more then maximum allowed amount" in new LineItemFixture {
        val payload = shippingCostPayload.copy(amount = shippingCostPayload.amount + 666)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(ReturnShippingCostExceeded(rma.referenceNumber,
                                                      amount = shippingCostPayload.amount + 666,
                                                      maxAmount = orderShippingMethod.price))
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
    val (returnReason, sku, giftCard, orderShippingMethod) = (for {
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
    } yield (returnReason, sku, giftCard, orderShippingMethod)).gimme

    val giftCardPayload =
      ReturnGiftCardLineItemPayload(code = giftCard.code, reasonId = returnReason.id)

    val shippingCostPayload =
      ReturnShippingCostLineItemPayload(amount = orderShippingMethod.price, reasonId = reason.id)

    val skuPayload = ReturnSkuLineItemPayload(sku = sku.code,
                                              quantity = 1,
                                              reasonId = returnReason.id,
                                              isReturnItem = true,
                                              inventoryDisposition = ReturnLineItem.Putaway)

    val returnLineItemPayloadTable =
      Table("returnLineItemPayload", giftCardPayload, shippingCostPayload, skuPayload)
  }

}
