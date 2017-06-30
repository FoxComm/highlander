package testutils.fixtures

import cats.data.NonEmptyList
import cats.implicits._
import org.scalatest.OptionValues
import org.scalatest.prop.Tables._
import phoenix.models.cord.Order
import phoenix.models.payment.PaymentMethod
import phoenix.models.product.{Mvp, SimpleProductData}
import phoenix.models.returns._
import phoenix.models.shipping.{ShippingMethod, ShippingMethods}
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.OrderPayloads.UpdateOrderPayload
import phoenix.payloads.PaymentPayloads._
import phoenix.payloads.ReturnPayloads._
import phoenix.payloads.UpdateShippingMethod
import phoenix.responses._
import phoenix.responses.cord.OrderResponse
import phoenix.utils.aliases._
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.api.{randomAddress, ApiFixtureHelpers}

trait ReturnsFixtures
    extends TestFixtureBase
    with BakedFixtures
    with ApiFixtureHelpers
    with DefaultJwtAdminAuth
    with OptionValues { self: FoxSuite ⇒

  trait OrderDefaults extends Reason_Baked {
    val shippingMethod: ShippingMethod =
      ShippingMethods.create(Factories.shippingMethods.last.copy(price = 300)).gimme

    val customer       = api_newCustomer()
    val addressPayload = randomAddress()
    val address        = customersApi(customer.id).addresses.create(addressPayload).as[AddressResponse]

    val creditCard = api_newCreditCard(customer.id, customer.name.value, addressPayload)

    val applePayPayment = CreateApplePayPayment(stripeToken = "tok_1A9YBQJVm1XvTUrO3V8caBvF")

    val giftCard = api_newGiftCard(GiftCardCreateByCsr(balance = 1000, reasonId = reason.id))

    val storeCredit =
      api_newStoreCredit(customer.id, CreateManualStoreCredit(amount = 1000, reasonId = reason.id))

    val product: SimpleProductData = Mvp.insertProduct(ctx.id, Factories.products.head).gimme

    def createOrder(
        lineItems: Seq[UpdateLineItemsPayload],
        paymentMethods: Map[PaymentMethod.Type, Option[Long]])(implicit sl: SL, sf: SF): OrderResponse = {
      val api = cartsApi(api_newCustomerCart(customer.id).referenceNumber)

      api.lineItems.add(lineItems)(defaultAdminAuth).mustBeOk()
      api.shippingAddress.updateFromAddress(address.id)(defaultAdminAuth).mustBeOk()
      api.shippingMethod
        .update(UpdateShippingMethod(shippingMethod.id))(defaultAdminAuth)
        .mustBeOk()
      paymentMethods.foreach {
        case (PaymentMethod.CreditCard, None) ⇒
          api.payments.creditCard
            .add(CreditCardPayment(creditCard.id))(defaultAdminAuth)
            .mustBeOk()
        case (PaymentMethod.ApplePay, None) ⇒
          api.payments.applePay.add(applePayPayment)(defaultAdminAuth).mustBeOk()
        case (PaymentMethod.GiftCard, amount) if amount.exists(_ <= giftCard.availableBalance) ⇒
          api.payments.giftCard
            .add(GiftCardPayment(giftCard.code, amount))(defaultAdminAuth)
            .mustBeOk()
        case (PaymentMethod.StoreCredit, Some(amount)) if amount <= storeCredit.availableBalance ⇒
          api.payments.storeCredit.add(StoreCreditPayment(amount))(defaultAdminAuth).mustBeOk()
        case other ⇒ sys.error(s"Unsupported configuration for order payment method: $other")
      }

      api.checkout()(defaultAdminAuth).as[OrderResponse]
    }

    def createDefaultOrder(
        paymentMethods: Map[PaymentMethod.Type, Option[Long]] = Map(PaymentMethod.CreditCard → None),
        items: List[UpdateLineItemsPayload] = List(UpdateLineItemsPayload(product.code, 1)),
        transitionStates: List[Order.State] = List(Order.FulfillmentStarted, Order.Shipped))
      : OrderResponse = {
      val initial = createOrder(
        lineItems = items,
        paymentMethods = paymentMethods
      )
      val api = ordersApi(initial.referenceNumber)
      NonEmptyList
        .fromList(transitionStates)
        .map(
          transitionEntity(_, none[OrderResponse])(_.orderState)(
            state ⇒
              api
                .update(UpdateOrderPayload(state = state))(defaultAdminAuth)
                .as[OrderResponse]))
        .getOrElse(initial)
    }

    val order: OrderResponse = createDefaultOrder()
  }

  trait ReturnFixture {
    def createReturn(orderRef: String, returnType: Return.ReturnType = Return.Standard)(
        implicit sl: SL,
        sf: SF): ReturnResponse =
      returnsApi
        .create(ReturnCreatePayload(orderRef, returnType))(defaultAdminAuth)
        .as[ReturnResponse]

    def updateReturnState(refNum: String, returnState: Return.State, reasonId: Option[Int] = None)(
        implicit sl: SL,
        sf: SF): ReturnResponse =
      returnsApi(refNum)
        .update(ReturnUpdateStatePayload(returnState, reasonId))(defaultAdminAuth)
        .as[ReturnResponse]

    def completeReturn(refNum: String)(implicit sl: SL, sf: SF): ReturnResponse = {
      val happyPath =
        NonEmptyList.fromListUnsafe(List(Return.Pending, Return.Processing, Return.Review, Return.Complete))

      transitionEntity(happyPath, returnsApi(refNum).get()(defaultAdminAuth).as[ReturnResponse].some)(
        _.state)(state ⇒ updateReturnState(refNum = refNum, returnState = state))
    }
  }

  trait ReturnDefaults extends ReturnFixture with OrderDefaults {
    val rma: ReturnResponse = createReturn(order.referenceNumber)
  }

  trait ReturnReasonFixture extends ReturnFixture {
    def createReturnReason(name: String)(implicit sl: SL, sf: SF): ReturnReasonsResponse =
      returnsApi.reasons
        .add(ReturnReasonPayload(name))(defaultAdminAuth)
        .as[ReturnReasonsResponse]
  }

  trait ReturnReasonDefaults extends ReturnReasonFixture {
    val returnReason: ReturnReasonsResponse = createReturnReason("whatever")
  }

  trait ReturnLineItemFixture extends ReturnReasonFixture {
    def createReturnLineItem(payload: ReturnLineItemPayload, refNum: String)(implicit sl: SL,
                                                                             sf: SF): ReturnResponse =
      returnsApi(refNum).lineItems.add(payload)(defaultAdminAuth).as[ReturnResponse]

    def createReturnSkuLineItems(payloads: List[ReturnSkuLineItemPayload],
                                 refNum: String)(implicit sl: SL, sf: SF): ReturnResponse =
      returnsApi(refNum).lineItems.addOrReplace(payloads)(defaultAdminAuth).as[ReturnResponse]
  }

  trait ReturnLineItemDefaults extends ReturnLineItemFixture with ReturnReasonDefaults with ReturnDefaults {

    val shippingCostPayload =
      ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = returnReason.id)

    val skuPayload =
      ReturnSkuLineItemPayload(sku = product.code, quantity = 1, reasonId = returnReason.id)

    val shippingCostItemId =
      createReturnLineItem(shippingCostPayload, rma.referenceNumber).lineItems.shippingCosts.value.id
    val skuItemId = createReturnLineItem(skuPayload, rma.referenceNumber).lineItems.skus.head.id
  }

  trait ReturnPaymentFixture extends ReturnLineItemFixture {
    def createReturnPayments(payments: Map[PaymentMethod.Type, Long],
                             refNum: String)(implicit sl: SL, sf: SF): ReturnResponse =
      returnsApi(refNum).paymentMethods
        .addOrReplace(ReturnPaymentsPayload(payments))(defaultAdminAuth)
        .as[ReturnResponse]

    def createReturnPayment(payment: PaymentMethod.Type, amount: Long, refNum: String)(
        implicit sl: SL,
        sf: SF): ReturnResponse =
      returnsApi(refNum).paymentMethods
        .add(payment, ReturnPaymentPayload(amount))(defaultAdminAuth)
        .as[ReturnResponse]

    val paymentMethodTable =
      Table("paymentMethod", PaymentMethod.CreditCard, PaymentMethod.GiftCard, PaymentMethod.StoreCredit)

    implicit class RichReturnPayments(payments: ReturnResponse.Payments) {
      def asMap: Map[PaymentMethod.Type, ReturnResponse.Payment] =
        Map.empty[PaymentMethod.Type, ReturnResponse.Payment] ++
          payments.creditCard.map(PaymentMethod.CreditCard   → _) ++
          payments.applePay.map(PaymentMethod.ApplePay       → _) ++
          payments.giftCard.map(PaymentMethod.GiftCard       → _) ++
          payments.storeCredit.map(PaymentMethod.StoreCredit → _)
    }
  }

  trait ReturnPaymentDefaults extends ReturnPaymentFixture with ReturnLineItemDefaults with ReturnDefaults

}
