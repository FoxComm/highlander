package testutils.fixtures

import cats.data.NonEmptyList
import cats.implicits._
import models.cord.Order
import models.payment.PaymentMethod
import models.product.{Mvp, SimpleProductData}
import models.returns._
import models.shipping.{ShippingMethod, ShippingMethods}
import org.scalatest.OptionValues
import org.scalatest.prop.Tables._
import payloads.AddressPayloads.CreateAddressPayload
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.UpdateOrderPayload
import payloads.PaymentPayloads._
import payloads.ReturnPayloads._
import payloads.UpdateShippingMethod
import responses._
import responses.cord.OrderResponse
import testutils._
import testutils.fixtures.api.ApiFixtureHelpers
import utils.aliases._
import utils.seeds.Factories

trait ReturnsFixtures
    extends TestFixtureBase
    with BakedFixtures
    with ApiFixtureHelpers
    with OptionValues {
  self: FoxSuite ⇒

  trait OrderDefaults extends EmptyCartWithShipAddress_Baked with Reason_Baked {
    val shippingMethod: ShippingMethod =
      ShippingMethods.create(Factories.shippingMethods.head).gimme

    val creditCard = {
      val cc = Factories.creditCard
      api_newCreditCard(customer.id,
                        CreateCreditCardFromTokenPayload(
                            token = "whatever",
                            lastFour = cc.lastFour,
                            expYear = cc.expYear,
                            expMonth = cc.expMonth,
                            brand = cc.brand,
                            holderName = cc.holderName,
                            billingAddress = CreateAddressPayload(
                                name = cc.address.name,
                                regionId = cc.address.regionId,
                                address1 = cc.address.address1,
                                address2 = cc.address.address2,
                                city = cc.address.city,
                                zip = cc.address.zip,
                                isDefault = false,
                                phoneNumber = cc.address.phoneNumber
                            ),
                            addressIsNew = true
                        ))
    }

    val giftCard = api_newGiftCard(GiftCardCreateByCsr(balance = 1000, reasonId = reason.id))

    val storeCredit =
      api_newStoreCredit(customer.id, CreateManualStoreCredit(amount = 1000, reasonId = reason.id))

    val product: SimpleProductData = Mvp.insertProduct(ctx.id, Factories.products.head).gimme

    def createOrder(lineItems: Seq[UpdateLineItemsPayload],
                    paymentMethods: Map[PaymentMethod.Type, Option[Int]])(
        implicit sl: SL,
        sf: SF): OrderResponse = {
      val api = cartsApi(api_newCustomerCart(customer.id).referenceNumber)

      api.lineItems.add(lineItems).mustBeOk()
      api.shippingAddress.updateFromAddress(address.id).mustBeOk()
      api.shippingMethod.update(UpdateShippingMethod(shippingMethod.id)).mustBeOk()
      paymentMethods.foreach {
        case (PaymentMethod.CreditCard, None) ⇒
          api.payments.creditCard.add(CreditCardPayment(creditCard.id)).mustBeOk()
        case (PaymentMethod.GiftCard, amount) if amount.exists(_ <= giftCard.availableBalance) ⇒
          api.payments.giftCard.add(GiftCardPayment(giftCard.code, amount)).mustBeOk()
        case (PaymentMethod.StoreCredit, Some(amount)) if amount <= storeCredit.availableBalance ⇒
          api.payments.storeCredit.add(StoreCreditPayment(amount)).mustBeOk()
        case other ⇒ sys.error(s"Unsupported configuration for order payment method: $other")
      }

      api.checkout().as[OrderResponse]
    }

    def createDefaultOrder(paymentMethods: Map[PaymentMethod.Type, Option[Int]] = Map(
                               PaymentMethod.CreditCard → None),
                           transitionStates: List[Order.State] =
                             List(Order.FulfillmentStarted, Order.Shipped)): OrderResponse = {
      val initial = createOrder(
          lineItems = List(UpdateLineItemsPayload(sku = product.code, quantity = 1)),
          paymentMethods = paymentMethods
      )
      val api = ordersApi(initial.referenceNumber)
      NonEmptyList
        .fromList(transitionStates)
        .map(transitionEntity(_, none[OrderResponse])(_.orderState)(state ⇒
                  api.update(UpdateOrderPayload(state = state)).as[OrderResponse]))
        .getOrElse(initial)
    }

    val order: OrderResponse = createDefaultOrder()
  }

  trait ReturnFixture {
    def createReturn(orderRef: String, returnType: Return.ReturnType = Return.Standard)(
        implicit sl: SL,
        sf: SF): ReturnResponse.Root =
      returnsApi.create(ReturnCreatePayload(orderRef, returnType)).as[ReturnResponse.Root]

    def updateReturnState(refNum: String, returnState: Return.State, reasonId: Option[Int] = None)(
        implicit sl: SL,
        sf: SF): ReturnResponse.Root =
      returnsApi(refNum)
        .update(ReturnUpdateStatePayload(returnState, reasonId))
        .as[ReturnResponse.Root]

    def completeReturn(refNum: String)(implicit sl: SL, sf: SF) = {
      val happyPath = NonEmptyList.fromListUnsafe(
          List(Return.Pending, Return.Processing, Return.Review, Return.Complete))

      transitionEntity(happyPath, returnsApi(refNum).get().as[ReturnResponse.Root].some)(_.state)(
          state ⇒ updateReturnState(refNum = refNum, returnState = state))
    }
  }

  trait ReturnDefaults extends ReturnFixture with OrderDefaults {
    val rma: ReturnResponse.Root = createReturn(order.referenceNumber)
  }

  trait ReturnReasonFixture extends ReturnFixture {
    def createReturnReason(name: String)(implicit sl: SL, sf: SF): ReturnReasonsResponse.Root =
      returnsApi.reasons.add(ReturnReasonPayload(name)).as[ReturnReasonsResponse.Root]
  }

  trait ReturnReasonDefaults extends ReturnReasonFixture {
    val returnReason: ReturnReasonsResponse.Root = createReturnReason("whatever")
  }

  trait ReturnLineItemFixture extends ReturnReasonFixture {
    def createReturnLineItem(payload: ReturnLineItemPayload,
                             refNum: String)(implicit sl: SL, sf: SF): ReturnResponse.Root =
      returnsApi(refNum).lineItems.add(payload).as[ReturnResponse.Root]
  }

  trait ReturnLineItemDefaults
      extends ReturnLineItemFixture
      with ReturnReasonDefaults
      with ReturnDefaults {

    val shippingCostPayload =
      ReturnShippingCostLineItemPayload(amount = order.totals.shipping, reasonId = reason.id)

    val skuPayload =
      ReturnSkuLineItemPayload(sku = product.code, quantity = 1, reasonId = returnReason.id)

    val shippingCostItemId =
      createReturnLineItem(shippingCostPayload, rma.referenceNumber).lineItems.shippingCosts.value.id
    val skuItemId = createReturnLineItem(skuPayload, rma.referenceNumber).lineItems.skus.head.id
  }

  trait ReturnPaymentFixture extends ReturnLineItemFixture {
    def createReturnPayments(payments: Map[PaymentMethod.Type, Int],
                             refNum: String)(implicit sl: SL, sf: SF): ReturnResponse.Root =
      returnsApi(refNum).paymentMethods
        .add(ReturnPaymentsPayload(payments))
        .as[ReturnResponse.Root]

    def createReturnPayment(payment: PaymentMethod.Type, amount: Int, refNum: String)(
        implicit sl: SL,
        sf: SF): ReturnResponse.Root =
      returnsApi(refNum).paymentMethods
        .add(payment, ReturnPaymentPayload(amount))
        .as[ReturnResponse.Root]

    val paymentMethodTable = Table("paymentMethod",
                                   PaymentMethod.CreditCard,
                                   PaymentMethod.GiftCard,
                                   PaymentMethod.StoreCredit)

    implicit class RichReturnPayments(payments: ReturnResponse.Payments) {
      def asMap: Map[PaymentMethod.Type, ReturnResponse.Payment] =
        Map.empty[PaymentMethod.Type, ReturnResponse.Payment] ++
          payments.creditCard.map(PaymentMethod.CreditCard   → _) ++
          payments.giftCard.map(PaymentMethod.GiftCard       → _) ++
          payments.storeCredit.map(PaymentMethod.StoreCredit → _)
    }
  }

  trait ReturnPaymentDefaults
      extends ReturnPaymentFixture
      with ReturnLineItemDefaults
      with ReturnDefaults

}
