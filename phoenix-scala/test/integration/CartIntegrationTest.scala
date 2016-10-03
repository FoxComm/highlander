import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import cats.syntax.order
import util.Extensions._
import failures.CartFailures._
import failures.LockFailures._
import failures.ShippingMethodFailures._
import failures.{GeneralFailure, NotFoundFailure400, NotFoundFailure404}
import models.cord._
import models.cord.lineitems._
import models.location.{Address, Addresses, Regions}
import models.payment.creditcard._
import models.product.Mvp
import models.rules.QueryStatement
import models.shipping._
import org.json4s.jackson.JsonMethods._
import payloads.AddressPayloads.UpdateAddressPayload
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.UpdateShippingMethod
import responses.cord.CartResponse
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CartIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "GET /v1/carts/:refNum" - {
    "payment state" - {

      "displays 'cart' payment state" in new Fixture {
        val fullCart = cartsApi(cart.refNum).get().asTheResult[CartResponse]
        fullCart.paymentState must === (CreditCardCharge.Cart)
      }

      "displays 'auth' payment state" in new PaymentStateFixture {
        CreditCardCharges.findById(ccc.id).extract.map(_.state).update(CreditCardCharge.Auth).gimme

        val fullCart = cartsApi(cart.refNum).get().asTheResult[CartResponse]
        fullCart.paymentState must === (CreditCardCharge.Auth)
      }
    }

    "returns correct image path" in new Fixture {
      val imgUrl = "testImgUrl";
      (for {
        product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head.copy(image = imgUrl))
        li      ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))
      } yield {}).gimme

      val fullCart = cartsApi(cart.refNum).get().asTheResult[CartResponse]
      fullCart.lineItems.skus.size must === (1)
      fullCart.lineItems.skus.head.imagePath must === (imgUrl)
    }
  }

  "POST /v1/orders/:refNum/line-items" - {
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
      cartsApi(cart.refNum).lineItems
        .add(Seq(UpdateLineItemsPayload(simpleSku.code, 1)))
        .mustFailWith400(SKUWithNoProductAdded(cart.refNum, simpleSku.code))
    }

    "should respond with 404 if cart is not found" in {
      cartsApi("NOPE").lineItems.add(payload).mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }
  }

  "PATCH /v1/orders/:refNum/line-items" - {
    val addPayload = Seq(UpdateLineItemsPayload("SKU-YAX", 2))

    "should successfully add line items" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val root = cartsApi(cart.refNum).lineItems.update(addPayload).asTheResult[CartResponse]
      val skus = root.lineItems.skus
      skus must have size 1
      skus.map(_.sku).toSet must === (Set("SKU-YAX"))
      skus.map(_.quantity).toSet must === (Set(4))
    }

    "adding a SKU with no product should return an error" in new OrderShippingMethodFixture
    with Sku_Raw with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      cartsApi(cart.refNum).lineItems
        .update(Seq(UpdateLineItemsPayload(simpleSku.code, 1)))
        .mustFailWith400(SKUWithNoProductAdded(cart.refNum, simpleSku.code))
    }

    "should successfully remove line items" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val subtractPayload = Seq(UpdateLineItemsPayload("SKU-YAX", -1))

      val root = cartsApi(cart.refNum).lineItems.update(subtractPayload).asTheResult[CartResponse]

      val skus = root.lineItems.skus
      skus must have size 1
      skus.map(_.sku).toSet must === (Set("SKU-YAX"))
      skus.map(_.quantity).toSet must === (Set(1))
    }

    "removing too many of an item should remove all of that item" in new OrderShippingMethodFixture
    with EmptyCartWithShipAddress_Baked with PaymentStateFixture {
      val subtractPayload = Seq(UpdateLineItemsPayload("SKU-YAX", -3))

      val root = cartsApi(cart.refNum).lineItems.update(subtractPayload).asTheResult[CartResponse]

      val skus = root.lineItems.skus
      skus must have size 0
    }

    "should respond with 404 if cart is not found" in {
      cartsApi("NOPE").lineItems.add(addPayload).mustFailWith404(NotFoundFailure404(Cart, "NOPE"))
    }

    "should add line items if productId and skuId are different" in new OrderShippingMethodFixture
    with ProductAndSkus_Baked {
      val addPayload = Seq(UpdateLineItemsPayload("TEST", 1))
      val root       = cartsApi(cart.refNum).lineItems.add(addPayload).asTheResult[CartResponse]
      val skus       = root.lineItems.skus
      skus must have size 2
      skus.map(_.sku) must contain theSameElementsAs Seq("SKU-YAX", "TEST")
      skus.map(_.quantity) must contain theSameElementsAs Seq(1, 2)
    }
  }

  "POST /v1/orders/:refNum/lock" - {
    "successfully locks a cart" in new Fixture {
      cartsApi(cart.refNum).lock().mustBeOk()

      val lockedCart = Carts.findByRefNum(cart.refNum).gimme.head
      lockedCart.isLocked must === (true)

      val locks = CartLockEvents.findByCartRef(cart.refNum).gimme
      locks.length must === (1)
      val lock = locks.head
      lock.lockedBy must === (1)
    }

    "refuses to lock an already locked cart" in new Fixture {
      Carts.update(cart, cart.copy(isLocked = true)).gimme

      cartsApi(cart.refNum).lock().mustFailWith400(LockedFailure(Cart, cart.refNum))
    }

    "avoids race condition" in new Fixture {
      pending // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587

      def request = cartsApi(cart.refNum).lock()

      val responses = Seq(0, 1).par.map(_ ⇒ request)
      responses.map(_.status) must contain allOf (StatusCodes.OK, StatusCodes.BadRequest)
      CartLockEvents.gimme.length mustBe 1
    }
  }

  "POST /v1/orders/:refNum/unlock" - {
    "unlocks cart" in new Fixture {
      cartsApi(cart.refNum).lock().mustBeOk()
      cartsApi(cart.refNum).unlock().mustBeOk()

      Carts.findByRefNum(cart.refNum).gimme.head.isLocked must === (false)
    }

    "refuses to unlock an already unlocked cart" in new Fixture {
      cartsApi(cart.refNum).unlock().mustFailWith400(NotLockedFailure(Cart, cart.refNum))
    }
  }

  /*
  "handles credit cards" - {
    val today = new DateTime
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val payload = CreateCreditCard(holderName = "Jax", number = StripeSupport.successfulCard, cvv = "123",
      expYear = today.getYear + 1, expMonth = today.getMonthOfYear, isDefault = true)

    "fails if the cart is not found" in {
      val response = POST(
        s"v1/orders/5/payment-methods/credit-card",
        payload)

      response.status must === (StatusCodes.NotFound)
    }

    "fails if the payload is invalid" in {
      val cart = Orders.save(Factories.cart.copy(customerId = 1)).gimme
      val response = POST(
        s"v1/orders/${cart.refNum}/payment-methods/credit-card",
        payload.copy(cvv = "", holderName = ""))

      val errors = parse(response.bodyText).extract[Errors]

      errors must === (Map("errors" → Seq("holderName must not be empty", "cvv must match regular expression " +
        "'[0-9]{3,4}'")))
      response.status must === (StatusCodes.BadRequest)
    }

    "fails if the card is invalid according to Stripe" ignore {
      val cart = Orders.save(Factories.cart.copy(customerId = 1)).gimme
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val response = POST(
        s"v1/orders/${cart.refNum}/payment-methods/credit-card",
        payload.copy(number = StripeSupport.declinedCard))

      val body = response.bodyText
      val errors = parse(body).extract[Errors]

      errors must === (Map("errors" → Seq("Your card was declined.")))
      response.status must === (StatusCodes.BadRequest)
    }

    /*
    "successfully creates records" ignore {
      val cart = Orders.save(Factories.cart.copy(customerId = 1)).gimme
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val customer = customerStub.copy(id = customerId)
      val addressPayload = CreateAddressPayload(name = "Home", stateId = 46, state = "VA".some, street1 = "500 Blah",
        city = "Richmond", zip = "50000")
      val payloadWithAddress = payload.copy(address = addressPayload.some)

      val response = POST(
        s"v1/orders/${cart.refNum}/payment-methods/credit-card",
        payloadWithAddress)

      val body = response.bodyText

      val cc = CreditCards.findById(1).futureValue.get
      val payment = OrderPayments.findAllByOrderId(cart.refNum).futureValue.head
      val (address, billingAddress) = BillingAddresses.findByPaymentId(payment.id).futureValue.get

      val respOrder = parse(body).extract[fullCart.Root]

      cc.customerId must === (customerId)
      cc.lastFour must === (payload.lastFour)
      cc.expMonth must === (payload.expMonth)
      cc.expYear must === (payload.expYear)
      cc.isDefault must === (true)

      payment.appliedAmount must === (0)
      payment.cordRef must === (cart.refNum)
      payment.status must === ("auth")

      response.status must === (StatusCodes.OK)

      address.stateId must === (addressPayload.stateId)
      address.customerId must === (customerId)
    }
   */
  }
   */

  "PATCH /v1/orders/:refNum/shipping-address/:id" - {

    "copying a shipping address from a customer's book" - {

      "succeeds if the address exists in their book" in new EmptyCustomerCart_Baked
      with CustomerAddress_Raw {
        val cartResponse = cartsApi(cart.refNum).shippingAddress
          .updateFromAddress(address.id)
          .asTheResult[CartResponse]

        val shippingAddressUpd = OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value
        shippingAddressUpd.cordRef must === (cart.refNum)
      }

      "removes an existing shipping address before copying new address" in new EmptyCartWithShipAddress_Baked {
        val newAddress =
          Addresses.create(address.copy(name = "Little Mary", isDefaultShipping = false)).gimme

        val fst :: snd :: Nil = List(address.id, newAddress.id).map { id ⇒
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
              Factories.address.copy(customerId = customer.id,
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

        val shippingAddressUpd = OrderShippingAddresses.findByOrderRef(cart.refNum).one.gimme.value
        shippingAddressUpd.cordRef must === (cart.refNum)
      }
    }
  }

  "PATCH /v1/orders/:refNum/shipping-address" - {

    "succeeds when a subset of the fields in the address change" in new EmptyCartWithShipAddress_Baked {
      cartsApi(cart.refNum).shippingAddress
        .update(UpdateAddressPayload(name = "New name".some, city = "Queen Anne".some))
        .mustBeOk()

      val (shippingAddressUpd :: Nil) =
        OrderShippingAddresses.findByOrderRef(cart.refNum).gimme.toList

      shippingAddressUpd.name must === ("New name")
      shippingAddressUpd.city must === ("Queen Anne")
      shippingAddressUpd.address1 must === (address.address1)
      shippingAddressUpd.address2 must === (address.address2)
      shippingAddressUpd.regionId must === (address.regionId)
      shippingAddressUpd.zip must === (address.zip)
    }

    "does not update the address book" in new EmptyCartWithShipAddress_Baked {
      cartsApi(cart.refNum).shippingAddress
        .update(UpdateAddressPayload(name = "Another name".some, city = "Fremont".some))
        .mustBeOk()

      val addressBook = Addresses.findOneById(address.id).gimme.value

      addressBook.name must === (address.name)
      addressBook.city must === (address.city)
    }

    "full cart returns updated shipping address" in new EmptyCartWithShipAddress_Baked {
      val updateResponse = cartsApi(cart.refNum).shippingAddress
        .update(UpdateAddressPayload(name = "Even newer name".some, city = "Queen Max".some))
        .asTheResult[CartResponse]
      checkCart(updateResponse)

      val getResponse = cartsApi(cart.refNum).get().asTheResult[CartResponse]
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
    "succeeds if an address exists" in new EmptyCartWithShipAddress_Baked {
      val noShipAddressFailure = NoShipAddress(cart.refNum).description

      //get cart and make sure it has a shipping address
      val cartWithAddress = cartsApi(cart.refNum).get().asThe[CartResponse].result
      cartWithAddress.shippingAddress mustBe defined

      //delete the shipping address
      //shipping address must not be defined
      val cartWithoutAddress = cartsApi(cart.refNum).shippingAddress.delete().asThe[CartResponse]
      cartWithoutAddress.result.shippingAddress must not be defined
      cartWithoutAddress.warnings.value must contain(noShipAddressFailure)

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

  "PATCH /v1/orders/:refNum/shipping-method" - {
    "succeeds if the cart meets the shipping restrictions" in new ShippingMethodFixture {
      val fullCart = cartsApi(cart.refNum).shippingMethod
        .update(UpdateShippingMethod(lowShippingMethod.id))
        .asTheResult[CartResponse]
      fullCart.shippingMethod.value.name must === (lowShippingMethod.adminDisplayName)

      val orderShippingMethod = OrderShippingMethods.findByOrderRef(cart.refNum).gimme.head
      orderShippingMethod.cordRef must === (cart.refNum)
      orderShippingMethod.shippingMethodId must === (lowShippingMethod.id)
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
    val lowConditions = parse(
        """
              | {
              |   "comparison": "and",
              |   "conditions": [{
              |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
              |   }]
              | }
            """.stripMargin).extract[QueryStatement]

    val highConditions = parse(
        """
              | {
              |   "comparison": "and",
              |   "conditions": [{
              |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 250
              |   }]
              | }
            """.stripMargin).extract[QueryStatement]

    val lowSm = Factories.shippingMethods.head
      .copy(adminDisplayName = "Low", conditions = lowConditions.some, code = "HIGH")
    val highSm = Factories.shippingMethods.head
      .copy(adminDisplayName = "High", conditions = highConditions.some, code = "LOW")

    val (lowShippingMethod, inactiveShippingMethod, highShippingMethod) = (for {
      product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head.copy(price = 100))
      _       ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))
      _       ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))

      lowShippingMethod ← * <~ ShippingMethods.create(lowSm)
      inactiveShippingMethod ← * <~ ShippingMethods.create(
                                  lowShippingMethod.copy(isActive = false, code = "INACTIVE"))
      highShippingMethod ← * <~ ShippingMethods.create(highSm)

      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield (lowShippingMethod, inactiveShippingMethod, highShippingMethod)).gimme
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
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      op ← * <~ OrderPayments.create(
              Factories.orderPayment.copy(cordRef = cart.refNum, paymentMethodId = cc.id))
      ccc ← * <~ CreditCardCharges.create(
               Factories.creditCardCharge.copy(creditCardId = cc.id, orderPaymentId = op.id))
    } yield (cc, op, ccc)).gimme
  }
}
