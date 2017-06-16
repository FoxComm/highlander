import cats.implicits._
import phoenix.failures.OrderFailures.OnlyOneExternalPaymentIsAllowed
import phoenix.failures.ReturnFailures._
import core.failures._
import org.scalatest.prop.PropertyChecks
import faker.Lorem
import phoenix.failures.ReturnFailures._
import phoenix.failures._
import phoenix.models.Reason.Cancellation
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.payment.storecredit.StoreCredit
import phoenix.models.product.Mvp
import phoenix.models.returns.Return._
import phoenix.models.returns._
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PaymentPayloads.CreateManualStoreCredit
import phoenix.payloads.ReturnPayloads._
import phoenix.responses.ReturnResponse.Root
import phoenix.responses._
import phoenix.services.activity.ReturnTailored._
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.api.ApiFixtureHelpers
import testutils.fixtures.{BakedFixtures, ReturnsFixtures}
import core.utils.Money._

class ReturnIntegrationTest
    extends IntegrationTestBase
    with ApiFixtureHelpers
    with DefaultJwtAdminAuth
    with BakedFixtures
    with ReturnsFixtures
    with PropertyChecks {

  "Returns header" - {
    val refNotExist = "ABC-666"

    "successfully creates new Return" in new ReturnFixture with OrderDefaults {
      val rmaCreated = returnsApi
        .create(ReturnCreatePayload(cordRefNum = order.referenceNumber, returnType = Standard))
        .as[ReturnResponse.Root]
      rmaCreated.referenceNumber must === (s"${order.referenceNumber}.1")
      rmaCreated.customer.head.id must === (customer.id)
      rmaCreated.storeAdmin.head.id must === (defaultAdmin.id)

      val getRmaRoot = returnsApi(rmaCreated.referenceNumber).get().as[ReturnResponse.Root]
      getRmaRoot.referenceNumber must === (rmaCreated.referenceNumber)
      getRmaRoot.id must === (rmaCreated.id)
    }

    "should get rma" in new ReturnFixture with OrderDefaults {
      val expected = createReturn(order.referenceNumber)
      returnsApi(expected.referenceNumber).get().as[ReturnResponse.Root] must === (expected)
    }

    "fails to creare Return for not shipped order" in new ReturnFixture with OrderDefaults {
      val cordRefNum =
        createDefaultOrder(transitionStates = List(Order.FulfillmentStarted)).referenceNumber
      returnsApi
        .create(ReturnCreatePayload(cordRefNum = cordRefNum, returnType = Standard))
        .mustFailWith400(OrderMustBeShippedForReturn(cordRefNum, Order.FulfillmentStarted))
    }

    "fails to create Return with invalid order refNum provided" in {
      val payload = ReturnCreatePayload(cordRefNum = refNotExist, returnType = Standard)
      returnsApi.create(payload).mustFailWith404(NotFoundFailure404(Order, refNotExist))
    }

    "PATCH /v1/returns/:refNum" - {
      "successfully changes status of Return" in new ReturnDefaults {
        val payload = ReturnUpdateStatePayload(state = Processing, reasonId = None)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (Processing)
      }

      "successfully cancels Return with valid reason" in new ReturnDefaults {
        val payload =
          ReturnUpdateStatePayload(state = Canceled, reasonId = cancellationReason.id.some)
        returnsApi(rma.referenceNumber).update(payload).as[ReturnResponse.Root].state must === (Canceled)
      }

      "fails if return reason has wrong type" in new ReturnDefaults {
        assert(reason.reasonType != Cancellation)
        val payload = ReturnUpdateStatePayload(state = Canceled, reasonId = reason.id.some)
        returnsApi(rma.referenceNumber)
          .update(payload)
          .mustFailWith400(InvalidCancellationReasonFailure)
      }

      "fails if no reason is provided upon return cancellation" in new ReturnDefaults {
        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Canceled, reasonId = None))
          .mustFailWith400(EmptyCancellationReasonFailure)
      }

      "fails if cancellation reason is provided with different than canceled state" in new ReturnDefaults {
        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Processing, reasonId = reason.id.some))
          .mustFailWith400(NonEmptyCancellationReasonFailure)
      }

      "Cancel state should be final " in new ReturnDefaults {
        val canceled = returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Canceled, reasonId = cancellationReason.id.some))
          .as[Root]

        canceled.state must === (Canceled)
        canceled.canceledReasonId must === (cancellationReason.id.some)

        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Pending, reasonId = None))
          .mustFailWith400(StateTransitionNotAllowed(Return, "Canceled", "Pending", rma.referenceNumber))
      }

      "Returns should be fine with state transition " in new ReturnDefaults {
        returnsApi(rma.referenceNumber).get().as[ReturnResponse.Root].state must === (Pending)

        def state(s: State, reasonId: Option[Int] = None) =
          ReturnUpdateStatePayload(state = s, reasonId = reasonId)

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
          .mustFailWith400(StateTransitionNotAllowed(Return, "Complete", "Pending", rma.referenceNumber))
      }

      "gift cards and store credits should be activated on complete state" in new ReturnLineItemDefaults
      with ReturnPaymentFixture {
        val payments = createReturnPayments(Map(
                                              PaymentMethod.GiftCard    → 100,
                                              PaymentMethod.StoreCredit → 150
                                            ),
                                            refNum = rma.referenceNumber).payments
        val gcApi = giftCardsApi(payments.giftCard.value.code)
        val scApi = storeCreditsApi(payments.storeCredit.value.id)

        gcApi.get().as[GiftCardResponse.Root].state must === (GiftCard.OnHold)
        scApi.get().as[StoreCreditResponse.Root].state must === (StoreCredit.OnHold)

        completeReturn(rma.referenceNumber).payments must === (payments)
        gcApi.get().as[GiftCardResponse.Root].state must === (GiftCard.Active)
        scApi.get().as[StoreCreditResponse.Root].state must === (StoreCredit.Active)
      }

      "gift cards and store credits should be canceled on canceled state" in new ReturnLineItemDefaults
      with ReturnPaymentFixture {
        val payments = createReturnPayments(Map(
                                              PaymentMethod.GiftCard    → 100,
                                              PaymentMethod.StoreCredit → 150
                                            ),
                                            refNum = rma.referenceNumber).payments
        val gcApi = giftCardsApi(payments.giftCard.value.code)
        val scApi = storeCreditsApi(payments.storeCredit.value.id)

        gcApi.get().as[GiftCardResponse.Root].state must === (GiftCard.OnHold)
        scApi.get().as[StoreCreditResponse.Root].state must === (StoreCredit.OnHold)

        returnsApi(rma.referenceNumber)
          .update(ReturnUpdateStatePayload(state = Canceled, reasonId = cancellationReason.id.some))
          .as[ReturnResponse.Root]
          .payments must === (payments)
        gcApi.get().as[GiftCardResponse.Root].state must === (GiftCard.Canceled)
        scApi.get().as[StoreCreditResponse.Root].state must === (StoreCredit.Canceled)
      }

      "fails if RMA refNum is not found" in new ReturnDefaults {
        returnsApi(refNotExist)
          .update(ReturnUpdateStatePayload(state = Processing, reasonId = None))
          .mustFailWith404(NotFoundFailure404(Return, refNotExist))
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
      "should return list of Returns of existing customer" in new ReturnFixture with OrderDefaults {
        val expected = createReturn(order.referenceNumber)
        val root     = returnsApi.getByCustomer(customer.id).as[Seq[ReturnResponse.Root]]
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
        returnsApi.getByOrder(refNotExist).mustFailWith404(NotFoundFailure404(Order, refNotExist))
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

      "fails if refNum is not found" in new ReturnDefaults {
        returnsApi(refNotExist)
          .message(ReturnMessageToCustomerPayload(message = "Hello!"))
          .mustFailWith404(NotFoundFailure404(Return, refNotExist))
      }

      "fails if message is too long" in new ReturnDefaults {
        val payload =
          ReturnMessageToCustomerPayload(message = List.fill(messageToAccountMaxLength)("Yax").mkString)
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

  "Return line items" - {

    "POST /v1/returns/:refNum/line-items" - {
      "successfully adds shipping cost line item" in new ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = returnReason.id)
        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .as[ReturnResponse.Root]
          .lineItems
          .shippingCosts
          .value must have(
          'name (shippingMethod.adminDisplayName),
          'amount (order.totals.shipping),
          'price (shippingMethod.price)
        )
      }

      "successfully adds SKU line item" in new ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnSkuLineItemPayload(sku = product.code, quantity = 1, reasonId = returnReason.id)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .as[ReturnResponse.Root]
          .lineItems
          .skus
          .onlyElement must have(
          'sku (product.code),
          'title (product.title),
          'imagePath (product.image),
          'quantity (1),
          'price (product.price),
          'currency (product.currency)
        )
      }

      "overwrites existing shipping cost" in new ReturnLineItemFixture with ReturnDefaults
      with ReturnReasonDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = returnReason.id)
        val first = createReturnLineItem(payload, rma.referenceNumber)
        first.lineItems.shippingCosts.value.amount must === (order.totals.shipping)

        val second = createReturnLineItem(payload.copy(amount = 42), rma.referenceNumber)
        second.lineItems.shippingCosts.value.amount must === (42)
      }

      "overwrite all skus with bulk insert" in new ReturnLineItemFixture with ReturnFixture
      with ReturnReasonDefaults with OrderDefaults {
        val secondProduct = Mvp.insertProduct(ctx.id, Factories.products.tail.head).gimme
        override val order = createDefaultOrder(
          items = List(UpdateLineItemsPayload(sku = product.code, quantity = 1),
                       UpdateLineItemsPayload(sku = secondProduct.code, quantity = 1)))
        val rma = createReturn(orderRef = order.referenceNumber)

        val api = returnsApi(rma.referenceNumber).lineItems

        val payload =
          ReturnSkuLineItemPayload(sku = product.code, quantity = 1, reasonId = returnReason.id)
        api.add(payload).as[ReturnResponse.Root].lineItems.skus.onlyElement must have(
          'sku (product.code),
          'quantity (1)
        )

        createReturnLineItem(ReturnShippingCostLineItemPayload(amount = 300, reasonId = returnReason.id),
                             rma.referenceNumber)

        val payloads =
          List(ReturnSkuLineItemPayload(sku = secondProduct.code, quantity = 1, reasonId = returnReason.id))
        val response = api.addOrReplace(payloads).as[ReturnResponse.Root].lineItems
        response.shippingCosts mustBe 'defined
        response.skus.onlyElement must have(
          'sku (secondProduct.code),
          'quantity (1)
        )
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
          .mustFailWith400(NotFoundFailure400(ReturnReason, 666))
      }

      "fails if quantity for sku is invalid" in new ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnSkuLineItemPayload(sku = product.code, quantity = -42, reasonId = returnReason.id)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Quantity got -42, expected more than 0")
      }

      "fails if quantity for sku is more then maximum allowed quantity" in new ReturnLineItemFixture
      with ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnSkuLineItemPayload(sku = product.code, quantity = 1, reasonId = returnReason.id)

        // create some other return for different order
        val otherOrderRef = createDefaultOrder().referenceNumber
        val otherRmaRef   = createReturn(otherOrderRef).referenceNumber
        createReturnLineItem(payload, refNum = otherRmaRef)
        completeReturn(refNum = otherRmaRef)

        // create some other return for the same order
        val previousRmaRef = createReturn(order.referenceNumber).referenceNumber
        createReturnLineItem(payload, previousRmaRef)
        completeReturn(refNum = previousRmaRef)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(
            ReturnSkuItemQuantityExceeded(rma.referenceNumber, quantity = payload.quantity, maxQuantity = 0))
      }

      "fails if amount for shipping cost is less then 0" in new ReturnDefaults with ReturnReasonDefaults {
        val payload = ReturnShippingCostLineItemPayload(amount = -666, reasonId = returnReason.id)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWithMessage("Amount got -666, expected more than 0")
      }

      "fails if amount for shipping cost is more then maximum allowed amount" in new ReturnLineItemFixture
      with ReturnDefaults with ReturnReasonDefaults {
        val payload =
          ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = returnReason.id)

        // create some other return for different order
        val otherOrderRef = createDefaultOrder().referenceNumber
        val otherRmaRef   = createReturn(otherOrderRef).referenceNumber
        createReturnLineItem(payload.copy(amount = 100), refNum = otherRmaRef)
        completeReturn(refNum = otherRmaRef)

        // create some other return for the same order
        val previousRmaRef = createReturn(order.referenceNumber).referenceNumber
        createReturnLineItem(payload.copy(amount = 25), previousRmaRef)
        completeReturn(refNum = previousRmaRef)

        returnsApi(rma.referenceNumber).lineItems
          .add(payload)
          .mustFailWith400(
            ReturnShippingCostExceeded(rma.referenceNumber,
                                       amount = payload.amount,
                                       maxAmount = order.totals.shipping - 25))
      }
    }

    "DELETE /v1/returns/:refNum/line-items/:id" - {
      "successfully deletes shipping cost line item" in new ReturnLineItemDefaults {
        val api = returnsApi(rma.referenceNumber)

        api.lineItems.remove(shippingCostItemId).mustBeEmpty()
        api.get().as[ReturnResponse.Root].lineItems.shippingCosts mustBe 'empty
      }

      "successfully deletes SKU line item" in new ReturnLineItemDefaults {
        val api = returnsApi(rma.referenceNumber)

        api.lineItems.remove(skuItemId).mustBeEmpty()
        api.get().as[ReturnResponse.Root].lineItems.skus mustBe 'empty
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
      "succeeds for any supported payment" in new ReturnPaymentFixture with ReturnDefaults
      with ReturnReasonDefaults {
        forAll(paymentMethodTable) { paymentMethod ⇒
          val order = createDefaultOrder()
          val rma   = createReturn(orderRef = order.referenceNumber)
          val shippingCostPayload =
            ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = returnReason.id)
          createReturnLineItem(shippingCostPayload, rma.referenceNumber)

          val payload = ReturnPaymentPayload(amount = shippingCostPayload.amount)
          val response = returnsApi(rma.referenceNumber).paymentMethods
            .add(paymentMethod, payload)
            .as[ReturnResponse.Root]

          val (pm, payment) = response.payments.asMap.onlyElement
          pm must === (paymentMethod)
          payment.amount must === (payload.amount)
        }
      }

      "Make sure that only one external payment is allowed" in new ReturnPaymentDefaults {
        val api = returnsApi(rma.referenceNumber).paymentMethods

        val payload =
          ReturnPaymentsPayload(Map(PaymentMethod.ApplePay → 50, PaymentMethod.CreditCard → 100))

        api.addOrReplace(payload).mustFailWith400(OnlyOneExternalPaymentIsAllowed)

      }

      "Apple Pay charges should be taken into account" in new ReturnPaymentDefaults {
        val apRma = createReturn(
          createDefaultOrder(
            paymentMethods = Map(PaymentMethod.ApplePay → None)
          ).referenceNumber)

        createReturnLineItem(shippingCostPayload, apRma.referenceNumber)
        createReturnLineItem(skuPayload, apRma.referenceNumber)

        val api = returnsApi(apRma.referenceNumber).paymentMethods

        api
          .add(PaymentMethod.ApplePay, ReturnPaymentPayload(50))
          .as[ReturnResponse.Root]
          .payments
          .asMap
          .mapValues(_.amount) must === (Map[PaymentMethod.Type, Long](PaymentMethod.ApplePay → 50))
      }

      "bulk insert should override any existing payments, whilst single addition endpoint should append payment to existing ones" in
      new ReturnPaymentDefaults {
        val api = returnsApi(rma.referenceNumber).paymentMethods

        api
          .add(PaymentMethod.GiftCard, ReturnPaymentPayload(130))
          .as[ReturnResponse.Root]
          .payments
          .asMap
          .mapValues(_.amount) must === (Map[PaymentMethod.Type, Long](PaymentMethod.GiftCard → 130))

        val payload =
          ReturnPaymentsPayload(Map(PaymentMethod.CreditCard → 100, PaymentMethod.StoreCredit → 120))
        val response = api.addOrReplace(payload).as[ReturnResponse.Root]
        response.payments.asMap.mapValues(_.amount) must === (payload.payments)
        mustProduceActivity(ReturnPaymentsDeleted(response, List(PaymentMethod.GiftCard)))

        api
          .add(PaymentMethod.StoreCredit, ReturnPaymentPayload(50))
          .as[ReturnResponse.Root]
          .payments
          .asMap
          .mapValues(_.amount) must === (payload.payments + (PaymentMethod.StoreCredit → 50L))

        api
          .add(PaymentMethod.GiftCard, ReturnPaymentPayload(80))
          .as[ReturnResponse.Root]
          .payments
          .asMap
          .mapValues(_.amount) must === (
          payload.payments + (PaymentMethod.StoreCredit → 50) + (PaymentMethod.GiftCard → 80))
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
        forAll(paymentMethodTable) { paymentMethod ⇒
          val payload  = ReturnPaymentPayload(amount = 42)
          val response = returnsApi("TRY_HARDER").paymentMethods.add(paymentMethod, payload)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }

      "fails if total payment exceeds returns items subtotal" in new ReturnPaymentDefaults {
        val payload =
          ReturnPaymentsPayload(Map(PaymentMethod.CreditCard → 3000, PaymentMethod.StoreCredit → 1500))

        // taxes wasn't taken into account @aafa
        val total = returnsApi(rma.referenceNumber).get().as[Root].totals.total

        returnsApi(rma.referenceNumber).paymentMethods
          .addOrReplace(payload)
          .mustFailWith400(ReturnPaymentExceeded(rma.referenceNumber, amount = 4500, maxAmount = total))
      }

      "fails if cc payment exceeds order cc payment minus any previously returned cc payments" in new ReturnPaymentFixture
      with OrderDefaults with ReturnReasonDefaults {
        val maxCCAmount = shippingMethod.price.applyTaxes(0.5)
        val scAmount    = product.price + shippingMethod.price
        override val storeCredit =
          api_newStoreCredit(customer.id, CreateManualStoreCredit(amount = scAmount, reasonId = reason.id))
        override val order =
          createDefaultOrder(Map(PaymentMethod.CreditCard → None, PaymentMethod.StoreCredit → Some(scAmount)))

        def createPayload(amount: Long) =
          ReturnShippingCostLineItemPayload(amount = amount, reasonId = returnReason.id)

        val payload = createPayload(amount = maxCCAmount)

        // create some other return from different order
        val otherOrderRef = createDefaultOrder().referenceNumber
        val otherRmaRef   = createReturn(otherOrderRef).referenceNumber
        createReturnLineItem(createPayload(amount = shippingMethod.price), refNum = otherRmaRef)
        createReturnPayments(Map(PaymentMethod.CreditCard → maxCCAmount), refNum = otherRmaRef)
        completeReturn(refNum = otherRmaRef)

        // create some other return for the same order
        val previousRmaRef  = createReturn(order.referenceNumber).referenceNumber
        val previousPayment = 25L
        createReturnLineItem(createPayload(amount = previousPayment), refNum = previousRmaRef)
        createReturnPayments(Map(PaymentMethod.CreditCard → previousPayment), refNum = previousRmaRef)
        completeReturn(refNum = previousRmaRef)
        // check if ReturnShippingCostLineItemPayload was taken into account
        returnsApi(previousRmaRef).get().as[Root].totals.shipping must === (previousPayment)

        val rma = createReturn(order.referenceNumber)
        createReturnLineItem(payload, rma.referenceNumber)

        val total = returnsApi(rma.referenceNumber).get().as[Root].totals.total
        returnsApi(rma.referenceNumber).paymentMethods
          .add(PaymentMethod.CreditCard, ReturnPaymentPayload(shippingMethod.price))
          .mustFailWith400(
            ReturnPaymentExceeded(rma.referenceNumber, amount = shippingMethod.price, maxAmount = total))
      }
    }

    "DELETE /v1/returns/:ref/payment-methods/credit-cards" - {
      "successfully delete any supported payment method" in new ReturnPaymentDefaults {
        val payments = createReturnPayments(Map(
                                              PaymentMethod.GiftCard    → 100,
                                              PaymentMethod.StoreCredit → 150
                                            ),
                                            refNum = rma.referenceNumber).payments

        forAll(paymentMethodTable) { paymentMethod ⇒
          val api = returnsApi(rma.referenceNumber)

          api.paymentMethods.remove(paymentMethod).mustBeEmpty()
          api.get().as[ReturnResponse.Root].payments.asMap.get(paymentMethod) mustBe 'empty
        }
      }

      "fails if the refNum is not found" in new ReturnPaymentFixture {
        forAll(paymentMethodTable) { paymentMethod ⇒
          val response = returnsApi("TRY_HARDER").paymentMethods.remove(paymentMethod)

          response.mustFailWith404(NotFoundFailure404(Return, "TRY_HARDER"))
        }
      }
    }
  }

}
