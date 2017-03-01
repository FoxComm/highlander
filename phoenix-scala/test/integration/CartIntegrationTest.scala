import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import failures.CartFailures._
import failures.LockFailures._
import failures.ShippingMethodFailures._
import failures.{NotFoundFailure400, NotFoundFailure404}
import faker.Lorem
import models.cord._
import models.cord.lineitems._
import models.location._
import models.payment.creditcard._
import models.product.Mvp
import models.rules.QueryStatement
import models.shipping._
import org.json4s.jackson.JsonMethods._
import payloads.AddressPayloads.{CreateAddressPayload, UpdateAddressPayload}
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LineItemPayloads._
import payloads.CartPayloads.CreateCart
import payloads.UpdateShippingMethod
import responses.cord.CartResponse
import responses.cord.base.CordResponseLineItem
import responses._
import models.cord.CordPaymentState
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.PaymentPayloads._
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures
import utils.db._
import utils.seeds.Factories
import utils.seeds.ShipmentSeeds

class CartIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with ApiFixtures
    with BakedFixtures {

  "GET /v1/carts/:refNum" - {
    "payment state" - {

      "displays 'cart' payment state" in new Fixture {
        val fullCart = cartsApi(cart.refNum).get().asTheResult[CartResponse]
        fullCart.paymentState must === (CordPaymentState.Cart)
      }

      "displays 'auth' payment state" in new PaymentStateFixture {
        CreditCardCharges.findById(ccc.id).extract.map(_.state).update(CreditCardCharge.Auth).gimme

        val fullCart = cartsApi(cart.refNum).get().asTheResult[CartResponse]
        fullCart.paymentState must === (CordPaymentState.Auth)
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
        _       ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))
      } yield {}).gimme

      val fullCart = cartsApi(cart.refNum).get().asTheResult[CartResponse]
      fullCart.lineItems.skus.size must === (1)
      fullCart.lineItems.skus.head.imagePath must === (imgUrl)
    }

    "empty payment methods having a guest customer" in new Fixture {
      val guestCustomer = customersApi
        .create(CreateCustomerPayload(email = "foo@bar.com", isGuest = Some(true)))
        .as[CustomerResponse.Root]
      val fullCart = customersApi(guestCustomer.id).cart().as[CartResponse]
      fullCart.paymentMethods.size must === (0)
    }

    "calculates customer’s expenses considering in-store payments" in new StoreAdmin_Seed
    with Customer_Seed with ProductSku_ApiFixture with Reason_Baked {
      val refNum =
        cartsApi.create(CreateCart(customerId = customer.id.some)).as[CartResponse].referenceNumber

      cartsApi(refNum).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1))).mustBeOk()

      val giftCardAmount    = 2500 // ¢
      val storeCreditAmount = 500  // ¢

      val giftCard = giftCardsApi
        .create(GiftCardCreateByCsr(giftCardAmount, reasonId = reason.id))
        .as[GiftCardResponse.Root]

      cartsApi(refNum).payments.giftCard
        .add(GiftCardPayment(giftCard.code, giftCardAmount.some))
        .asTheResult[CartResponse]

      customersApi(customer.id).payments.storeCredit
        .create(CreateManualStoreCredit(amount = storeCreditAmount, reasonId = reason.id))
        .as[StoreCreditResponse.Root]

      cartsApi(refNum).payments.storeCredit.add(StoreCreditPayment(storeCreditAmount))

      val fullCart = cartsApi(refNum).get().asTheResult[CartResponse]

      fullCart.totals.customersExpenses must === (
          fullCart.totals.total - giftCardAmount - storeCreditAmount)
    }
  }

  "POST /v1/carts/:refNum/line-items" - {
    val payload = Seq(UpdateLineItemsPayload("SKU-YAX", 2))

    "should successfully update line items" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val root = cartsApi(cart.refNum).lineItems.add(payload).asTheResult[CartResponse]
      val skus = root.lineItems.skus
      skus must have size 1
      skus.map(_.sku).toSet must === (Set("SKU-YAX"))
      skus.map(_.quantity).toSet must === (Set(2))
    }

    "adding a SKU with no product should return an error" in new OrderShippingMethodFixture
    with Sku_Raw with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val payload = Seq(UpdateLineItemsPayload(simpleSku.code, 1))
      cartsApi(cart.refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(simpleSku.code, 1)))
        .mustFailWith400(SkuWithNoProductAdded(cart.refNum, simpleSku.code))
    }

    "adding a SKU that's associated through a variant should succeed" in new ProductAndVariants_Baked
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val (_, _, skus) = productWithVariants
      val code         = skus.head.code

      val testPayload = Seq(UpdateLineItemsPayload(code, 1))
      val root        = cartsApi(cart.refNum).lineItems.add(testPayload).asTheResult[CartResponse]
      val liSkus      = root.lineItems.skus
      liSkus must have size 1
    }

    "should respond with 404 if cart is not found" in {
      cartsApi("NOPE").lineItems.add(payload).mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }
  }

  "PATCH /v1/carts/:refNum/line-items" - {
    val addPayload = Seq(UpdateLineItemsPayload("SKU-YAX", 2))

    val attributes = LineItemAttributes(
        GiftCardLineItemAttributes(senderName = "senderName",
                                   recipientName = "recipientName",
                                   recipientEmail = "example@example.com",
                                   message = "message".some).some).some

    val attributes2 = LineItemAttributes(
        GiftCardLineItemAttributes(senderName = "senderName2",
                                   recipientName = "recipientName2",
                                   recipientEmail = "example2@example.com",
                                   message = "message2".some).some).some

    def addGiftCardPayload(sku: String) =
      Seq(UpdateLineItemsPayload(sku, 2, attributes), UpdateLineItemsPayload(sku, 1, attributes2))

    def removeGiftCardPayload(sku: String) = Seq(UpdateLineItemsPayload(sku, -2, attributes))

    "should successfully add line items" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val root = cartsApi(cart.refNum).lineItems.update(addPayload).asTheResult[CartResponse]
      val skus = root.lineItems.skus
      skus must have size 1
      skus.map(_.sku).headOption.value must === ("SKU-YAX")
      skus.map(_.quantity).headOption.value must === (4)

      val root2 = cartsApi(cart.refNum).lineItems.update(addPayload).asTheResult[CartResponse]
      val skus2 = root2.lineItems.skus
      skus2 must have size 1
      skus2.map(_.sku).headOption.value must === ("SKU-YAX")
      skus2.map(_.quantity).headOption.value must === (6)
    }

    "should successfully add a gift card line item" in new Customer_Seed
    with ProductSku_ApiFixture {
      val refNum =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      cartsApi(refNum).lineItems
        .update(addGiftCardPayload(skuCode))
        .asTheResult[CartResponse]
        .lineItems
        .skus
        .map(sku ⇒ (sku.sku, sku.quantity, sku.attributes)) must contain theSameElementsAs Seq(
          (skuCode, 1, attributes2),
          (skuCode, 2, attributes))
    }

    "adding a SKU with no product should return an error" in new OrderShippingMethodFixture
    with Sku_Raw with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      cartsApi(cart.refNum).lineItems
        .update(Seq(UpdateLineItemsPayload(simpleSku.code, 1)))
        .mustFailWith400(SkuWithNoProductAdded(cart.refNum, simpleSku.code))
    }

    "should successfully remove line items" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val subtractPayload = Seq(UpdateLineItemsPayload("SKU-YAX", -1))
      val root            = cartsApi(cart.refNum).lineItems.update(subtractPayload).asTheResult[CartResponse]
      val skus            = root.lineItems.skus
      skus must have size 1
      skus.map(_.sku).headOption.value must === ("SKU-YAX")
      skus.map(_.quantity).headOption.value must === (1)
    }

    "should successfully remove gift card line item" in new Customer_Seed
    with ProductSku_ApiFixture {
      val refNum =
        cartsApi.create(CreateCart(email = customer.email)).as[CartResponse].referenceNumber

      val regSkus = cartsApi(refNum).lineItems.update(addGiftCardPayload(skuCode)).mustBeOk()

      val skus = cartsApi(refNum).lineItems
          .update(removeGiftCardPayload(skuCode))
          .asTheResult[CartResponse]
          .lineItems
          .skus
          .map(sku ⇒ (sku.sku, sku.quantity, sku.attributes)) must === (
            Seq((skuCode, 1, attributes2)))
    }

    "removing too many of an item should remove all of that item" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val subtractPayload = Seq(UpdateLineItemsPayload("SKU-YAX", -3))
      cartsApi(cart.refNum).lineItems
        .update(subtractPayload)
        .asTheResult[CartResponse]
        .lineItems
        .skus mustBe empty
    }

    "should respond with 404 if cart is not found" in {
      cartsApi("NOPE").lineItems.add(addPayload).mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }

    "should add line items if productId and skuId are different" in new OrderShippingMethodFixture
    with ProductAndSkus_Baked {
      val addPayload = Seq(UpdateLineItemsPayload("TEST", 1))
      val skus: Seq[CordResponseLineItem] = cartsApi(cart.refNum).lineItems
        .update(Seq(UpdateLineItemsPayload("TEST", 1)))
        .asTheResult[CartResponse]
        .lineItems
        .skus
      skus must have size 2
      skus.map(_.sku) must contain theSameElementsAs Seq("SKU-YAX", "TEST")
      skus.map(_.quantity) must contain theSameElementsAs Seq(1, 2)
    }
  }

  "POST /v1/carts/:refNum/lock" - {
    "successfully locks a cart" in new Fixture {
      cartsApi(cart.refNum).lock().mustBeOk()

      Carts.findByRefNum(cart.refNum).gimme.head.isLocked must === (true)

      val locks: Seq[CartLockEvent] = CartLockEvents.findByCartRef(cart.refNum).gimme
      locks must have size 1
      locks.head.lockedBy must === (1)
    }

    "refuses to lock an already locked cart" in new Fixture {
      Carts.update(cart, cart.copy(isLocked = true)).gimme

      cartsApi(cart.refNum).lock().mustFailWith400(LockedFailure(Cart, cart.refNum))
    }

    "avoids race condition" in new Fixture {
      pending // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587

      Seq(0, 1).par
        .map(_ ⇒ cartsApi(cart.refNum).lock())
        .map(_.status) must contain allOf (StatusCodes.OK, StatusCodes.BadRequest)

      CartLockEvents.size.gimme must === (1)
    }
  }

  "POST /v1/carts/:refNum/unlock" - {
    "unlocks cart" in new Fixture {
      cartsApi(cart.refNum).lock().mustBeOk()
      cartsApi(cart.refNum).unlock().mustBeOk()

      Carts.findByRefNum(cart.refNum).gimme.head.isLocked must === (false)
    }

    "refuses to unlock an already unlocked cart" in new Fixture {
      cartsApi(cart.refNum).unlock().mustFailWith400(NotLockedFailure(Cart, cart.refNum))
    }
  }

  "PATCH /v1/carts/:refNum/shipping-address/:id" - {

    "copying a shipping address from a customer's book" - {

      "succeeds if the address exists in their book" in new EmptyCustomerCart_Baked
      with CustomerAddress_Raw {
        cartsApi(cart.refNum).shippingAddress.updateFromAddress(address.id).mustBeOk()

        val shippingAddressUpd = OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value
        shippingAddressUpd.cordRef must === (cart.refNum)
      }

      "removes an existing shipping address before copying new address" in new EmptyCartWithShipAddress_Baked {
        val newAddress =
          Addresses.create(address.copy(name = "Little Mary", isDefaultShipping = false)).gimme

        List(address.id, newAddress.id).foreach { id ⇒
          cartsApi(cart.refNum).shippingAddress.updateFromAddress(id).mustBeOk()
        }

        val shippingAddressUpd = OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value
        shippingAddressUpd.name must === ("Little Mary")
      }

      "errors if the address does not exist" in new EmptyCartWithShipAddress_Baked {
        cartsApi(cart.refNum).shippingAddress
          .updateFromAddress(99)
          .mustFailWith404(NotFoundFailure404(Address, 99))
      }
    }

    "editing a shipping address by copying from a customer's address book" - {

      "succeeds when the address exists" in new EmptyCartWithShipAddress_Baked {
        val newAddress = Addresses
          .create(
              Factories.address.copy(accountId = customer.accountId,
                                     isDefaultShipping = false,
                                     name = "Paul P",
                                     address1 = "29918 Kenloch Dr",
                                     city = "Farmington Hills",
                                     regionId = 4177))
          .gimme

        cartsApi(cart.refNum).shippingAddress.updateFromAddress(newAddress.id).mustBeOk()

        val shippingAddressUpd = OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value
        shippingAddressUpd.cordRef must === (cart.refNum)
      }

      "errors if the address does not exist" in new EmptyCartWithShipAddress_Baked {
        cartsApi(cart.refNum).shippingAddress
          .updateFromAddress(99)
          .mustFailWith404(NotFoundFailure404(Address, 99))
      }

      "does not change the current shipping address if the edit fails" in new EmptyCartWithShipAddress_Baked {
        cartsApi(cart.refNum).shippingAddress
          .updateFromAddress(101)
          .mustFailWith404(NotFoundFailure404(Address, 101))

        OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value.cordRef must === (
            cart.refNum)
      }
    }
  }

  "PATCH /v1/carts/:refNum/shipping-address" - {

    "succeeds when a subset of the fields in the address change" in new EmptyCartWithShipAddress_Baked {
      cartsApi(cart.refNum).shippingAddress
        .update(UpdateAddressPayload(name = "New name".some, city = "Queen Anne".some))
        .mustBeOk()

      val updatedAddress: OrderShippingAddress =
        OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value
      updatedAddress.name must === ("New name")
      updatedAddress.city must === ("Queen Anne")
      updatedAddress.address1 must === (address.address1)
      updatedAddress.address2 must === (address.address2)
      updatedAddress.regionId must === (address.regionId)
      updatedAddress.zip must === (address.zip)
    }

    "does not update the address book" in new EmptyCartWithShipAddress_Baked {
      cartsApi(cart.refNum).shippingAddress
        .update(UpdateAddressPayload(name = "Another name".some, city = "Fremont".some))
        .mustBeOk()

      val addressBook: Address = Addresses.findOneById(address.id).gimme.value
      addressBook.name must === (address.name)
      addressBook.city must === (address.city)
    }

    "full cart returns updated shipping address" in new EmptyCartWithShipAddress_Baked {
      val updateResponse: CartResponse = cartsApi(cart.refNum).shippingAddress
        .update(UpdateAddressPayload(name = "Even newer name".some, city = "Queen Max".some))
        .asTheResult[CartResponse]
      checkCart(updateResponse)

      val getResponse: CartResponse = cartsApi(cart.refNum).get().asTheResult[CartResponse]
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

  "DELETE /v1/carts/:refNum/shipping-address" - {
    "succeeds if an address exists" in new EmptyCartWithShipAddress_Baked {
      cartsApi(cart.refNum).get().asThe[CartResponse].result.shippingAddress mustBe defined

      //delete the shipping address
      val noAddressCart: TheResponse[CartResponse] =
        cartsApi(cart.refNum).shippingAddress.delete().asThe[CartResponse]
      //shipping address must not be defined
      noAddressCart.result.shippingAddress must not be defined
      noAddressCart.warnings.value must contain(NoShipAddress(cart.refNum).description)

      //fails if the cart does not have shipping address
      cartsApi(cart.refNum).shippingAddress.delete().mustFailWith400(NoShipAddress(cart.refNum))
    }

    "fails if the cart is not found" in new EmptyCartWithShipAddress_Baked {
      cartsApi("NOPE").shippingAddress.delete().mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      OrderShippingAddresses.length.result.gimme must === (1)
    }

    "fails if the order has already been placed" in new Order_Baked {
      cartsApi(order.refNum).shippingAddress
        .delete()
        .mustFailWith400(OrderAlreadyPlaced(cart.refNum))

      OrderShippingAddresses.length.result.gimme must === (1)
    }
  }

  "PATCH /v1/carts/:refNum/shipping-method" - {
    "succeeds if the cart meets the shipping restrictions" in new ShippingMethodFixture {
      cartsApi(cart.refNum).shippingMethod
        .update(UpdateShippingMethod(lowShippingMethod.id))
        .asTheResult[CartResponse]
        .shippingMethod
        .value
        .name must === (lowShippingMethod.adminDisplayName)

      val shipMethod: OrderShippingMethod =
        OrderShippingMethods.findByOrderRef(cart.refNum).gimme.head
      shipMethod.cordRef must === (cart.refNum)
      shipMethod.shippingMethodId must === (lowShippingMethod.id)
    }

    "fails if the cart does not meet the shipping restrictions" in new ShippingMethodFixture {
      cartsApi(cart.refNum).shippingMethod
        .update(UpdateShippingMethod(highShippingMethod.id))
        .mustFailWith400(ShippingMethodNotApplicableToCart(highShippingMethod.id, cart.refNum))
    }

    "fails if the shipping method isn't found" in new ShippingMethodFixture {
      cartsApi(cart.refNum).shippingMethod
        .update(UpdateShippingMethod(999))
        .mustFailWith400(NotFoundFailure400(ShippingMethod, 999))
    }

    "fails if the shipping method isn't active" in new ShippingMethodFixture {
      cartsApi(cart.refNum).shippingMethod
        .update(UpdateShippingMethod(inactiveShippingMethod.id))
        .mustFailWith400(ShippingMethodIsNotActive(inactiveShippingMethod.id))
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed

  trait ShippingMethodFixture extends EmptyCartWithShipAddress_Baked {
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
        _       ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))
        _       ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))

        lowShippingMethod ← * <~ ShippingMethods.create(lowSm)
        inactiveShippingMethod ← * <~ ShippingMethods.create(
                                    lowShippingMethod.copy(isActive = false, code = "INACTIVE"))
        highShippingMethod ← * <~ ShippingMethods.create(highSm)

        _ ← * <~ CartTotaler.saveTotals(cart)
      } yield (lowShippingMethod, inactiveShippingMethod, highShippingMethod)
    }.gimme
  }

  trait OrderShippingMethodFixture extends ShippingMethodFixture {
    val shipment = (for {
      orderShipMethod ← * <~ OrderShippingMethods.create(
                           OrderShippingMethod.build(cordRef = cart.refNum,
                                                     method = highShippingMethod))
      shipment ← * <~ Shipments.create(
                    Shipment(cordRef = cart.refNum,
                             orderShippingMethodId = Some(orderShipMethod.id)))
    } yield shipment).gimme
  }

  trait PaymentStateFixture extends Fixture {

    val (cc, op, ccc) = (for {
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      op ← * <~ OrderPayments.create(
              Factories.orderPayment.copy(cordRef = cart.refNum, paymentMethodId = cc.id))
      ccc ← * <~ CreditCardCharges.create(
               Factories.creditCardCharge.copy(creditCardId = cc.id, orderPaymentId = op.id))
    } yield (cc, op, ccc)).gimme
  }

  class TaxesFixture(regionId: Int) extends ShipmentSeeds with ProductSku_ApiFixture {
    // Shipping method
    val shipMethodId = ShippingMethods.create(shippingMethods(2)).gimme.id

    // Cart
    val cartRef =
      cartsApi.create(CreateCart(email = "foo@bar.com".some)).as[CartResponse].referenceNumber

    cartsApi(cartRef).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1))).mustBeOk()

    private val randomAddress = CreateAddressPayload(regionId = regionId,
                                                     name = Lorem.letterify("???"),
                                                     address1 = Lorem.letterify("???"),
                                                     city = Lorem.letterify("???"),
                                                     zip = Lorem.numerify("#####"))

    cartsApi(cartRef).shippingAddress.create(randomAddress).mustBeOk()

    val totals = cartsApi(cartRef).shippingMethod
      .update(UpdateShippingMethod(shipMethodId))
      .asTheResult[CartResponse]
      .totals
  }
}
