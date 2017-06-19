import phoenix.failures.GiftCardFailures.GiftCardIsInactive
import phoenix.models.shipping.ShippingMethods
import phoenix.models.{Reason, Reasons}
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.payloads.PaymentPayloads.{CreditCardPayment, GiftCardPayment}
import phoenix.payloads.UpdateShippingMethod
import phoenix.responses.{GiftCardResponse, ShippingMethodsResponse}
import phoenix.utils.seeds.Factories
import testutils._
import testutils.apis.PhoenixPublicApi
import testutils.fixtures.api._

// https://github.com/FoxComm/highlander/issues/1904
class RedeemedGcCheckoutIT
    extends IntegrationTestBase
    with PhoenixPublicApi
    with DefaultJwtAdminAuth
    with ApiFixtures
    with ApiFixtureHelpers {

  "checkout with redeemed gc must fail nicely" in {
    val customer = api_newCustomer()
    val skuCode  = ProductSku_ApiFixture(skuPrice = 7000).skuCode
    val gc = {
      val reason = Reasons.create(Reason(body = "foo", storeAdminId = defaultAdmin.id)).gimme
      giftCardsApi
        .create(GiftCardCreateByCsr(balance = 10000, reasonId = reason.id))
        .as[GiftCardResponse.Root]
    }
    val address = randomAddress()
    val cc      = api_newCreditCard(customer.id, customer.name.value, address)

    val (cartApi, cartRef) = {
      val refNum = api_newCustomerCart(customer.id).referenceNumber
      (cartsApi(refNum), refNum)
    }

    cartApi.lineItems
      .add(Seq(UpdateLineItemsPayload(sku = skuCode, quantity = 1)))
      .mustBeOk()

    cartApi.shippingAddress.create(address).mustBeOk()

    ShippingMethods.createAll(Factories.shippingMethods).gimme
    val shippingMethod = shippingMethodsApi.forCart(cartRef).as[Seq[ShippingMethodsResponse.Root]].head
    cartApi.shippingMethod.update(UpdateShippingMethod(shippingMethod.id)).mustBeOk()

    cartApi.payments.giftCard
      .add(GiftCardPayment(code = gc.code))
      .mustBeOk()
    cartApi.payments.creditCard.add(CreditCardPayment(cc.id)).mustBeOk()

    giftCardsApi(gc.code).convertToStoreCredit(customer.id).mustBeOk()

    cartApi.checkout().mustFailWith400(GiftCardIsInactive(gc.code))
  }
}
