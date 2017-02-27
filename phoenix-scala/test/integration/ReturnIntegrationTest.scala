import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import failures.LockFailures._
import failures.ReturnFailures._
import failures._
import models.Reason.Cancellation
import models.account._
import models.cord._
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.product.{Mvp, SimpleProductData}
import models.returns.Return._
import models.returns._
import models.shipping.{ShippingMethod, ShippingMethods}
import org.scalatest.prop.PropertyChecks
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads.{CreditCardPayment, GiftCardPayment, StoreCreditPayment}
import payloads.ReturnPayloads._
import payloads.UpdateShippingMethod
import responses.ReturnResponse.Root
import responses._
import responses.cord.OrderResponse
import services.returns.ReturnLockUpdater
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtureHelpers
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with ApiFixtureHelpers
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
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
          val payload = ReturnPaymentPayload(amount = 42)
          val response = returnsApi(rma.referenceNumber).paymentMethods
            .add(paymentType, payload)
            .as[ReturnResponse.Root]

          response.payments must have size 1
          response.payments.head.amount must === (payload.amount)
        }
      }

      "fails if the amount is less than zero" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload = ReturnPaymentPayload(amount = -42)

          val response =
            returnsApi(createReturn().referenceNumber).paymentMethods.add(paymentType, payload)
          response.mustFailWithMessage("Amount got -42, expected more than 0")
        }
      }

      "fails if the RMA is not found" in new PaymentMethodFixture {
        forAll(paymentMethodTable) { paymentType ⇒
          val payload  = ReturnPaymentPayload(amount = 42)
          val response = returnsApi("TRY_HARDER").paymentMethods.add(paymentType, payload)

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

  trait Fixture extends EmptyCartWithShipAddress_Baked with Reason_Baked {
    lazy val shippingMethod: ShippingMethod = (for {
      id     ← * <~ ShippingMethods.insertOrUpdate(Factories.shippingMethods.head)
      method ← * <~ ShippingMethods.mustFindById404(id)
    } yield method).gimme

    lazy val creditCard: CreditCard = (for {
      id ← * <~ CreditCards.insertOrUpdate(
              Factories.creditCard.copy(accountId = customer.accountId))
      cc ← * <~ CreditCards.mustFindById404(id)
    } yield cc).gimme

    lazy val giftCard = api_newGiftCard(amount = 1000, reasonId = reason.id)

    lazy val storeCredit =
      api_newStoreCredit(amount = 1000, reasonId = reason.id, customerId = customer.id)

    lazy val product: SimpleProductData = Mvp.insertProduct(ctx.id, Factories.products.head).gimme

    def createOrder(
        lineItems: Seq[UpdateLineItemsPayload],
        paymentMethods: Seq[PaymentMethod.Type])(implicit sl: SL, sf: SF): OrderResponse = {
      val api = cartsApi(api_newCustomerCart(customer.id).referenceNumber)

      api.lineItems.add(lineItems).mustBeOk()
      api.shippingAddress.updateFromAddress(address.id).mustBeOk()
      api.shippingMethod.update(UpdateShippingMethod(shippingMethod.id)).mustBeOk()
      paymentMethods.foreach {
        case PaymentMethod.CreditCard ⇒
          api.payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
        case PaymentMethod.GiftCard ⇒
          api.payments.giftCard.add(GiftCardPayment(giftCard.code)).mustBeOk()
        case PaymentMethod.StoreCredit ⇒
          api.payments.storeCredit.add(StoreCreditPayment(storeCredit.availableBalance)).mustBeOk()
      }

      api.checkout().as[OrderResponse]
    }

    def createDefaultOrder(): OrderResponse = createOrder(
        lineItems = List(UpdateLineItemsPayload(sku = product.code, quantity = 1)),
        paymentMethods = List(PaymentMethod.CreditCard)
    )

    lazy val order: Order = Orders.mustFindByRefNum(createDefaultOrder().referenceNumber).gimme

    def createReturn(
        returnType: Return.ReturnType = Return.Standard,
        orderRef: String = order.referenceNumber)(implicit sl: SL, sf: SF): ReturnResponse.Root =
      returnsApi.create(ReturnCreatePayload(orderRef, returnType)).as[ReturnResponse.Root]

    lazy val rma: Return = Returns.mustFindByRefNum(createReturn().referenceNumber).gimme
  }

  trait ReasonFixture extends Fixture {
    def createReturnReason(name: String)(implicit sl: SL, sf: SF): ReturnReasonsResponse.Root =
      returnsApi.reasons.add(ReturnReasonPayload(name)).as[ReturnReasonsResponse.Root]

    lazy val returnReason: ReturnReason =
      ReturnReasons.mustFindById404(createReturnReason("whatever").id).gimme
  }

  trait LineItemFixture extends ReasonFixture {
    rma // force return creation

    def createReturnLineItem(payload: ReturnLineItemPayload, refNum: String = rma.referenceNumber)(
        implicit sl: SL,
        sf: SF): ReturnResponse.Root =
      returnsApi(refNum).lineItems.add(payload).as[ReturnResponse.Root]

    val giftCardPayload =
      ReturnGiftCardLineItemPayload(code = giftCard.code, reasonId = returnReason.id)

    val shippingCostPayload =
      ReturnShippingCostLineItemPayload(amount = order.shippingTotal, reasonId = reason.id)

    val skuPayload = ReturnSkuLineItemPayload(sku = product.code,
                                              quantity = 1,
                                              reasonId = returnReason.id,
                                              isReturnItem = true,
                                              inventoryDisposition = ReturnLineItem.Putaway)

    val returnLineItemPayloadTable =
      Table("returnLineItemPayload", giftCardPayload, shippingCostPayload, skuPayload)
  }

  trait PaymentMethodFixture extends LineItemFixture {
    createReturnLineItem(shippingCostPayload)
    createReturnLineItem(skuPayload)

    val paymentMethodTable = Table("paymentMethod",
                                   PaymentMethod.CreditCard,
                                   PaymentMethod.GiftCard,
                                   PaymentMethod.StoreCredit)
  }
}
