import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import failures.CustomerFailures._
import models.cord.Order.RemorseHold
import models.cord._
import models.customer.Customers
import models.inventory._
import models.location.Addresses
import models.objects._
import models.payment.giftcard._
import models.product.{Mvp, SimpleContext}
import models.shipping._
import models.{Reasons, StoreAdmins}
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      // Create cart
      val createCart = POST("v1/orders", CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[CartResponse].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
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
      val createCart = POST("v1/orders", CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber

      // Update customer
      Customers.update(customer, customer.copy(isGuest = true, email = None)).run().futureValue

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.error must === (CustomerMustHaveCredentials.description)
    }

    "fails if AFS is zero" in new Fixture {
      // FIXME #middlewarehouse
      pending

      //Create cart
      val refNum =
        POST("v1/orders", CreateCart(Some(customer.id))).as[OrderResponse].referenceNumber

      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)

      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[OrderResponse].totals.total

      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.OK)

      val order = Orders.findOneByRefNum(refNum).gimme.value
      order.state must === (RemorseHold)
    }

    "errors 404 if no cart found by reference number" in {
      val response = POST("v1/orders/NOPE/checkout")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
    }

    "fails if customer is blacklisted" in new BlacklistedFixture {
      val createCart = POST("v1/orders", CreateCart(Some(customer.id)))
      createCart.status must === (StatusCodes.OK)
      val refNum = createCart.as[CartResponse].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 2))).status must === (
          StatusCodes.OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (StatusCodes.OK)
      // Set shipping method
      val setShipMethod =
        PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (StatusCodes.OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[CartResponse].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (StatusCodes.OK)
      val gcCode    = createGiftCard.as[GiftCardResponse.Root].code
      val gcPayload = GiftCardPayment(gcCode, grandTotal.some)
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", gcPayload).status must === (
          StatusCodes.OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (StatusCodes.BadRequest)

      checkout.error must === (CustomerIsBlacklisted(customer.id).description)
    }
  }

  trait Fixture {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customer   ← * <~ Customers.create(Factories.customer)
      address    ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      _          ← * <~ Factories.shippingMethods.map(ShippingMethods.create(_))
      shipMethod ← * <~ ShippingMethods
                    .filter(_.adminDisplayName === ShippingMethod.expressShippingNameForAdmin)
                    .mustFindOneOr(
                        ShippingMethodNotFoundByName(ShippingMethod.expressShippingNameForAdmin))
      product ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku     ← * <~ Skus.mustFindById404(product.skuId)
      admin   ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason  ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }

  trait BlacklistedFixture {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      admin      ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(
                    Factories.customer.copy(isBlacklisted = true, blacklistedBy = Some(admin.id)))
      address    ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      product    ← * <~ Mvp.insertProduct(productCtx.id, Factories.products.head)
      sku        ← * <~ Skus.mustFindById404(product.skuId)
      reason     ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (customer, address, shipMethod, product, sku, reason)).gimme
  }
}
