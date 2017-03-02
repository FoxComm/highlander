import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import failures.LockFailures._
import failures.ReturnFailures._
import failures._
import models.Reason.Cancellation
import models.account._
import models.cord._
import models.returns.Return._
import models.returns._
import org.scalatest.prop.PropertyChecks
import payloads.ReturnPayloads._
import responses.ReturnResponse.Root
import responses._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.api.ApiFixtureHelpers
import testutils.fixtures.{BakedFixtures, ReturnsFixtures}

class ReturnIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with ApiFixtureHelpers
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
    with ReturnsFixtures
    with PropertyChecks {

  "Returns header" - {
    val orderRefNotExist = "ABC-666"

    "successfully creates new Return" in new Fixture {
      val rmaCreated = returnsApi
        .create(ReturnCreatePayload(cordRefNum = order.refNum, returnType = Standard))
        .as[ReturnResponse.Root]
      rmaCreated.referenceNumber must === (s"${order.refNum}.1")
      rmaCreated.customer.head.id must === (order.accountId)
      rmaCreated.storeAdmin.head.id must === (storeAdmin.accountId)

      val getRmaRoot = returnsApi(rmaCreated.referenceNumber).get().as[ReturnResponse.Root]
      getRmaRoot.referenceNumber must === (rmaCreated.referenceNumber)
      getRmaRoot.id must === (rmaCreated.id)
    }

    "should get rma" in new Fixture {
      val expected = createReturn()
      returnsApi(expected.referenceNumber)
        .get()
        .as[ReturnResponse.Root]
        .copy(totals = None) must === (expected)
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
        createReturn()
        createReturn()

        returnsApi.get().as[Seq[Root]].size must === (2)
      }
    }

    "GET /v1/returns/customer/:id" - {
      "should return list of Returns of existing customer" in new Fixture {
        val expected = createReturn()
        val root     = returnsApi.getByCustomer(customer.accountId).as[Seq[ReturnResponse.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (expected.referenceNumber)
      }

      "should return failure for non-existing customer" in new Fixture {
        private val accountId = 255
        returnsApi.getByCustomer(accountId).mustFailWith404(NotFoundFailure404(Account, accountId))
      }
    }

    "GET /v1/returns/order/:refNum" - {
      "should return list of Returns of existing order" in new Fixture {
        val expected = createReturn()
        val root     = returnsApi.getByOrder(order.referenceNumber).as[Seq[ReturnResponse.Root]]
        root.size must === (1)
        root.head.referenceNumber must === (expected.referenceNumber)
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

      "fails if Return not found" in new Fixture {
        private val rmaId = "99"
        val payload       = ReturnMessageToCustomerPayload(message = "Hello!")
        returnsApi(rmaId).message(payload).mustFailWith404(NotFoundFailure404(Return, rmaId))
      }

      "fails if message is too long" in new Fixture {
        val payload = ReturnMessageToCustomerPayload(
            message = List.fill(messageToAccountMaxLength)("Yax").mkString)
        returnsApi(rma.referenceNumber)
          .message(payload)
          .mustFailWith400(GeneralFailure("Message length got 3000, expected 1000 or less"))
      }
    }
  }

  "Return reasons" - {
    "add new return reason" in new ReasonFixture {
      val payload = ReturnReasonPayload(name = "Simple reason")
      returnsApi.reasons.add(payload).as[ReturnReasonsResponse.Root].name must === (payload.name)
    }

    "get list of return reasons" in new ReasonFixture {
      val expected = createReturnReason("whatever")
      returnsApi.reasons
        .list()
        .as[Seq[ReturnReasonsResponse.Root]] must contain theSameElementsAs List(expected)
    }

    "remove return reason by id" in new ReasonFixture {
      returnsApi.reasons.remove(returnReason.id)
      returnsApi.reasons.list().as[Seq[ReturnReasonsResponse.Root]] mustBe 'empty

      info("must fail if returnReason was already deleted")
      returnsApi.reasons
        .remove(returnReason.id)
        .mustFailWith404(NotFoundFailure404(ReturnReasons, returnReason.id))
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
        response.lineItems.shippingCosts.value.amount must === (order.shippingTotal)
      }

      "successfully adds SKU line item" in new LineItemFixture {
        val response =
          returnsApi(rma.referenceNumber).lineItems.add(skuPayload).as[ReturnResponse.Root]
        response.lineItems.skus.headOption.value.sku.sku must === (product.code)
      }

      "overwrites existing shipping cost" in new LineItemFixture {
        val first =
          createReturnLineItem(shippingCostPayload.copy(amount = 42), rma.referenceNumber)
        first.lineItems.shippingCosts.value.amount must === (42)

        val second =
          createReturnLineItem(shippingCostPayload.copy(amount = 25), rma.referenceNumber)
        second.lineItems.shippingCosts.value.amount must === (25)
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
                                                      amount = payload.amount,
                                                      maxAmount = order.shippingTotal))
      }

      "sets max shipping cost based on order total shipping cost minus any previous shipping cost returns for that order" in new LineItemFixture {
        // create some other return
        val otherOrderRef = createDefaultOrder().referenceNumber
        val otherRmaRef   = createReturn(orderRef = otherOrderRef).referenceNumber
        createReturnLineItem(payload = shippingCostPayload.copy(amount = 100),
                             refNum = otherRmaRef)

        val previousRmaRef = createReturn().referenceNumber
        createReturnLineItem(shippingCostPayload.copy(amount = 25), previousRmaRef)

        returnsApi(rma.referenceNumber).lineItems
          .add(shippingCostPayload)
          .mustFailWith400(ReturnShippingCostExceeded(rma.referenceNumber,
                                                      amount = shippingCostPayload.amount,
                                                      maxAmount = order.shippingTotal - 25))
      }
    }

    "DELETE /v1/returns/:refNum/line-items/:id" - {

      "successfully deletes gift card line item" in new LineItemFixture {
        pending

        val lineItemId =
          createReturnLineItem(giftCardPayload).lineItems.giftCards.headOption.value.lineItemId

        returnsApi(rma.referenceNumber).lineItems
          .remove(lineItemId)
          .as[ReturnResponse.Root]
          .lineItems
          .giftCards mustBe 'empty
      }

      "successfully deletes shipping cost line item" in new LineItemFixture {
        val lineItemId =
          createReturnLineItem(shippingCostPayload).lineItems.shippingCosts.headOption.value.lineItemId

        returnsApi(rma.referenceNumber).lineItems
          .remove(lineItemId)
          .as[ReturnResponse.Root]
          .lineItems
          .shippingCosts mustBe 'empty
      }

      "successfully deletes SKU line item" in new LineItemFixture {
        val lineItemId =
          createReturnLineItem(skuPayload).lineItems.skus.headOption.value.lineItemId

        returnsApi(rma.referenceNumber).lineItems
          .remove(lineItemId)
          .as[ReturnResponse.Root]
          .lineItems
          .skus mustBe 'empty
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

  "Return payment methods" - {
    "POST /v1/returns/:ref/payment-methods" - {
      "succeeds for any supported payment" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload = ReturnPaymentPayload(amount = 42, paymentType)
          val response = returnsApi(createReturn().referenceNumber).paymentMethods
            .add(payload)
            .as[ReturnResponse.Root]

          response.payments must have size 1
          response.payments.head.amount must === (payload.amount)
        }
      }

      "fails if the amount is less than zero" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload = ReturnPaymentPayload(amount = -42, paymentType)

          val response = returnsApi(createReturn().referenceNumber).paymentMethods.add(payload)
          response.mustFailWithMessage("Amount got -42, expected more than 0")
        }
      }

      "fails if the RMA is not found" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload  = ReturnPaymentPayload(amount = 42, paymentType)
          val response = returnsApi("TRY_HARDER").paymentMethods.add(payload)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/credit-cards" - {
      "successfully delete any supported payment method" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val response = returnsApi(createReturn().referenceNumber).paymentMethods
            .remove(paymentType)
            .as[ReturnResponse.Root]

          response.payments mustBe 'empty
        }
      }

      "fails if the RMA is not found" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val response = returnsApi("TRY_HARDER").paymentMethods.remove(paymentType)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }
    }
  }

}
