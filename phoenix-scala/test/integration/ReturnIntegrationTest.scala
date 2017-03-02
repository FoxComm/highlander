import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import failures.LockFailures._
import failures.ReturnFailures._
import failures._
import models.Reason.Cancellation
import models.account._
import models.cord._
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCardCharges
import models.returns.Return._
import models.returns._
import org.scalatest.prop.PropertyChecks
import payloads.PaymentPayloads.CreateManualStoreCredit
import payloads.ReturnPayloads._
import responses.ReturnResponse.Root
import responses._
import services.returns.ReturnLockUpdater
import testutils._
import testutils.fixtures.api.ApiFixtureHelpers
import testutils.fixtures.{BakedFixtures, ReturnsFixtures}

class ReturnIntegrationTest
    extends IntegrationTestBase
    with ApiFixtureHelpers
    with AutomaticAuth
    with BakedFixtures
    with ReturnsFixtures
    with PropertyChecks {

  "Returns header" - {
    val orderRefNotExist = "ABC-666"

    "successfully creates new Return" in new ReturnFixture with OrderDefaults {
      val rmaCreated = returnsApi
        .create(ReturnCreatePayload(cordRefNum = order.referenceNumber, returnType = Standard))
        .as[ReturnResponse.Root]
      rmaCreated.referenceNumber must === (s"${order.referenceNumber}.1")
      rmaCreated.customer.head.id must === (customer.accountId)
      rmaCreated.storeAdmin.head.id must === (storeAdmin.accountId)

      val getRmaRoot = returnsApi(rmaCreated.referenceNumber).get().as[ReturnResponse.Root]
      getRmaRoot.referenceNumber must === (rmaCreated.referenceNumber)
      getRmaRoot.id must === (rmaCreated.id)
    }

    "should get rma" in new ReturnFixture with OrderDefaults {
      val expected = createReturn(order.referenceNumber)
      returnsApi(expected.referenceNumber)
        .get()
        .as[ReturnResponse.Root]
        .copy(totals = None) must === (expected)
    }

    "fails to create Return with invalid order refNum provided" in {
      val payload = ReturnCreatePayload(cordRefNum = orderRefNotExist, returnType = Standard)
      returnsApi.create(payload).mustFailWith404(NotFoundFailure404(Order, orderRefNotExist))
    }

    "PATCH /v1/returns/:refNum" - {
      "successfully changes status of Return" in new ReturnDefaults {
        val payload = ReturnUpdateStatePayload(state = Processing)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (
            Processing)
      }

      "successfully cancels Return with valid reason" in new ReturnDefaults {
        val payload =
          ReturnUpdateStatePayload(state = Canceled, reasonId = cancellationReason.id.some)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (
            Canceled)
      }

      "fail if return reason has wrong type" in new ReturnDefaults {
        assert(reason.reasonType != Cancellation)
        val payload = ReturnUpdateStatePayload(state = Canceled, reasonId = reason.id.some)
        returnsApi(rma.referenceNumber)
          .update(payload)
          .mustFailWith400(InvalidCancellationReasonFailure)
      }

      "Cancel state should be final " in new ReturnDefaults {
        val canceled = returnsApi(rma.referenceNumber)
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

      "Returns should be fine with state transition " in new ReturnDefaults {
        returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root].state must === (Pending)

        def state(s: State) = {
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

      "fails if Return is not found" in new ReturnDefaults {
        returnsApi(orderRefNotExist)
          .update(ReturnUpdateStatePayload(state = Processing))
          .mustFailWith404(NotFoundFailure404(Return, orderRefNotExist))
      }
    }

    "GET /v1/returns" - {
      "should return list of Returns" in new ReturnFixture with OrderDefaults {
        createReturn(order.referenceNumber)
        createReturn(order.referenceNumber)

        returnsApi.get().as[Seq[Root]].size must === (2)
      }
    }

    "GET /v1/returns/customer/:id" - {
      "should return list of Returns of existing customer" in new ReturnFixture
      with OrderDefaults {
        val expected = createReturn(order.referenceNumber)
        val root     = returnsApi.getByCustomer(customer.accountId).as[Seq[ReturnResponse.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (expected.referenceNumber)
      }

      "should return failure for non-existing customer" in new ReturnFixture {
        val accountId = 255
        returnsApi.getByCustomer(accountId).mustFailWith404(NotFoundFailure404(Account, accountId))
      }
    }

    "GET /v1/returns/order/:refNum" - {
      "should return list of Returns of existing order" in new ReturnFixture with OrderDefaults {
        val expected = createReturn(order.referenceNumber)
        val root     = returnsApi.getByOrder(order.referenceNumber).as[Seq[ReturnResponse.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (expected.referenceNumber)
      }

      "should return failure for non-existing order" in new ReturnFixture {
        returnsApi
          .getByOrder(orderRefNotExist)
          .mustFailWith404(NotFoundFailure404(Order, orderRefNotExist))
      }
    }

    "POST /v1/returns/:refNum/message" - {
      "successfully sends message to the customer" in new ReturnDefaults {
        val payload = ReturnMessageToCustomerPayload(message = "Hello!")
        returnsApi(rma.referenceNumber)
          .message(payload)
          .as[ReturnResponse.Root]
          .messageToCustomer
          .head must === (payload.message)

        returnsApi(rma.referenceNumber)
          .message(ReturnMessageToCustomerPayload(message = ""))
          .as[ReturnResponse.Root]
          .messageToCustomer must === (None)
      }

      "fails if Return not found" in new ReturnDefaults {
        val rmaId   = "99"
        val payload = ReturnMessageToCustomerPayload(message = "Hello!")
        returnsApi(rmaId).message(payload).mustFailWith404(NotFoundFailure404(Return, rmaId))
      }

      "fails if message is too long" in new ReturnDefaults {
        val payload = ReturnMessageToCustomerPayload(
            message = List.fill(messageToAccountMaxLength)("Yax").mkString)
        returnsApi(rma.referenceNumber)
          .message(payload)
          .mustFailWith400(GeneralFailure("Message length got 3000, expected 1000 or less"))
      }
    }
  }

  "Return reasons" - {
    "add new return reason" in new ReturnReasonFixture {
      val payload = ReturnReasonPayload(name = "Simple reason")
      returnsApi.reasons.add(payload).as[ReturnReasonsResponse.Root].name must === (payload.name)
    }

    "get list of return reasons" in new ReturnReasonFixture {
      val expected = createReturnReason("whatever")
      returnsApi.reasons
        .list()
        .as[Seq[ReturnReasonsResponse.Root]] must contain theSameElementsAs List(expected)
    }

    "remove return reason by id" in new ReturnReasonDefaults {
      returnsApi.reasons.remove(returnReason.id)
      returnsApi.reasons.list().as[Seq[ReturnReasonsResponse.Root]] mustBe 'empty

      info("must fail if returnReason was already deleted")
      returnsApi.reasons
        .remove(returnReason.id)
        .mustFailWith404(NotFoundFailure404(ReturnReasons, returnReason.id))
    }
  }

  "Return locks" - { // todo implement later, not critical for mvp
    pending

    "GET /v1/returns/:refNum/lock" - {
      "returns lock info on locked Return" in new ReturnDefaults {
        ReturnLockUpdater.lock("ABC-123.1", storeAdmin).gimme

        val response = GET(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnLockResponse.Root]
        root.isLocked must === (true)
        root.lock.head.lockedBy.id must === (storeAdmin.accountId)
      }

      "returns negative lock status on unlocked Return" in new ReturnDefaults {
        val response = GET(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val root = response.as[ReturnLockResponse.Root]
        root.isLocked must === (false)
        root.lock.isEmpty must === (true)
      }
    }

    "POST /v1/returns/:refNum/lock" - {
      "successfully locks an Return" in new ReturnDefaults {
        val response = POST(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.OK)

        val lockedRma = Returns.findByRefNum(rma.referenceNumber).gimme.head
        lockedRma.isLocked must === (true)

        val locks = ReturnLockEvents.findByRma(rma.id).gimme
        locks.length must === (1)
        val lock = locks.head
        lock.lockedBy must === (1)
      }

      "refuses to lock an already locked Return" in new ReturnDefaults {
        val response = POST(s"v1/returns/${rma.referenceNumber}/lock")
        response.status must === (StatusCodes.BadRequest)
        response.error must === (LockedFailure(Return, rma.referenceNumber).description)
      }

      "avoids race condition" in new ReturnDefaults {
        // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587
        def request = POST(s"v1/returns/${rma.referenceNumber}/lock")

        val responses = Seq(0, 1).par.map(_ ⇒ request)
        responses.map(_.status) must contain allOf (StatusCodes.OK, StatusCodes.BadRequest)
        ReturnLockEvents.gimme.length mustBe 1
      }
    }

    "POST /v1/returns/:refNum/unlock" - {
      "unlocks an Return" in new ReturnDefaults {
        POST(s"v1/returns/${rma.referenceNumber}/lock")

        val response = POST(s"v1/returns/${rma.referenceNumber}/unlock")
        response.status must === (StatusCodes.OK)

        val unlockedRma = Returns.findByRefNum(rma.referenceNumber).gimme.head
        unlockedRma.isLocked must === (false)
      }

      "refuses to unlock an already unlocked Return" in new ReturnDefaults {
        val response = POST(s"v1/returns/${rma.referenceNumber}/unlock")

        response.status must === (StatusCodes.BadRequest)
        response.error must === (NotLockedFailure(Return, rma.referenceNumber).description)
      }
    }

  }

  "Return line items" - {
    "POST /v1/returns/:refNum/line-items" - {
      "successfully adds gift card line item" in new ReturnDefaults with ReturnReasonDefaults {
        pending

        val payload =
          ReturnGiftCardLineItemPayload(code = giftCard.code, reasonId = returnReason.id)
        val response =
          returnsApi(rma.referenceNumber).lineItems.add(payload).as[ReturnResponse.Root]
        response.lineItems.giftCards.headOption.value.giftCard.code must === (giftCard.code)
      }

      "successfully adds shipping cost line item" in new ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = reason.id)
        val response =
          returnsApi(rma.referenceNumber).lineItems.add(payload).as[ReturnResponse.Root]
        response.lineItems.shippingCosts.value.amount must === (order.totals.shipping)
      }

      "successfully adds SKU line item" in new ReturnDefaults with ReturnReasonDefaults {
        val payload = ReturnSkuLineItemPayload(sku = product.code,
                                               quantity = 1,
                                               reasonId = returnReason.id,
                                               isReturnItem = true,
                                               inventoryDisposition = ReturnLineItem.Putaway)
        val response =
          returnsApi(rma.referenceNumber).lineItems.add(payload).as[ReturnResponse.Root]
        response.lineItems.skus.headOption.value.sku.sku must === (product.code)
      }

      "overwrites existing shipping cost" in new ReturnLineItemFixture with ReturnDefaults
      with ReturnReasonDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = reason.id)
        val first = createReturnLineItem(payload, rma.referenceNumber)
        first.lineItems.shippingCosts.value.amount must === (order.totals.shipping)

        val second = createReturnLineItem(payload.copy(amount = 42), rma.referenceNumber)
        second.lineItems.shippingCosts.value.amount must === (42)
      }

      "fails if refNum is not found" in {
        val payload = ReturnShippingCostLineItemPayload(amount = 666, reasonId = 666)
        returnsApi("ABC-666").lineItems
          .add(payload)
          .mustFailWith404(NotFoundFailure404(Return, "ABC-666"))
      }

      "fails if reason is not found" in new ReturnDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = 666)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(ReturnReasonNotFoundFailure(666))
      }

      "fails if quantity for sku is invalid" in new ReturnDefaults with ReturnReasonDefaults {
        val payload = ReturnSkuLineItemPayload(sku = product.code,
                                               quantity = -42,
                                               reasonId = returnReason.id,
                                               isReturnItem = true,
                                               inventoryDisposition = ReturnLineItem.Putaway)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Quantity got -42, expected more than 0")
      }

      "fails if amount for shipping cost is less then 0" in new ReturnDefaults
      with ReturnReasonDefaults {
        val payload = ReturnShippingCostLineItemPayload(amount = -666, reasonId = reason.id)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Amount got -666, expected more than 0")
      }

      "fails if amount for shipping cost is more then maximum allowed amount" in new ReturnDefaults
      with ReturnReasonDefaults {
        val payload = ReturnShippingCostLineItemPayload(amount = order.totals.shipping + 666,
                                                        reasonId = reason.id)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(ReturnShippingCostExceeded(rma.referenceNumber,
                                                      amount = payload.amount,
                                                      maxAmount = order.totals.shipping))
      }

      "sets max shipping cost based on order total shipping cost minus any previous shipping cost returns for that order" in new ReturnLineItemFixture
      with ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = reason.id)

        // create some other return for different order
        val otherOrderRef = createDefaultOrder().referenceNumber
        val otherRmaRef   = createReturn(otherOrderRef).referenceNumber
        createReturnLineItem(payload = payload.copy(amount = 100), refNum = otherRmaRef)

        // create some other return for the same order
        val previousRmaRef = createReturn(order.referenceNumber).referenceNumber
        createReturnLineItem(payload.copy(amount = 25), previousRmaRef)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(ReturnShippingCostExceeded(rma.referenceNumber,
                                                      amount = payload.amount,
                                                      maxAmount = order.totals.shipping - 25))
      }
    }

    "DELETE /v1/returns/:refNum/line-items/:id" - {

      "successfully deletes gift card line item" in new ReturnLineItemDefaults {
        pending

        val lineItemId =
          createReturnLineItem(giftCardPayload, rma.referenceNumber).lineItems.giftCards.headOption.value.lineItemId

        returnsApi(rma.referenceNumber).lineItems
          .remove(lineItemId)
          .as[ReturnResponse.Root]
          .lineItems
          .giftCards mustBe 'empty
      }

      "successfully deletes shipping cost line item" in new ReturnLineItemDefaults {
        returnsApi(rma.referenceNumber).lineItems
          .remove(shippingCostItemId)
          .as[ReturnResponse.Root]
          .lineItems
          .shippingCosts mustBe 'empty
      }

      "successfully deletes SKU line item" in new ReturnLineItemDefaults {
        returnsApi(rma.referenceNumber).lineItems
          .remove(skuItemId)
          .as[ReturnResponse.Root]
          .lineItems
          .skus mustBe 'empty
      }

      "fails if refNum is not found" in {
        returnsApi("ABC-666").lineItems
          .remove(1)
          .mustFailWith404(NotFoundFailure404(Return, "ABC-666"))
      }

      "fails if line item ID is not found" in new ReturnDefaults {
        returnsApi(rma.referenceNumber).lineItems
          .remove(666)
          .mustFailWith404(NotFoundFailure404(ReturnLineItem, 666))
      }
    }
  }

  "Return payment methods" - {
    "POST /v1/returns/:ref/payment-methods" - {
      "succeeds for bulk insert" in new ReturnPaymentDefaults {
        val payload = ReturnPaymentsPayload(
            Map(PaymentMethod.CreditCard → 100, PaymentMethod.StoreCredit → 120))
        val response =
          returnsApi(rma.referenceNumber).paymentMethods.add(payload).as[ReturnResponse.Root]

        response.payments must have size 2
        response.payments.map(payment ⇒ payment.paymentMethodType → payment.amount) must
          contain theSameElementsAs payload.payments
      }

      "succeeds for any supported payment" in new ReturnPaymentFixture with ReturnDefaults
      with ReturnReasonDefaults {
        forAll(paymentMethodTable) { paymentType ⇒
          val order = createDefaultOrder()
          val rma   = createReturn(orderRef = order.referenceNumber)
          val shippingCostPayload =
            ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = reason.id)
          createReturnLineItem(shippingCostPayload, rma.referenceNumber)

          val payload = ReturnPaymentPayload(amount = shippingCostPayload.amount)
          val response = returnsApi(rma.referenceNumber).paymentMethods
            .add(paymentType, payload)
            .as[ReturnResponse.Root]

          response.payments must have size 1
          response.payments.head.paymentMethodType must === (paymentType)
          response.payments.head.amount must === (payload.amount)
        }
      }

      "fails if the amount is less than zero" in new ReturnPaymentFixture with OrderDefaults {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload = ReturnPaymentPayload(amount = -42)

          val response =
            returnsApi(createReturn(order.referenceNumber).referenceNumber).paymentMethods
              .add(paymentType, payload)
          response.mustFailWithMessage("Amount got -42, expected more than 0")
        }
      }

      "fails if the RMA is not found" in new ReturnPaymentFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload  = ReturnPaymentPayload(amount = 42)
          val response = returnsApi("TRY_HARDER").paymentMethods.add(paymentType, payload)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }

      "fails if total payment exceeds returns items subtotal" in new ReturnPaymentDefaults {
        val payload = ReturnPaymentsPayload(
            Map(PaymentMethod.CreditCard → 200, PaymentMethod.StoreCredit → 120))

        returnsApi(rma.referenceNumber).paymentMethods
          .add(payload)
          .mustFailWith400(
              ReturnPaymentExceeded(rma.referenceNumber, amount = 320, maxAmount = 300))
      }

      "fails if cc payment exceeds order cc payment minus any previous returned cc payments" in new ReturnPaymentFixture
      with OrderDefaults with ReturnReasonDefaults {
        val maxCCAmount = (0.5 * shippingMethod.price).toInt
        val scAmount    = product.price + shippingMethod.price - maxCCAmount
        override val storeCredit =
          api_newStoreCredit(customer.id,
                             CreateManualStoreCredit(amount = scAmount, reasonId = reason.id))
        override val order = createDefaultOrder(
            Map(PaymentMethod.CreditCard → None, PaymentMethod.StoreCredit → Some(scAmount)))

        val payload = ReturnShippingCostLineItemPayload(amount = maxCCAmount, reasonId = reason.id)

        // create some other return from different order
        val otherOrderRef = createDefaultOrder().referenceNumber
        val otherRmaRef   = createReturn(otherOrderRef).referenceNumber
        createReturnLineItem(payload.copy(amount = shippingMethod.price), refNum = otherRmaRef)
        createReturnPayment(Map(PaymentMethod.CreditCard → maxCCAmount), refNum = otherRmaRef)

        // create some other return for the same order
        val previousRmaRef = createReturn(order.referenceNumber).referenceNumber
        createReturnLineItem(payload.copy(amount = 25), refNum = previousRmaRef)
        createReturnPayment(Map(PaymentMethod.CreditCard → 25), refNum = previousRmaRef)

        val rma = createReturn(order.referenceNumber)
        createReturnLineItem(payload, rma.referenceNumber)

        returnsApi(rma.referenceNumber).paymentMethods
          .add(PaymentMethod.CreditCard, ReturnPaymentPayload(maxCCAmount))
          .mustFailWith400(ReturnCCPaymentExceeded(rma.referenceNumber,
                                                   amount = payload.amount,
                                                   maxAmount = maxCCAmount - 25))
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/credit-cards" - {
      "successfully delete any supported payment method" in new ReturnPaymentDefaults {
        forAll(paymentMethodTable) { paymentType ⇒
          val response = returnsApi(rma.referenceNumber).paymentMethods
            .remove(paymentType)
            .as[ReturnResponse.Root]

          response.payments mustBe 'empty
        }
      }

      "fails if the RMA is not found" in new ReturnPaymentFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val response = returnsApi("TRY_HARDER").paymentMethods.remove(paymentType)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }
    }
  }

}
