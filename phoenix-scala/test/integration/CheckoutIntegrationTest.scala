import java.time.Instant

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import failures.UserFailures._
import models.account._
import models.cord.Order.RemorseHold
import models.cord._
import models.customer._
import models.inventory._
import models.location.{Address, Addresses}
import models.payment.giftcard._
import models.product.Mvp
import models.shipping._
import models.{Reason, Reasons}
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads.{UpdateLineItemsPayload, UpdateOrderLineItemsPayload}
import payloads.OrderPayloads.CreateCart
import payloads.PaymentPayloads.GiftCardPayment
import payloads.UpdateShippingMethod
import responses.GiftCardResponse
import responses.cord._
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.aliases.Json
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "PATCH /v1/orders/:refNum/order-line-items" - {
    val attributes = Some(
        parse("""{"attributes":{"giftCard":{"senderName":"senderName","recipientName":"recipientName","recipientEmail":"example@example.com"}}}"""))
    val addGiftCardPayload = Seq(UpdateLineItemsPayload("SKU-YAX", 2, attributes))
    "should update attributes of order-line-items succesfully" in new Fixture {
      val refNum =
        cartsApi.create(CreateCart(customer.accountId.some)).as[CartResponse].referenceNumber
      val orderResponse =
        doCheckout(customer, sku, address, shipMethod, reason, refNum).as[OrderResponse]
      val lineItemToUpdate = orderResponse.lineItems.skus.head
      val root = cartsApi(orderResponse.referenceNumber)
        .updateorderLineItem(
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

    def compareAttributes(a: Option[Json], b: Option[Json]): Boolean = {
      (a, b) match {
        case (Some(p), Some(a))            ⇒ p.equals(a)
        case (None, Some(a: JNull.type))   ⇒ true
        case (Some(a: JNull.type), None)   ⇒ true
        case (None, Some(JObject(fields))) ⇒ fields.isEmpty
        case (None, None)                  ⇒ true
        case _                             ⇒ false
      }
    }

    def doCheckout(customer: User,
                   sku: Sku,
                   address: Address,
                   shipMethod: ShippingMethod,
                   reason: Reason,
                   refNum: String): HttpResponse = {
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
  }

  "POST v1/orders/:refNum/checkout" - {

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
      GiftCardAdjustments.map(_.state).gimme must contain only GiftCardAdjustment.Auth
    }

    "fails if customer's credentials are empty" in new Fixture {
      val refNum =
        cartsApi.create(CreateCart(Some(customer.accountId))).as[CartResponse].referenceNumber

      Users.update(customer, customer.copy(email = None)).run().futureValue

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

    def doCheckout(customer: User,
                   sku: Sku,
                   address: Address,
                   shipMethod: ShippingMethod,
                   reason: Reason): HttpResponse = {
      val refNum =
        cartsApi.create(CreateCart(customer.accountId.some)).as[CartResponse].referenceNumber
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
  }

  trait BlacklistedFixture extends StoreAdmin_Seed {
    val (customer, address, shipMethod, product, sku, reason) = (for {
      account ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(
                    Factories.customer.copy(accountId = account.id,
                                            isBlacklisted = true,
                                            blacklistedBy = Some(storeAdmin.accountId)))
      custData ← * <~ CustomersData.create(
                    CustomerData(userId = customer.accountId, accountId = account.id))
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
