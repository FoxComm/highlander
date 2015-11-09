import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.testkit.TestActorRef

import models._
import models.rules.QueryStatement
import payloads.{Assignment, UpdateOrderPayload}
import responses.{StoreAdminResponse, FullOrderWithWarnings, FullOrder}
import services.CartFailures.OrderMustBeCart
import services.{OrderStatusTransitionNotAllowed, LockedFailure, GeneralFailure, NotFoundFailure404}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import Order._
import utils.{RemorseTimer, Tick}
import models.OrderLockEvents.scope._
import org.json4s.jackson.JsonMethods._

import utils.time._

class OrderIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._

  type Errors = Map[String, Seq[String]]

  def getUpdated(refNum: String) = db.run(Orders.findByRefNum(refNum).result.headOption).futureValue.value

  "GET /v1/orders/:refNum" - {
    "payment status" - {
      "does not display payment status if no cc" in new Fixture {
        Orders.findByRefNum(order.refNum).map(_.status).update(Order.ManualHold).run.futureValue

        val response = GET(s"v1/orders/${order.refNum}")
        response.status must === (StatusCodes.OK)
        val fullOrder = response.as[FullOrder.Root]
        fullOrder.paymentStatus must not be defined
        fullOrder.payment must not be defined
      }

      "displays payment status if cc present" in new PaymentStatusFixture {
        Orders.findByRefNum(order.refNum).map(_.status).update(Order.ManualHold).run.futureValue
        CreditCardCharges.findById(ccc.id).extract.map(_.status).update(CreditCardCharge.Auth).run.futureValue

        val response = GET(s"v1/orders/${order.refNum}")
        response.status must === (StatusCodes.OK)
        val fullOrder = response.as[FullOrder.Root]

        fullOrder.paymentStatus.value must === (CreditCardCharge.Auth)
        fullOrder.payment.value.status must === (CreditCardCharge.Auth)
      }

      "displays 'cart' payment status if order is cart and cc present" in new PaymentStatusFixture {
        Orders.findByRefNum(order.refNum).map(_.status).update(Order.Cart).run.futureValue
        CreditCardCharges.findById(ccc.id).extract.map(_.status).update(CreditCardCharge.Auth).run.futureValue

        val response = GET(s"v1/orders/${order.refNum}")
        response.status must === (StatusCodes.OK)
        val fullOrder = response.as[FullOrder.Root]

        fullOrder.paymentStatus.value must === (CreditCardCharge.Cart)
        fullOrder.payment.value.status must === (CreditCardCharge.Auth)
      }
    }
  }

  "POST /v1/orders/:refNum/line-items" - {
    "should successfully update line items" in new OrderFixture {
      val response = POST(
        s"v1/orders/${order.refNum}/line-items",
        """
          | [ { "sku": "SKU-YAX", "quantity": 1 },
          |   { "sku": "SKU-ABC", "quantity": 2 } ]
        """.stripMargin)

      val root = parse(response.bodyText).extract[FullOrder.Root]
      root.lineItems.skus.map(_.sku).sortBy(identity) must ===(List("SKU-ABC", "SKU-ABC", "SKU-YAX"))
    }

    "should return error if order is not in Cart state" in new OrderFixture {
      pending
      Orders.findByRefNum(order.refNum).map(_.status).update(Order.ManualHold).run().futureValue

      val response = POST(
        s"v1/orders/${order.refNum}/line-items",
        """
          | [ { "sku": "SKU-YAX", "quantity": 1 },
          |   { "sku": "SKU-ABC", "quantity": 2 } ]
        """.stripMargin)

      response.status must ===(StatusCodes.NotFound)
    }
  }

  "PATCH /v1/orders/:refNum" - {

    "successfully" in {
      val order = Orders.saveNew(Factories.order).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(FraudHold))

      response.status must === (StatusCodes.OK)

      val responseOrder = parse(response.bodyText).extract[FullOrder.Root]
      responseOrder.orderStatus must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in {
      val order = Orders.saveNew(Factories.order).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(Cart))

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (OrderStatusTransitionNotAllowed(order.status, Cart, order.refNum).description)
    }

    "fails if transition from current status is not allowed" in {
      val order = Orders.saveNew(Factories.order.copy(status = Canceled)).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (OrderStatusTransitionNotAllowed(order.status, ManualHold, order.refNum).description)
    }

    "fails if the order is not found" in {
      Orders.saveNew(Factories.order).run().futureValue

      val response = PATCH(s"v1/orders/NOPE", UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.NotFound)
      response.errors must === (NotFoundFailure404(Order, "NOPE").description)
    }

    /* This test should really test against an order and not a *cart*. Karin has filed a story to come back to this
    "cancels order with line items and payments" in new PaymentMethodsFixture {
      (for {
        creditCard ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id, billingAddressId = address.id))
        payment ← OrderPayments.save(Factories.orderPayment.copy(orderId = order.id, paymentMethodId = creditCard.id))
        _ ← OrderLineItems ++= Factories.orderLineItems.map(li ⇒ li.copy(orderId = order.id))
      } yield (creditCard, payment)).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(Canceled))

      val responseOrder = parse(response.bodyText).extract[FullOrder.Root]
      responseOrder.orderStatus must === (Canceled)
      responseOrder.lineItems.head.status must === (OrderLineItem.Canceled)

      // Testing via DB as currently FullOrder returns 'order.status' as 'payment.status'
      // OrderPayments.findAllByOrderId(order.id).futureValue.head.status must === ("cancelAuth")
    }
    */
  }

  "POST /v1/orders/:refNum/increase-remorse-period" - {

    "successfully" in {
      val order = Orders.saveNew(Factories.order.copy(status = Order.RemorseHold)).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")

      val result = parse(response.bodyText).extract[FullOrder.Root]
      result.remorsePeriodEnd must ===(order.remorsePeriodEnd.map(_.plusMinutes(15)))
    }

    "only when in RemorseHold status" in {
      val order = Orders.saveNew(Factories.order).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")
      response.status must ===(StatusCodes.BadRequest)

      val newOrder = Orders.findById(order.id).extract.one.run().futureValue.value
      newOrder.remorsePeriodEnd must ===(order.remorsePeriodEnd)
    }
  }

  "POST /v1/orders/:refNum/lock" - {
    "successfully locks an order" in {
      val order = Orders.saveNew(Factories.order).run().futureValue
      StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue

      val response = POST(s"v1/orders/${order.referenceNumber}/lock")
      response.status must === (StatusCodes.OK)

      val lockedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.head
      lockedOrder.locked must === (true)

      val locks = OrderLockEvents.findByOrder(order.id).result.run().futureValue
      locks.length must === (1)
      val lock = locks.head
      lock.lockedBy must === (1)
    }

    "refuses to lock an already locked order" in {
      val order = Orders.saveNew(Factories.order.copy(locked = true)).run().futureValue

      val response = POST(s"v1/orders/${order.referenceNumber}/lock")
      response.status must === (StatusCodes.BadRequest)
      response.errors must === (LockedFailure(Order, order.referenceNumber).description)
    }

    "avoids race condition" in {
      StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue
      val order = Orders.saveNew(Factories.order).run().futureValue

      def request = POST(s"v1/orders/${order.referenceNumber}/lock")

      val responses = Seq(0, 1).par.map(_ ⇒ request)
      responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
      OrderLockEvents.result.run().futureValue.length mustBe 1
    }
  }

  "POST /v1/orders/:refNum/unlock" - {
    "unlocks an order" in {
      StoreAdmins.saveNew(Factories.storeAdmin).run().futureValue
      val order = Orders.saveNew(Factories.order).run().futureValue

      POST(s"v1/orders/${order.referenceNumber}/lock")

      val response = POST(s"v1/orders/${order.referenceNumber}/unlock")
      response.status must === (StatusCodes.OK)

      val unlockedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.head
      unlockedOrder.locked must === (false)
    }

    "refuses to unlock an already unlocked order" in {
      val order = Orders.saveNew(Factories.order).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/unlock")

      response.status must === (StatusCodes.BadRequest)
      response.errors must === (GeneralFailure("Order is not locked").description)
    }

    "adjusts remorse period when order is unlocked" in new RemorseFixture {
      val timer = TestActorRef(new RemorseTimer())

      POST(s"v1/orders/$refNum/lock")

      (timer ? Tick).futureValue // Nothing should happen
      val order1 = getUpdated(refNum)
      order1.remorsePeriodEnd must ===(order.remorsePeriodEnd)
      order1.status must ===(Order.RemorseHold)

      Thread.sleep(3000)

      POST(s"v1/orders/$refNum/unlock")

      (timer ? Tick).futureValue

      val order2 = getUpdated(refNum)
      val newRemorseEnd = order2.remorsePeriodEnd.value

      originalRemorseEnd.durationUntil(newRemorseEnd).getSeconds mustBe >= (3L)
      order2.status must ===(Order.RemorseHold)
    }

    "uses most recent lock record" in new RemorseFixture {
      OrderLockEvents.saveNew(OrderLockEvent(id = 1, lockedBy = 1, orderId = 1, lockedAt = Instant.now.minusMinutes(30)))

      POST(s"v1/orders/$refNum/lock")
      POST(s"v1/orders/$refNum/unlock")

      val newRemorseEnd = getUpdated(order.referenceNumber).remorsePeriodEnd.value
      originalRemorseEnd.durationUntil(newRemorseEnd).getMinutes mustBe 0
    }

    "adds 15 minutes to remorse if order lock event is absent" in new RemorseFixture {
      POST(s"v1/orders/$refNum/lock")
      db.run(OrderLockEvents.findById(1).delete).futureValue
      // Sanity check
      OrderLockEvents.findByOrder(order.id).mostRecentLock.result.headOption.run().futureValue must ===(None)
      POST(s"v1/orders/$refNum/unlock")
      getUpdated(refNum).remorsePeriodEnd.value must ===(originalRemorseEnd.plusMinutes(15))
    }
  }

  "POST /v1/orders/:refNum/assignees" - {

    "can be assigned to order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      val fullOrderWithWarnings = parse(response.bodyText).extract[FullOrderWithWarnings]
      fullOrderWithWarnings.order.assignees must not be empty
      fullOrderWithWarnings.order.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
      fullOrderWithWarnings.warnings mustBe empty
    }

    "can be assigned to locked order" in {
      val (order, storeAdmin) = (for {
        customer ← Customers.saveNew(Factories.customer)
        order ← Orders.saveNew(Factories.order.copy(locked = true, customerId = customer.id))
        storeAdmin ← StoreAdmins.saveNew(authedStoreAdmin)
      } yield (order, storeAdmin)).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)
    }

    "404 if order is not found" in new Fixture {
      val response = POST(s"v1/orders/NOPE/assignees", Assignment(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
    }

    "warning if assignee is not found" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      val fullOrderWithWarnings = parse(response.bodyText).extract[FullOrderWithWarnings]
      fullOrderWithWarnings.order.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
      fullOrderWithWarnings.warnings mustBe Seq(NotFoundFailure404("storeAdmin with id=999 not found"))
    }

    "can be viewed with order" in new Fixture {
      val response1 = GET(s"v1/orders/${order.referenceNumber}")
      response1.status must === (StatusCodes.OK)
      val responseOrder1 = parse(response1.bodyText).extract[FullOrder.Root]
      responseOrder1.assignees mustBe empty

      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      val response2 = GET(s"v1/orders/${order.referenceNumber}")
      response2.status must === (StatusCodes.OK)
      val responseOrder2 = parse(response2.bodyText).extract[FullOrder.Root]
      responseOrder2.assignees must not be empty
      responseOrder2.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
    }

    "do not create duplicate records" in new Fixture {
      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }
  }

  /*
  "handles credit cards" - {
    val today = new DateTime
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val payload = CreateCreditCard(holderName = "Jax", number = StripeSupport.successfulCard, cvv = "123",
      expYear = today.getYear + 1, expMonth = today.getMonthOfYear, isDefault = true)

    "fails if the order is not found" in {
      val response = POST(
        s"v1/orders/5/payment-methods/credit-card",
        payload)

      response.status must === (StatusCodes.NotFound)
    }

    "fails if the payload is invalid" in {
      val order = Orders.save(Factories.order.copy(customerId = 1)).run().futureValue
      val response = POST(
        s"v1/orders/${order.referenceNumber}/payment-methods/credit-card",
        payload.copy(cvv = "", holderName = ""))

      val errors = parse(response.bodyText).extract[Errors]

      errors must === (Map("errors" → Seq("holderName must not be empty", "cvv must match regular expression " +
        "'[0-9]{3,4}'")))
      response.status must === (StatusCodes.BadRequest)
    }

    "fails if the card is invalid according to Stripe" ignore {
      val order = Orders.save(Factories.order.copy(customerId = 1)).run().futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val response = POST(
        s"v1/orders/${order.referenceNumber}/payment-methods/credit-card",
        payload.copy(number = StripeSupport.declinedCard))

      val body = response.bodyText
      val errors = parse(body).extract[Errors]

      errors must === (Map("errors" → Seq("Your card was declined.")))
      response.status must === (StatusCodes.BadRequest)
    }

    /*
    "successfully creates records" ignore {
      val order = Orders.save(Factories.order.copy(customerId = 1)).run().futureValue
      val customerId = db.run(Customers.returningId += customerStub).futureValue
      val customer = customerStub.copy(id = customerId)
      val addressPayload = CreateAddressPayload(name = "Home", stateId = 46, state = Some("VA"), street1 = "500 Blah",
        city = "Richmond", zip = "50000")
      val payloadWithAddress = payload.copy(address = Some(addressPayload))

      val response = POST(
        s"v1/orders/${order.referenceNumber}/payment-methods/credit-card",
        payloadWithAddress)

      val body = response.bodyText

      val cc = CreditCards.findById(1).futureValue.get
      val payment = OrderPayments.findAllByOrderId(order.id).futureValue.head
      val (address, billingAddress) = BillingAddresses.findByPaymentId(payment.id).futureValue.get

      val respOrder = parse(body).extract[FullOrder.Root]

      cc.customerId must === (customerId)
      cc.lastFour must === (payload.lastFour)
      cc.expMonth must === (payload.expMonth)
      cc.expYear must === (payload.expYear)
      cc.isDefault must === (true)

      payment.appliedAmount must === (0)
      payment.orderId must === (order.id)
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

      "succeeds if the address exists in their book" in new AddressFixture {
        val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address/${address.id}")
        response.status must ===(StatusCodes.OK)
        val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

        val shippingAddressMap = shippingAddress.toMap -- Seq("id", "orderId", "createdAt", "deletedAt", "updatedAt")
        val addressMap = address.toMap -- Seq("id", "customerId", "isDefaultShipping", "createdAt", "deletedAt",
          "deletedAt", "updatedAt")

        shippingAddressMap must ===(addressMap)
        shippingAddress.orderId must ===(order.id)
      }

      "removes an existing shipping address before copying new address" in new AddressFixture {
        val newAddress = Addresses.saveNew(address.copy(name = "New", isDefaultShipping = false)).run().futureValue

        val fst :: snd :: Nil = List(address.id, newAddress.id).map { id ⇒
          PATCH(s"v1/orders/${order.referenceNumber}/shipping-address/$id")
        }

        fst.status must === (StatusCodes.OK)
        snd.status must === (StatusCodes.OK)

        val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

        shippingAddress.name must === ("New")
      }

      "errors if the address does not exist" in new AddressFixture {
        val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address/99")

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (NotFoundFailure404(Address, 99).description)
      }
    }

    "editing a shipping address by copying from a customer's address book" - {

      "succeeds when the address exists" in new ShippingAddressFixture {
        val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address/${newAddress.id}")

        response.status must === (StatusCodes.OK)
        val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

        val shippingAddressMap = shippingAddress.toMap -- Seq("id", "customerId", "orderId", "createdAt", "deletedAt",
          "updatedAt")
        val addressMap = newAddress.toMap -- Seq("id", "customerId", "orderId", "isDefaultShipping", "createdAt",
          "deletedAt", "updatedAt")

        shippingAddressMap must === (addressMap)
        shippingAddress.orderId must === (order.id)
      }

      "errors if the address does not exist" in new ShippingAddressFixture {
        val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address/99")

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (NotFoundFailure404(Address, 99).description)
      }

      "does not change the current shipping address if the edit fails" in new ShippingAddressFixture {
        val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address/101")

        response.status must === (StatusCodes.NotFound)
        val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

        val shippingAddressMap = shippingAddress.toMap -- Seq("id", "customerId", "orderId", "createdAt", "deletedAt",
          "updatedAt")
        val addressMap = address.toMap -- Seq("id", "customerId", "orderId", "isDefaultShipping", "createdAt",
          "deletedAt", "updatedAt")

        shippingAddressMap must ===(addressMap)
        shippingAddress.orderId must ===(order.id)
      }

    }
  }

  "PATCH /v1/orders/:refNum/shipping-address" - {

    "succeeds when a subset of the fields in the address change" in new ShippingAddressFixture {
      val updateAddressPayload = payloads.UpdateAddressPayload(name = Some("New name"), city = Some("Queen Anne"))
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address", updateAddressPayload)

      response.status must === (StatusCodes.OK)

      val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

      shippingAddress.name must === ("New name")
      shippingAddress.city must === ("Queen Anne")
      shippingAddress.address1 must === (address.address1)
      shippingAddress.address2 must === (address.address2)
      shippingAddress.regionId must === (address.regionId)
      shippingAddress.zip must === (address.zip)
    }

    "does not update the address book" in new ShippingAddressFixture {
      val updateAddressPayload = payloads.UpdateAddressPayload(name = Some("Another name"), city = Some("Fremont"))
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address", updateAddressPayload)

      response.status must === (StatusCodes.OK)

      val addressBook = Addresses.findOneById(address.id).run().futureValue.value

      addressBook.name must === (address.name)
      addressBook.city must === (address.city)
    }

    "full order returns updated shipping addresss" in new ShippingAddressFixture {
      //update address
      val name = "Even newer name"
      val city = "Queen Max"
      val updateAddressPayload = payloads.UpdateAddressPayload(name = Some(name), city = Some(city))
      val addressUpdateResponse = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address", updateAddressPayload)

      //get full order
      val fullOrderResponse = GET(s"v1/orders/${order.referenceNumber}")

      //test both responses
      val responses = Seq(addressUpdateResponse, fullOrderResponse)
      responses.foreach(r ⇒ {
        r.status must === (StatusCodes.OK)
        val fullOrder = r.as[FullOrder.Root]
        fullOrder.shippingAddress match {
          case Some(addr) ⇒
            addr.name must === (name)
            addr.city must === (city)
            addr.address1 must === (address.address1)
            addr.address2 must === (address.address2)
            val region = Regions.findOneById(address.regionId).run().futureValue.value
            addr.region must === (region)
            addr.zip must === (address.zip)

          case None ⇒
            fail("FullOrder should have a shipping address")
        }
      })
    }
  }

  "DELETE /v1/orders/:refNum/shipping-address" - {
    "succeeds if an address exists" in new ShippingAddressFixture {
      Orders.update(order.copy(status = Order.Cart)).run().futureValue

      //get order and make sure it has a shipping address
      val fullOrderResponse = GET(s"v1/orders/${order.referenceNumber}")
      val fullOrder = fullOrderResponse.as[FullOrder.Root]
      fullOrder.shippingAddress mustBe defined

      //delete the shipping address
      val deleteResponse = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
      deleteResponse.status must === (StatusCodes.OK)

      //shipping address must not be defined
      val lessThanFullOrder = deleteResponse.as[FullOrder.Root]
      lessThanFullOrder.shippingAddress mustBe None

      //fails if the order does not have shipping address
      val deleteFailedResponse = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
      deleteFailedResponse.status must === (StatusCodes.BadRequest)
    }

    "fails if the order is not found" in new ShippingAddressFixture {
      Orders.update(order.copy(status = Order.Cart)).run().futureValue

      val response = DELETE(s"v1/orders/ABC-123/shipping-address")
      response.status must === (StatusCodes.NotFound)

      db.run(OrderShippingAddresses.length.result).futureValue must === (1)
    }

    "fails if the order is not in cart status" in new ShippingAddressFixture {
      Orders.update(order.copy(status = Order.FulfillmentStarted)).run().futureValue

      val response = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
      response.status must === (StatusCodes.BadRequest)
      response.errors must === (OrderMustBeCart(order.refNum).description)

      db.run(OrderShippingAddresses.length.result).futureValue must === (1)
    }
  }

  "adding a shipping method method to an order" - {
    "succeeds if the order meets the shipping restrictions" in new ShippingMethodFixture {
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-method",
        payloads.UpdateShippingMethod(shippingMethodId = lowShippingMethod.id))

      response.status must === (StatusCodes.OK)

      val orderShippingMethod = OrderShippingMethods.findByOrderId(order.id).result.run().futureValue.head
      orderShippingMethod.orderId must === (order.id)
      orderShippingMethod.shippingMethodId must === (lowShippingMethod.id)
    }

    "fails if the order does not meet the shipping restrictions" in new ShippingMethodFixture {
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-method",
        payloads.UpdateShippingMethod(shippingMethodId = highShippingMethod.id))

      response.status must === (StatusCodes.BadRequest)
    }

    "fails if the shipping method isn't found" in new ShippingMethodFixture {
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-method",
        payloads.UpdateShippingMethod(shippingMethodId = 999))

      response.status must === (StatusCodes.BadRequest)
    }

    "fails if the shipping method isn't active" in new ShippingMethodFixture {
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-method",
        payloads.UpdateShippingMethod(shippingMethodId = inactiveShippingMethod.id))

      response.status must === (StatusCodes.BadRequest)
    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(customerId = customer.id, status = Order.Cart))
      storeAdmin ← StoreAdmins.saveNew(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).run().futureValue
  }

  trait OrderFixture {
    val (order, storeAdmin, customer) = (for {
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(customerId = customer.id, status = Order.Cart))
      storeAdmin ← StoreAdmins.saveNew(authedStoreAdmin)
      skus ← Skus ++= Factories.skus
      summaries ← InventorySummaries ++= Factories.inventorySummaries
    } yield (order, storeAdmin, customer)).run().futureValue
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.saveNew(Factories.address.copy(customerId = customer.id)).run().futureValue
  }

  trait ShippingAddressFixture extends AddressFixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      newAddress ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, isDefaultShipping = false,
        name = "New Shipping", address1 = "29918 Kenloch Dr", city = "Farmington Hills", regionId = 4177))
    } yield(orderShippingAddress, newAddress)).run().futureValue
  }

  trait PaymentMethodsFixture extends AddressFixture {
  }

  trait ShippingMethodFixture extends AddressFixture {
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

    val (lowShippingMethod, inactiveShippingMethod, highShippingMethod) = (for {
      sku ← Skus.saveNew(Factories.skus.head.copy(price = 100))
      lineItemSku ← OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = sku.id, orderId = order.id))
      lineItem ← OrderLineItems.saveNew(OrderLineItem(orderId = order.id, originId = lineItemSku.id,
        originType = OrderLineItem.SkuItem))

      lowShippingMethod ← ShippingMethods.saveNew(Factories.shippingMethods.head.copy(conditions = Some(lowConditions)))
      inactiveShippingMethod ← ShippingMethods.saveNew(lowShippingMethod.copy(isActive = false))
      highShippingMethod ← ShippingMethods.saveNew(Factories.shippingMethods.head.copy(conditions = Some(highConditions)))
    } yield (lowShippingMethod, inactiveShippingMethod, highShippingMethod)).run().futureValue
  }

  trait RemorseFixture {
    val (admin, order) = (for {
      admin ← StoreAdmins.saveNew(Factories.storeAdmin)
      order ← Orders.saveNew(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).run().futureValue

    val refNum = order.referenceNumber
    val originalRemorseEnd = order.remorsePeriodEnd.value
  }

  trait PaymentStatusFixture extends Fixture {
    val (cc, op, ccc) = (for {
      cc ← CreditCards.saveNew(Factories.creditCard.copy(customerId = customer.id))
      op ← OrderPayments.saveNew(Factories.orderPayment.copy(orderId = order.id, paymentMethodId = cc.id))
      ccc ← CreditCardCharges.saveNew(Factories.creditCardCharge.copy(creditCardId = cc.id, orderPaymentId = op.id))
    } yield (cc, op, ccc)).run().futureValue
  }
}

