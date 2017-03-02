import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import failures.CartFailures._
import failures.LockFailures._
import failures.ShippingMethodFailures._
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.cord._
import models.cord.lineitems._
import models.location._
import models.payment.creditcard._
import models.product.Mvp
import models.rules.QueryStatement
import models.shipping._
import org.json4s.jackson.JsonMethods._
import payloads.AddressPayloads.UpdateAddressPayload
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.LineItemPayloads._
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import responses._
import responses.cord.CartResponse
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures._
import testutils.fixtures.api._
import utils.db._
import utils.seeds.Factories
import utils.seeds.ShipmentSeeds

class CartIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with ApiFixtures
    with ApiFixtureHelpers
    with BakedFixtures {

  "GET /v1/carts/:refNum" - {
    "payment state" - {

      "displays 'cart' payment state" in new Fixture {
        val fullCart = cartsApi(cartRef).get().asTheResult[CartResponse]
        fullCart.paymentState must === (CordPaymentState.Cart)
      }

      "displays 'auth' payment state" in new PaymentStateFixture {
        CreditCardCharges.findById(ccc.id).extract.map(_.state).update(CreditCardCharge.Auth).gimme

        cartsApi(cartRef).get().asTheResult[CartResponse].paymentState must === (
            CordPaymentState.Auth)
      }
    }

    "calculates taxes" - {
      "default" in new TaxesFixture(regionId = Region.californiaId - 1) {
        totals.taxes must === (0)
      }

      "configured" in new TaxesFixture(regionId = Region.californiaId) {
        // test section in configuration is configured for California and 7.5% rate
        totals.taxes must === (((totals.subTotal + totals.shipping) * 0.075).toInt)
      }
    }

    "returns correct image path" in new Fixture {
      val imgUrl = "testImgUrl"
      (for {
        product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head.copy(image = imgUrl))
        _ ← * <~ CartLineItems.create(
               CartLineItem(cordRef = cartRef, productVariantId = product.variantId))
      } yield {}).gimme

      val fullCart = cartsApi(cartRef).get().asTheResult[CartResponse]
      fullCart.lineItems.skus.size must === (1)
      fullCart.lineItems.skus.head.imagePath must === (imgUrl)
    }

    "empty payment methods having a guest customer" in new Fixture {
      val guestCustomer = customersApi
        .create(CreateCustomerPayload(email = "foo@bar.com", isGuest = Some(true)))
        .as[CustomerResponse.Root]
      customersApi(guestCustomer.id).cart().as[CartResponse].paymentMethods.size must === (0)
    }

    "calculates customer’s expenses considering in-store payments" in new Fixture
    with Reason_Baked {
      cartsApi(cartRef).lineItems.add(liPayload(1)).mustBeOk()

      val giftCardAmount    = 2500 // ¢
      val storeCreditAmount = 500  // ¢

      val giftCard = giftCardsApi
        .create(GiftCardCreateByCsr(giftCardAmount, reasonId = reason.id))
        .as[GiftCardResponse.Root]

      cartsApi(cartRef).payments.giftCard
        .add(GiftCardPayment(giftCard.code, giftCardAmount.some))
        .asTheResult[CartResponse]

      customersApi(customer.id).payments.storeCredit
        .create(CreateManualStoreCredit(amount = storeCreditAmount, reasonId = reason.id))
        .as[StoreCreditResponse.Root]

      cartsApi(cartRef).payments.storeCredit.add(StoreCreditPayment(storeCreditAmount))

      val fullCart = cartsApi(cartRef).get().asTheResult[CartResponse]

      fullCart.totals.customersExpenses must === (
          fullCart.totals.total - giftCardAmount - storeCreditAmount)
    }
  }

  "POST /v1/orders/:refNum/line-items" - {

    "should successfully update line items" in new Fixture {
      val sku = cartsApi(cartRef).lineItems
        .add(liPayload())
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .onlyElement
      sku.sku must === (productVariantCode)
      sku.variantId must === (productVariant.id)
      sku.quantity must === (2)
      // TODO: check if *variant* IDs match?
    }

    "adding a SKU with no product should return an error" in new Variant_Raw {
      val cartRef = api_newGuestCart().referenceNumber

      cartsApi(cartRef).lineItems
        .add(Seq(UpdateLineItemsPayload(simpleVariant.formId, 1)))
        .mustFailWith400(SkuWithNoProductAdded(cartRef, simpleVariant.code))
    }

    "adding a SKU that's associated through a productOption should succeed" in new Fixture {
      cartsApi(cartRef).lineItems
        .add(Seq(UpdateLineItemsPayload(productVariant.id, 1)))
        .asTheResult[CartResponse]
        .lineItems
        .skus must have size 1
    }

    "should respond with 404 if cart is not found" in new ShippingMethodFixture {
      cartsApi("NOPE").lineItems.add(liPayload()).mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }
  }

  "PATCH /v1/orders/:refNum/line-items" - {

    val giftCardAttrs1, giftCardAttrs2 = giftCardLineItemAttributes

    def addGiftCardPayload(productVariantId: Int) =
      Seq(UpdateLineItemsPayload(productVariantId, 2, giftCardAttrs1),
          UpdateLineItemsPayload(productVariantId, 1, giftCardAttrs2))

    def removeGiftCardPayload(productVariantId: Int) =
      Seq(UpdateLineItemsPayload(productVariantId, -2, giftCardAttrs1))

    "should successfully add line items" in new Fixture {
      val sku = cartsApi(cartRef).lineItems
        .update(liPayload(4))
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .onlyElement
      // TODO: check if *variant* IDs match?
      sku.sku must === (productVariantCode)
      sku.quantity must === (4)

      val sku2 = cartsApi(cartRef).lineItems
        .update(liPayload(2))
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .onlyElement
      sku2.sku must === (productVariantCode)
      sku2.quantity must === (6)
    }

    "should successfully add a gift card line item" in new Fixture {
      cartsApi(cartRef).lineItems
        .update(addGiftCardPayload(productVariant.id))
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .map(sku ⇒ (sku.sku, sku.quantity, sku.attributes)) must contain theSameElementsAs Seq(
          (productVariantCode, 1, giftCardAttrs2),
          (productVariantCode, 2, giftCardAttrs1))
    }

    "adding a SKU with no product should return an error" in new Variant_Raw {
      val cartRef = api_newGuestCart().referenceNumber

      cartsApi(cartRef).lineItems
        .update(Seq(UpdateLineItemsPayload(simpleVariant.formId, 1)))
        .mustFailWith400(SkuWithNoProductAdded(cartRef, simpleVariant.code))
    }

    "should successfully remove line items" in new Fixture {
      cartsApi(cartRef).lineItems.add(liPayload()).mustBeOk()

      val sku = cartsApi(cartRef).lineItems
        .update(liPayload(-1))
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .onlyElement
      sku.sku must === (productVariantCode)
      sku.quantity must === (1)
    }

    "should successfully remove gift card line item" in new Fixture {
      cartsApi(cartRef).lineItems.update(addGiftCardPayload(productVariant.id)).mustBeOk()

      val sku = cartsApi(cartRef).lineItems
        .update(removeGiftCardPayload(productVariant.id))
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .onlyElement

      sku.sku must === (productVariantCode)
      sku.quantity must === (1)
      sku.attributes must === (giftCardAttrs2)
    }

    "removing too many of an item should remove all of that item" in new Fixture {
      cartsApi(cartRef).lineItems.update(liPayload(1)).mustBeOk()

      cartsApi(cartRef).lineItems
        .update(liPayload(-3))
        .asTheResult[CartResponse]
        .lineItems
        .skus mustBe empty
    }

    "should respond with 404 if cart is not found" in new Fixture {
      cartsApi("NOPE").lineItems.add(liPayload()).mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }
  }

  "POST /v1/carts/:refNum/lock" - {
    "successfully locks a cart" in new Fixture {
      cartsApi(cartRef).lock().mustBeOk()
      Carts.findByRefNum(cartRef).gimme.head.isLocked must === (true)
      CartLockEvents.findByCartRef(cartRef).gimme.onlyElement.lockedBy must === (1)
    }

    "refuses to lock an already locked cart" in new Fixture {
      cartsApi(cartRef).lock().mustBeOk()
      cartsApi(cartRef).lock().mustFailWith400(LockedFailure(Cart, cartRef))
    }

    "avoids race condition" in new Fixture {
      pending // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587

      Seq(0, 1).par
        .map(_ ⇒ cartsApi(cartRef).lock())
        .map(_.status) must contain allOf (StatusCodes.OK, StatusCodes.BadRequest)

      CartLockEvents.size.gimme must === (1)
    }
  }

  "POST /v1/carts/:refNum/unlock" - {
    "unlocks cart" in new Fixture {
      cartsApi(cartRef).lock().mustBeOk()
      cartsApi(cartRef).unlock().mustBeOk()

      Carts.findByRefNum(cartRef).gimme.head.isLocked must === (false)
    }

    "refuses to unlock an already unlocked cart" in new Fixture {
      cartsApi(cartRef).unlock().mustFailWith400(NotLockedFailure(Cart, cartRef))
    }
  }

  "PATCH /v1/carts/:refNum/shipping-address/:id" - {

    "copying a shipping address from a customer's book" - {

      "removes an existing shipping address before copying new address" in new CartWithShipAddressFixture {
        val newAddress =
          Addresses.create(address.copy(name = "Little Mary", isDefaultShipping = false)).gimme

        List(address.id, newAddress.id).foreach { id ⇒
          cartsApi(cartRef).shippingAddress.updateFromAddress(id).mustBeOk()
        }

        val shippingAddressUpd = OrderShippingAddresses.findByOrderRef(cartRef).one.gimme.value
        shippingAddressUpd.name must === ("Little Mary")
      }

      "errors if the address does not exist" in new CartWithShipAddressFixture {
        cartsApi(cartRef).shippingAddress
          .updateFromAddress(99)
          .mustFailWith404(NotFoundFailure404(Address, 99))
      }
    }

    "editing a shipping address by copying from a customer's address book" - {

      "succeeds when the address exists" in new CartWithShipAddressFixture {
        val newAddress =
          Addresses.create(Address.fromPayload(randomAddress(region.id), customer.id)).gimme

        cartsApi(cartRef).shippingAddress.updateFromAddress(newAddress.id).mustBeOk()

        OrderShippingAddresses.findByOrderRef(cartRef).one.gimme.value.cordRef must === (cartRef)
      }

      "errors if the address does not exist" in new CartWithShipAddressFixture {
        cartsApi(cartRef).shippingAddress
          .updateFromAddress(99)
          .mustFailWith404(NotFoundFailure404(Address, 99))
      }

      "does not change the current shipping address if the edit fails" in new CartWithShipAddressFixture {
        cartsApi(cartRef).shippingAddress
          .updateFromAddress(101)
          .mustFailWith404(NotFoundFailure404(Address, 101))

        OrderShippingAddresses.findByOrderRef(cartRef).one.gimme.value.cordRef must === (cartRef)
      }
    }
  }

  "PATCH /v1/carts/:refNum/shipping-address" - {

    "succeeds when a subset of the fields in the address change" in new CartWithShipAddressFixture {
      cartsApi(cartRef).shippingAddress
        .update(UpdateAddressPayload(name = "New name".some, city = "Queen Anne".some))
        .mustBeOk()

      val updatedAddress = OrderShippingAddresses.findByOrderRef(cartRef).one.gimme.value
      updatedAddress.name must === ("New name")
      updatedAddress.city must === ("Queen Anne")
      updatedAddress.address1 must === (address.address1)
      updatedAddress.address2 must === (address.address2)
      updatedAddress.regionId must === (address.regionId)
      updatedAddress.zip must === (address.zip)
    }

    "does not update the address book" in new CartWithShipAddressFixture {
      cartsApi(cartRef).shippingAddress
        .update(UpdateAddressPayload(name = "Another name".some, city = "Fremont".some))
        .mustBeOk()

      val addressBook: Address = Addresses.findOneById(address.id).gimme.value
      addressBook.name must === (address.name)
      addressBook.city must === (address.city)
    }

    "full cart returns updated shipping address" in new CartWithShipAddressFixture {
      val updateResponse: CartResponse = cartsApi(cartRef).shippingAddress
        .update(UpdateAddressPayload(name = "Even newer name".some, city = "Queen Max".some))
        .asTheResult[CartResponse]
      checkCart(updateResponse)

      val getResponse: CartResponse = cartsApi(cartRef).get().asTheResult[CartResponse]
      checkCart(getResponse)

      private def checkCart(fullCart: CartResponse): Unit = {
        val addr = fullCart.shippingAddress.value
        addr.name must === ("Even newer name")
        addr.city must === ("Queen Max")
        addr.address1 must === (address.address1)
        addr.address2 must === (address.address2)
        val region = Regions.findOneById(address.regionId).gimme.value
        addr.region must === (region)
        addr.zip must === (address.zip)
      }
    }
  }

  "DELETE /v1/orders/:refNum/shipping-address" - {
    "succeeds if an address exists" in new CartWithShipAddressFixture {
      cartsApi(cartRef).get().asThe[CartResponse].result.shippingAddress mustBe defined

      val noAddressCart = cartsApi(cartRef).shippingAddress.delete().asThe[CartResponse]
      noAddressCart.result.shippingAddress must not be defined
      noAddressCart.warnings.value must contain(NoShipAddress(cartRef).description)

      cartsApi(cartRef).shippingAddress.delete().mustFailWith400(NoShipAddress(cartRef))
    }

    "fails if the cart is not found" in new CartWithShipAddressFixture {
      cartsApi("NOPE").shippingAddress.delete().mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      OrderShippingAddresses.length.result.gimme must === (1)
    }

    "fails if the order has already been placed" in new Order_Baked {
      cartsApi(order.refNum).shippingAddress
        .delete()
        .mustFailWith400(OrderAlreadyPlaced(order.refNum))

      OrderShippingAddresses.length.result.gimme must === (1)
    }
  }

  "PATCH /v1/carts/:refNum/shipping-method" - {
    "succeeds if the cart meets the shipping restrictions" in new ShippingMethodFixture {
      cartsApi(cartRef).shippingMethod
        .update(UpdateShippingMethod(lowShippingMethod.id))
        .asTheResult[CartResponse]
        .shippingMethod
        .value
        .name must === (lowShippingMethod.adminDisplayName)

      val shipMethod = OrderShippingMethods.findByOrderRef(cartRef).gimme.head
      shipMethod.cordRef must === (cartRef)
      shipMethod.shippingMethodId must === (lowShippingMethod.id)
    }

    "fails if the cart does not meet the shipping restrictions" in new ShippingMethodFixture {
      cartsApi(cartRef).shippingMethod
        .update(UpdateShippingMethod(highShippingMethod.id))
        .mustFailWith400(ShippingMethodNotApplicableToCart(highShippingMethod.id, cartRef))
    }

    "fails if the shipping method isn't found" in new ShippingMethodFixture {
      cartsApi(cartRef).shippingMethod
        .update(UpdateShippingMethod(999))
        .mustFailWith400(NotFoundFailure400(ShippingMethod, 999))
    }

    "fails if the shipping method isn't active" in new ShippingMethodFixture {
      cartsApi(cartRef).shippingMethod
        .update(UpdateShippingMethod(inactiveShippingMethod.id))
        .mustFailWith400(ShippingMethodIsNotActive(inactiveShippingMethod.id))
    }
  }

  trait Fixture extends StoreAdmin_Seed with ProductVariant_ApiFixture {
    val customer = api_newCustomer()
    val cartRef  = api_newCustomerCart(customer.id).referenceNumber

    def liPayload(quantity: Int = 2): Seq[UpdateLineItemsPayload] =
      Seq(UpdateLineItemsPayload(productVariant.id, quantity))
  }

  trait CartWithShipAddressFixture extends Fixture {
    val region = Regions.result.headOption.gimme.value
    val address =
      Addresses.create(Address.fromPayload(randomAddress(region.id), customer.id)).gimme
    cartsApi(cartRef).shippingAddress.updateFromAddress(address.id).mustBeOk()
  }

  trait ShippingMethodFixture extends Fixture {

    val lowConditions: QueryStatement = parse(
        """
              | {
              |   "comparison": "and",
              |   "conditions": [{
              |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
              |   }]
              | }
            """.stripMargin).extract[QueryStatement]

    val highConditions: QueryStatement = parse(
        """
              | {
              |   "comparison": "and",
              |   "conditions": [{
              |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 250
              |   }]
              | }
            """.stripMargin).extract[QueryStatement]

    val lowSm: ShippingMethod = Factories.shippingMethods.head
      .copy(adminDisplayName = "Low", conditions = lowConditions.some, code = "HIGH")
    val highSm: ShippingMethod = Factories.shippingMethods.head
      .copy(adminDisplayName = "High", conditions = highConditions.some, code = "LOW")

    val (lowShippingMethod, inactiveShippingMethod, highShippingMethod) = {
      for {
        product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head.copy(price = 100))
        _ ← * <~ CartLineItems.create(
               CartLineItem(cordRef = cartRef, productVariantId = product.variantId))
        _ ← * <~ CartLineItems.create(
               CartLineItem(cordRef = cartRef, productVariantId = product.variantId))

        lowShippingMethod ← * <~ ShippingMethods.create(lowSm)
        inactiveShippingMethod ← * <~ ShippingMethods.create(
                                    lowShippingMethod.copy(isActive = false, code = "INACTIVE"))
        highShippingMethod ← * <~ ShippingMethods.create(highSm)

        cart ← * <~ Carts.mustFindByRefNum(cartRef)
        _    ← * <~ CartTotaler.saveTotals(cart)
      } yield (lowShippingMethod, inactiveShippingMethod, highShippingMethod)
    }.gimme
  }

  trait OrderShippingMethodFixture extends ShippingMethodFixture {
    val shipment = (for {
      orderShipMethod ← * <~ OrderShippingMethods.create(
                           OrderShippingMethod.build(cordRef = cartRef,
                                                     method = highShippingMethod))
      shipment ← * <~ Shipments.create(
                    Shipment(cordRef = cartRef, orderShippingMethodId = Some(orderShipMethod.id)))
    } yield shipment).gimme
  }

  trait PaymentStateFixture extends Fixture {
    val (cc, op, ccc) = (for {
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.id))
      op ← * <~ OrderPayments.create(
              Factories.orderPayment.copy(cordRef = cartRef, paymentMethodId = cc.id))
      ccc ← * <~ CreditCardCharges.create(
               Factories.creditCardCharge.copy(creditCardId = cc.id, orderPaymentId = op.id))
    } yield (cc, op, ccc)).gimme
  }

  class TaxesFixture(regionId: Int) extends ShipmentSeeds with Fixture {
    // Shipping method
    val shipMethodId = ShippingMethods.create(shippingMethods(2)).gimme.id

    cartsApi(cartRef).lineItems.add(liPayload(1)).mustBeOk()

    cartsApi(cartRef).shippingAddress.create(randomAddress(regionId)).mustBeOk()

    val totals = cartsApi(cartRef).shippingMethod
      .update(UpdateShippingMethod(shipMethodId))
      .asTheResult[CartResponse]
      .totals
  }
}
