import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import failures.AddressFailures.NoDefaultAddressForCustomer
import failures.CreditCardFailures.NoDefaultCreditCardForCustomer
import failures.{ArchiveFailures, NotFoundFailure404}
import failures.ShippingMethodFailures._
import failures.UserFailures._
import java.time.Instant
import java.time.temporal.ChronoUnit

import models.account._
import models.cord.Order.RemorseHold
import models.cord._
import models.cord.lineitems._
import models.customer._
import models.inventory._
import models.location.{Address, Addresses, Region}
import models.payment.giftcard._
import models.payment.{InStorePaymentStates, PaymentMethod}
import models.product.Mvp
import models.shipping._
import models.{Reason, Reasons}
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CartPayloads.{CheckoutCart, CreateCart}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads._
import payloads.PaymentPayloads._
import payloads.SkuPayloads.SkuPayload
import payloads.UpdateShippingMethod
import payloads.UserPayloads.ToggleUserBlacklisted
import responses.SkuResponses.SkuResponse
import responses.{AddressResponse, GiftCardResponse}
import responses.cord._
import slick.jdbc.PostgresProfile.api._
import testutils._
import testutils.apis._
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api._
import utils.aliases._
import utils.db._
import utils.seeds.Factories

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with ApiFixtureHelpers
    with DefaultJwtAdminAuth
    with BakedFixtures {

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
      import org.json4s.JsonDSL._
      import org.json4s._
      val cartApi     = prepareCheckout()
      val skuResponse = skusApi(skuCode).get().as[SkuResponse.Root]
      val activeFromJson: Json = ("t" → "date") ~ ("v" → (Instant.now
              .minus(2, ChronoUnit.DAYS))
              .toString)
      val activeToJson: Json = ("t" → "date") ~ ("v" → (Instant.now
              .minus(1, ChronoUnit.DAYS))
              .toString)
      // Deactivate this SKU.
      skusApi(skuCode)
        .update(SkuPayload(attributes = skuResponse.attributes.extract[Map[String, Json]] ++
                    Map("activeFrom" → activeFromJson, "activeTo" → activeToJson)))
        .mustBeOk()
      class Cart // FIXME: bad failures design @michalrus
      val expectedFailure = ArchiveFailures.LinkArchivedSkuFailure(
          new Cart,
          cartApi.get.asTheResult[CartResponse].referenceNumber,
          skuCode)
      cartApi
        .checkout()
        .mustFailWith400(List.fill(4 /* FIXME: why 4? o_O @michalrus */ )(expectedFailure): _*)
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

    val skuCode = new ProductSku_ApiFixture {}.skuCode

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
