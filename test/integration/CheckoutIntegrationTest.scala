import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.OK

import Extensions._
import models.customer.Customers
import models.inventory.Skus
import models.location.Addresses
import models.order.{Orders, Order}
import models.shipping.ShippingMethods
import models.{Reasons, StoreAdmins}
import payloads.{CreateOrder, GiftCardCreateByCsr, GiftCardPayment, UpdateLineItemsPayload, UpdateShippingMethod}
import responses.FullOrder.Root
import responses.{FullOrder, GiftCardResponse}
import services.CartFailures.CustomerHasNoActiveOrder
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/orders/:refNum/checkout" - {

    "places order as admin" in new Fixture {
      // Create cart
      val createCart = POST("v1/orders", CreateOrder(Some(customer.id)))
      createCart.status must === (OK)
      val refNum = createCart.as[FullOrder.Root].referenceNumber
      // Add line items
      POST(s"v1/orders/$refNum/line-items", Seq(UpdateLineItemsPayload(sku.code, 1))).status must === (OK)
      // Set address
      PATCH(s"v1/orders/$refNum/shipping-address/${address.id}").status must === (OK)
      // Set shipping method
      val setShipMethod = PATCH(s"v1/orders/$refNum/shipping-method", UpdateShippingMethod(shipMethod.id))
      setShipMethod.status must === (OK)
      val grandTotal = setShipMethod.ignoreFailuresAndGiveMe[FullOrder.Root].totals.total
      // Pay
      val createGiftCard = POST("v1/gift-cards", GiftCardCreateByCsr(grandTotal, reason.id))
      createGiftCard.status must === (OK)
      val gcCode = createGiftCard.as[GiftCardResponse.Root].code
      POST(s"v1/orders/$refNum/payment-methods/gift-cards", GiftCardPayment(gcCode, grandTotal)).status must === (OK)

      // Checkout!
      val checkout = POST(s"v1/orders/$refNum/checkout")
      checkout.status must === (OK)
      checkout.as[Root].orderState must === (Order.RemorseHold)
      Orders.findOneByRefNum(refNum).run().futureValue.value.placedAt.value
    }

    "errors 404 if no cart found by reference number" in {
      val response = POST("v1/orders/NOPE/checkout")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }
  }

  "POST v1/my/cart/checkout" - {
    "errors if no cart found for customer" in {
      val customer = Customers.create(Factories.customer).run().futureValue.rightVal

      val response = POST("v1/my/cart/checkout")
      response.status must === (StatusCodes.BadRequest)
      response.errors must contain(CustomerHasNoActiveOrder(customer.id).description)
    }
  }

  trait Fixture {
    val (customer, address, shipMethod, sku, reason) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      address    ← * <~ Addresses.create(Factories.usAddress1.copy(customerId = customer.id))
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
      sku        ← * <~ Skus.create(Factories.skus.head)
      admin      ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason     ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (customer, address, shipMethod, sku, reason)).run().futureValue.rightVal
  }
}
