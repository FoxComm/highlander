import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import core.failures.NotFoundFailure404
import phoenix.failures.AddressFailures.NoDefaultAddressForCustomer
import phoenix.failures.CreditCardFailures.NoDefaultCreditCardForCustomer
import phoenix.failures.ShippingMethodFailures._
import phoenix.failures.UserFailures._
import phoenix.models.Reasons
import phoenix.models.cord.Order.RemorseHold
import phoenix.models.cord._
import phoenix.models.inventory._
import phoenix.models.location.Region
import phoenix.models.payment.giftcard._
import phoenix.models.payment.{InStorePaymentStates, PaymentMethod}
import phoenix.models.shipping._
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.CartPayloads.CheckoutCart
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.payloads.LineItemPayloads._
import phoenix.payloads.PaymentPayloads._
import phoenix.payloads.SkuPayloads.SkuPayload
import phoenix.payloads.UpdateShippingMethod
import phoenix.payloads.UserPayloads.ToggleUserBlacklisted
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.responses.cord._
import phoenix.responses.{AddressResponse, GiftCardResponse}
import phoenix.utils.aliases._
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.apis._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import core.db._

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with ApiFixtureHelpers
    with DefaultJwtAdminAuth
    with BakedFixtures
    with SkuOps {

  "PATCH /v1/carts/:refNum/line-items/attributes" - {
    val attributes = randomGiftCardLineItemAttributes()

    "should update attributes of line-items successfully" in new Fixture {
      val order = doCheckout().as[OrderResponse]

      val lineItemToUpdate = order.lineItems.skus.head
      val updatedOrder = cartsApi(order.referenceNumber)
        .updateCartLineItem(
            Seq(UpdateOrderLineItemsPayload(lineItemToUpdate.state,
                                            attributes,
                                            lineItemToUpdate.referenceNumbers.head)))
        .as[OrderResponse]

      updatedOrder.lineItems.skus
        .filter(oli ⇒ oli.referenceNumbers.head == lineItemToUpdate.referenceNumbers.head)
        .onlyElement
        .attributes
        .value must === (attributes.value)

    }
  }

  "POST v1/carts/:refNum/checkout" - {

    "allow to do one-click checkout" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontAddressesApi(address.id).setDefault().mustBeOk()
        storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

        val order = storefrontCartsApi
          .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(skuCode, 1))))
          .as[OrderResponse]
        order.lineItems.skus.onlyElement must have(
            'sku (skuCode),
            'quantity (1)
        )
        order.billingCreditCardInfo.value must have(
            'id (creditCard.id),
            'type (PaymentMethod.CreditCard)
        )
        order.shippingAddress.id must === (address.id)
        order.shippingMethod.id must === (shipMethod.id)
      }
    }

    "should stash existing cart line items on one-click checkout and then bring them back" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()

      val otherSkuCode = new ProductSku_ApiFixture {}.skuCode

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontAddressesApi(address.id).setDefault().mustBeOk()
        storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

        cartsApi(storefrontCartsApi.get().as[CartResponse].referenceNumber).lineItems
          .update(List(UpdateLineItemsPayload(otherSkuCode, 2)))
          .asThe[CartResponse]
          .result
          .lineItems
          .skus
          .onlyElement must have(
            'sku (otherSkuCode),
            'quantity (2)
        )

        storefrontCartsApi
          .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(skuCode, 1))))
          .as[OrderResponse]
          .lineItems
          .skus
          .onlyElement must have(
            'sku (skuCode),
            'quantity (1)
        )

        storefrontCartsApi.get().as[CartResponse].lineItems.skus.onlyElement must have(
            'sku (otherSkuCode),
            'quantity (2)
        )
      }
    }

    "places order as admin" in new Fixture {
      val order = doCheckout().as[OrderResponse]

      // Checkout:
      // Triggers cart → order transition
      Orders.findOneByRefNum(order.referenceNumber).gimme mustBe defined
      Carts.findOneByRefNum(order.referenceNumber).gimme must not be defined

      // Properly creates an order
      order.orderState must === (Order.RemorseHold)
      order.remorsePeriodEnd.value.isAfter(Instant.now) mustBe true

      // Authorizes payments
      GiftCardAdjustments.map(_.state).gimme must contain only InStorePaymentStates.Auth
    }

    "fails to do one-click checkout if no default credit card is selected for a customer" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontAddressesApi(address.id).setDefault().mustBeOk()
        storefrontPaymentsApi.creditCards.unsetDefault().mustBeEmpty()

        storefrontCartsApi
          .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(skuCode, 1))))
          .mustFailWith404(NoDefaultCreditCardForCustomer())
      }
    }

    "fails to do one-click checkout if no default shipping address is selected for a customer" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontAddressesApi.unsetDefault().mustBeEmpty()
        storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

        storefrontCartsApi
          .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(skuCode, 1))))
          .mustFailWith404(NoDefaultAddressForCustomer())
      }
    }

    "fails to do one-click checkout if no default shipping method is selected for an organisation" in new OneClickCheckoutFixture {
      shippingMethodsApi.unsetDefault().mustBeEmpty()

      withCustomerAuth(customerLoginData, customer.id) { implicit auth ⇒
        storefrontAddressesApi(address.id).setDefault().mustBeOk()
        storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

        storefrontCartsApi
          .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(skuCode, 1))))
          .mustFailWith404(NoDefaultShippingMethod())
      }
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      doCheckout().as[OrderResponse].orderState must === (RemorseHold)

    }

    "errors 404 if no cart found by reference number" in {
      cartsApi("NOPE").checkout().mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }

    "fails if customer is blacklisted" in new BlacklistedFixture {
      doCheckout().mustFailWith400(UserIsBlacklisted(customer.id))
    }

    "fails when some SKUs in cart are inactive" in new Fixture {
      val cartApi = prepareCheckout()
      deactivateSku(skuCode)
      val expectedFailure = NotFoundFailure404(Sku, skuCode)
      cartApi.checkout().mustFailWith404(expectedFailure)
    }

    "succeeds even if the product has been archived after checkout" in new Fixture {
      val order = doCheckout().as[OrderResponse]
      productsApi(product.id).archive().mustBeOk()
      ordersApi(order.referenceNumber).get().mustBeOk()
    }

    "succeeds even if the SKU has been archived after checkout" in new Fixture {
      val order = doCheckout().as[OrderResponse]
      skusApi(skuCode).archive().mustBeOk()
      ordersApi(order.referenceNumber).get().mustBeOk()
    }

  }

  trait OneClickCheckoutFixture extends Fixture {
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
  }

  trait Fixture {
    val (shipMethod, reason) = (for {
      _ ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethodName = ShippingMethod.expressShippingNameForAdmin
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === shipMethodName)
                    .mustFindOneOr(ShippingMethodNotFoundByName(shipMethodName))
      reason ← * <~ Reasons.create(Factories.reason(defaultAdmin.id))
    } yield (shipMethod, reason)).gimme

    val (customer, customerLoginData) = api_newCustomerWithLogin()

    val address = customersApi(customer.id).addresses
      .create(randomAddress(regionId = Region.californiaId))
      .as[AddressResponse]

    private val apiFixture = new ProductSku_ApiFixture {}
    val skuCode            = apiFixture.skuCode
    val product            = apiFixture.product

    def prepareCheckout(): cartsApi = {
      val _cartApi = cartsApi(api_newCustomerCart(customer.id).referenceNumber)

      _cartApi.lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 2))).mustBeOk()

      _cartApi.shippingAddress.updateFromAddress(address.id).mustBeOk()

      val grandTotal = _cartApi.shippingMethod
        .update(UpdateShippingMethod(shipMethod.id))
        .asTheResult[CartResponse]
        .totals
        .total

      val gcCode = giftCardsApi
        .create(GiftCardCreateByCsr(grandTotal, reason.id))
        .as[GiftCardResponse.Root]
        .code

      _cartApi.payments.giftCard.add(GiftCardPayment(gcCode, grandTotal.some)).mustBeOk()

      _cartApi
    }

    def doCheckout(): HttpResponse = prepareCheckout().checkout()

  }

  trait BlacklistedFixture extends Fixture {
    customersApi(customer.id).blacklist(ToggleUserBlacklisted(blacklisted = true)).mustBeOk()
  }
}
