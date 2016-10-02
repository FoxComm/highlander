import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.CustomerFailures._
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import models.Reasons
import models.cord.Order.RemorseHold
import models.cord._
import models.customer.Customers
import models.inventory._
import models.location.Addresses
import models.payment.giftcard._
import models.product.Mvp
import models.shipping._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      // Create cart
      val createCart = cartsApi.create(CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber

      val _cartApi = cartsApi(refNum)

      // Add line items
      _cartApi.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      _cartApi.shippingAddress.updateFromAddress(address.id).status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod = _cartApi.shippingMethod.update(UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[CartResponse].totals.total
      // Pay
      val createGiftCard = giftCardsApi.create(GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      cartsApi(refNum).payments.giftCard.add(gcPayload).status must === (StatusCodes.OK)

      // Checkout!
      val checkout = _cartApi.checkout()
      checkout.status must === (StatusCodes.OK)

      val orderResponse = checkout.as[OrderResponse]

      // Checkout:
      // Triggers cart → order transition
      Orders.findOneByRefNum(refNum).gimme mustBe defined
      Carts.findOneByRefNum(refNum).gimme must not be defined

      // Properly creates an order
      orderResponse.orderState must === (Order.RemorseHold)
      orderResponse.remorsePeriodEnd.value.isAfter(Instant.now) mustBe true

      // Authorizes payments
      GiftCardAdjustments.map(_.state).gimme must contain only GiftCardAdjustment.Auth
    }

    "fails if customer's credentials are empty" in new Fixture {
      // Create cart
      val createCart = cartsApi.create(CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber

      // Update customer
      Customers.update(customer, customer.copy(isGuest = true, email = None)).run().futureValue

      // Checkout!
      val checkout = cartsApi(refNum).checkout()
      checkout.error must === (CustomerMustHaveCredentials.description)
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      //Create cart
      val refNum = cartsApi.create(CreateCart(Some(customer.id))).as[OrderResponse].referenceNumber

      val _cartApi = cartsApi(refNum)

      _cartApi.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)

      // Set address
      _cartApi.shippingAddress.updateFromAddress(address.id).status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod = _cartApi.shippingMethod.update(UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[OrderResponse].totals.total

      // Pay
      val createGiftCard = giftCardsApi.create(GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      cartsApi(refNum).payments.giftCard.add(gcPayload).status must === (StatusCodes.OK)

      // Checkout!
      val checkout = _cartApi.checkout()
      checkout.status must === (StatusCodes.OK)

      val order = Orders.findOneByRefNum(refNum).gimme.value
      order.state must === (RemorseHold)
    }

    "errors 404 if no cart found by reference number" in {
      val response = cartsApi("NOPE").checkout()
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
    }

    "fails if customer is blacklisted" in new BlacklistedFixture {
      val createCart = cartsApi.create(CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber

      val _cartApi = cartsApi(refNum)

      // Add line items
      _cartApi.lineItems.add(Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      _cartApi.shippingAddress.updateFromAddress(address.id).status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod = _cartApi.shippingMethod.update(UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[CartResponse].totals.total
      // Pay
      val createGiftCard = giftCardsApi.create(GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      cartsApi(refNum).payments.giftCard.add(gcPayload).status must === (StatusCodes.OK)

      // Checkout!
      val checkout = _cartApi.checkout()
      checkout.status must === (StatusCodes.BadRequest)

      checkout.error must === (CustomerIsBlacklisted(customer.id).description)
    }
  }

  trait FullCartWithGcPayment
      extends Reason_Baked
      with EmptyCartWithShipAddress_Baked
      with FullCart_Raw
      with GiftCard_Raw
      with CartWithGiftCardPayment_Raw

  trait Fixture extends CustomerAddress_Baked with StoreAdmin_Seed {
    val (shipMethod, product, sku, reason) = (for {
      _ ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethodName = ShippingMethod.expressShippingNameForAdmin
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === shipMethodName)
                    .mustFindOneOr(ShippingMethodNotFoundByName(shipMethodName))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
    } yield (shipMethod, product, sku, reason)).gimme
  }

  trait BlacklistedFixture extends StoreAdmin_Seed {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      customer ← * <~ Customers.create(
                    Factories.customer.copy(isBlacklisted = true,
                                            blacklistedBy = Some(storeAdmin.id)))
      address ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      _       ← * <~ Factories.shippingMethods.map(ShippingMethods.create)
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === ShippingMethod.expressShippingNameForAdmin)
                    .mustFindOneOr(
                        ShippingMethodNotFoundByName(ShippingMethod.expressShippingNameForAdmin))
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      reason  ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }
}
