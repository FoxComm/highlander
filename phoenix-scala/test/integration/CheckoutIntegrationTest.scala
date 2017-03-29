import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import failures.AddressFailures.NoDefaultAddressForCustomer
import failures.CreditCardFailures.NoDefaultCreditCardForCustomer
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.{NoDefaultShippingMethod, ShippingMethodNotFoundByName}
import failures.UserFailures._
import java.time.Instant
import models.account._
import models.cord.Order.RemorseHold
import models.cord._
import models.cord.lineitems._
import models.customer._
import models.inventory._
import models.location.{Address, Addresses}
import models.payment.giftcard._
import models.payment.{InStorePaymentStates, PaymentMethod}
import models.product.Mvp
import models.shipping._
import models.{Reason, Reasons}
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CartPayloads.{CheckoutCart, CreateCart}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads._
import payloads.PaymentPayloads.{CreateCreditCardFromTokenPayload, GiftCardPayment}
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixStorefrontApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtureHelpers
import utils.db._
import utils.seeds.Factories

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with ApiFixtureHelpers
    with AutomaticAuth
    with BakedFixtures {

  def doCheckout(customer: User,
                 sku: Sku,
                 address: Address,
                 shipMethod: ShippingMethod,
                 reason: Reason,
                 referenceNumber: Option[String] = None): HttpResponse = {
    val refNum = referenceNumber.getOrElse(
        cartsApi.create(CreateCart(customer.accountId.some)).as[CartResponse].referenceNumber)
    val _cartApi = cartsApi(refNum)

    _cartApi.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 2))).mustBeOk()

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

    _cartApi.checkout()
  }

  "PATCH /v1/carts/:refNum/line-items/attributes" - {
    val attributes = LineItemAttributes(
        GiftCardLineItemAttributes(senderName = "senderName",
                                   recipientName = "recipientName",
                                   recipientEmail = "example@example.com",
                                   message = "Boop".some).some).some

    "should update attributes of line-items successfully" in new Fixture {
      val refNum =
        cartsApi.create(CreateCart(customer.accountId.some)).as[CartResponse].referenceNumber
      val orderResponse =
        doCheckout(customer, sku, address, shipMethod, reason, Some(refNum)).as[OrderResponse]
      val lineItemToUpdate = orderResponse.lineItems.skus.head
      val root = cartsApi(orderResponse.referenceNumber)
        .updateCartLineItem(
            Seq(UpdateOrderLineItemsPayload(lineItemToUpdate.state,
                                            attributes,
                                            lineItemToUpdate.referenceNumbers.headOption.get)))
        .as[OrderResponse]
      val itemsToCheck = root.lineItems.skus.filter(oli ⇒
            oli.referenceNumbers.headOption.get == lineItemToUpdate.referenceNumbers.headOption.get)
      itemsToCheck.size mustBe 1
      itemsToCheck
        .forall(oli ⇒ oli.attributes.get.toString == attributes.get.toString()) mustBe true

    }
  }

  "POST v1/carts/:refNum/checkout" - {

    "allow to do one-click checkout" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()
      storefrontAddressesApi(address.id).setDefault().mustBeOk()
      storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

      val order = storefrontCartsApi
        .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(sku.code, 1))))
        .as[OrderResponse]
      order.lineItems.skus.onlyElement must have(
          'sku (sku.code),
          'quantity (1)
      )
      order.billingCreditCardInfo.value must have(
          'id (creditCard.id),
          'type (PaymentMethod.CreditCard)
      )
      order.shippingAddress.id must === (address.id)
      order.shippingMethod.id must === (shipMethod.id)
    }

    "should stash existing cart line items on one-click checkout and then bring them back" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()
      storefrontAddressesApi(address.id).setDefault().mustBeOk()
      storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

      cartsApi(storefrontCartsApi.get().as[CartResponse].referenceNumber).lineItems
        .update(List(UpdateLineItemsPayload(otherSku.code, 2)))
        .asThe[CartResponse]
        .result
        .lineItems
        .skus
        .onlyElement must have(
          'sku (otherSku.code),
          'quantity (2)
      )

      storefrontCartsApi
        .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(sku.code, 1))))
        .as[OrderResponse]
        .lineItems
        .skus
        .onlyElement must have(
          'sku (sku.code),
          'quantity (1)
      )

      storefrontCartsApi.get().as[CartResponse].lineItems.skus.onlyElement must have(
          'sku (otherSku.code),
          'quantity (2)
      )
    }

    "places order as admin" in new Fixture {
      val orderResponse = doCheckout(customer, sku, address, shipMethod, reason).as[OrderResponse]

      // Checkout:
      // Triggers cart → order transition
      Orders.findOneByRefNum(orderResponse.referenceNumber).gimme mustBe defined
      Carts.findOneByRefNum(orderResponse.referenceNumber).gimme must not be defined

      // Properly creates an order
      orderResponse.orderState must === (Order.RemorseHold)
      orderResponse.remorsePeriodEnd.value.isAfter(Instant.now) mustBe true

      // Authorizes payments
      GiftCardAdjustments.map(_.state).gimme must contain only InStorePaymentStates.Auth
    }

    "fails to do one-click checkout if no default credit card is selected for a customer" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()
      storefrontAddressesApi(address.id).setDefault().mustBeOk()
      storefrontPaymentsApi.creditCards.unsetDefault().mustBeEmpty()

      storefrontCartsApi
        .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(sku.code, 1))))
        .mustFailWith404(NoDefaultCreditCardForCustomer())
    }

    "fails to do one-click checkout if no default shipping address is selected for a customer" in new OneClickCheckoutFixture {
      shippingMethodsApi(shipMethod.id).setDefault().mustBeOk()
      storefrontAddressesApi.unsetDefault().mustBeEmpty()
      storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

      storefrontCartsApi
        .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(sku.code, 1))))
        .mustFailWith404(NoDefaultAddressForCustomer())
    }

    "fails to do one-click checkout if no default shipping method is selected for an organisation" in new OneClickCheckoutFixture {
      shippingMethodsApi.unsetDefault().mustBeEmpty()
      storefrontAddressesApi(address.id).setDefault().mustBeOk()
      storefrontPaymentsApi.creditCard(creditCard.id).setDefault().mustBeOk()

      storefrontCartsApi
        .checkout(CheckoutCart(items = List(UpdateLineItemsPayload(sku.code, 1))))
        .mustFailWith404(NoDefaultShippingMethod())
    }

    "fails if customer's credentials are empty" in new Fixture {
      val refNum =
        cartsApi.create(CreateCart(Some(customer.accountId))).as[CartResponse].referenceNumber

      Users.update(customer, customer.copy(email = None)).runTxn().void.runEmptyA.value.futureValue

      val checkout = cartsApi(refNum).checkout()
      checkout.error must === (UserMustHaveCredentials.description)
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      doCheckout(customer, sku, address, shipMethod, reason)
        .as[OrderResponse]
        .orderState must === (RemorseHold)

    }

    "errors 404 if no cart found by reference number" in {
      cartsApi("NOPE").checkout().mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }

    "fails if customer is blacklisted" in new BlacklistedFixture {
      doCheckout(customer, sku, address, shipMethod, reason).mustFailWith400(
          UserIsBlacklisted(customer.accountId))
    }
  }

  trait OneClickCheckoutFixture extends Fixture {
    val creditCard = {
      val cc = Factories.creditCard
      api_newCreditCard(customer.accountId,
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

  trait FullCartWithGcPayment
      extends Reason_Baked
      with EmptyCartWithShipAddress_Baked
      with FullCart_Raw
      with GiftCard_Raw
      with CartWithGiftCardPayment_Raw

  trait Fixture extends StoreAdmin_Seed with CustomerAddress_Baked {
    val (shipMethod, product, sku, reason) = (for {
      _ ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethodName = ShippingMethod.expressShippingNameForAdmin
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === shipMethodName)
                    .mustFindOneOr(ShippingMethodNotFoundByName(shipMethodName))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
    } yield (shipMethod, product, sku, reason)).gimme

    val otherSku = (for {
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.tail.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
    } yield sku).gimme
  }

  trait BlacklistedFixture extends StoreAdmin_Seed {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      account ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(
                    Factories.customer.copy(accountId = account.id,
                                            isBlacklisted = true,
                                            blacklistedBy = Some(storeAdmin.accountId)))
      custData ← * <~ CustomersData.create(
                    CustomerData(userId = customer.accountId,
                                 accountId = account.id,
                                 scope = Scope.current))
      address ← * <~ Addresses.create(Factories.usAddress1.copy(accountId = customer.accountId))
      _       ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === ShippingMethod.expressShippingNameForAdmin)
                    .mustFindOneOr(
                        ShippingMethodNotFoundByName(ShippingMethod.expressShippingNameForAdmin))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }
}
