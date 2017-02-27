package testutils.fixtures

import models.cord._
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.product.{Mvp, SimpleProductData}
import models.returns._
import models.shipping.{ShippingMethod, ShippingMethods}
import org.scalatest.prop.Tables
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads.{CreditCardPayment, GiftCardPayment, StoreCreditPayment}
import payloads.ReturnPayloads._
import payloads.UpdateShippingMethod
import responses._
import responses.cord.OrderResponse
import testutils._
import testutils.fixtures.api.ApiFixtureHelpers
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.seeds.Seeds.Factories

trait ReturnsFixtures extends TestFixtureBase with BakedFixtures with ApiFixtureHelpers {
  self: FoxSuite ⇒

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

    lazy val rma: ReturnResponse.Root = createReturn()
  }

  trait ReasonFixture extends Fixture {
    def createReturnReason(name: String)(implicit sl: SL, sf: SF): ReturnReasonsResponse.Root =
      returnsApi.reasons.add(ReturnReasonPayload(name)).as[ReturnReasonsResponse.Root]

    lazy val returnReason: ReturnReason =
      ReturnReasons.mustFindById404(createReturnReason("whatever").id).gimme
  }

  trait LineItemFixture extends ReasonFixture with Tables {
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
  }

  trait PaymentMethodFixture extends Fixture with Tables {
    val paymentMethodTable = Table("paymentMethod",
                                   PaymentMethod.CreditCard,
                                   PaymentMethod.GiftCard,
                                   PaymentMethod.StoreCredit)
  }

}
