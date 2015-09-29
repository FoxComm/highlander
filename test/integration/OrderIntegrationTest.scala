import java.time.Instant

import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.testkit.TestActorRef

import models._
import payloads.{Assignment, UpdateOrderPayload}
import responses.{StoreAdminResponse, FullOrderWithWarnings, AdminNotes, FullOrder}
import models.rules.QueryStatement
import services.LockAwareOrderUpdater.NewRemorsePeriodEnd
import services.{NotFoundFailure, NoteManager}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import Order._
import utils.{RemorseTimer, Tick}
import models.OrderLockEvents.scope._

import utils.time._

class OrderIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  import concurrent.ExecutionContext.Implicits.global

  import Extensions._
  import org.json4s.jackson.JsonMethods._

  type Errors = Map[String, Seq[String]]

  def getUpdated(refNum: String) = db.run(Orders.findByRefNum(refNum).result.headOption).futureValue.get

  "returns new items" in {
    pending
    val orderId = db.run(Orders.returningId += Order(id = 0, customerId = 1)).futureValue

    val response = POST(
      s"v1/orders/$orderId/line-items",
       """
         | [ { "sku": 1, "quantity": 1 },
         |   { "sku": 5, "quantity": 2 } ]
       """.stripMargin)

    val order = parse(response.bodyText).extract[FullOrder.Root]
    order.lineItems.map(_.sku).sortBy(identity) must === (List("1", "5", "5"))
  }

  "updates status" - {

    "successfully" in {
      val order = Orders.save(Factories.order).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(FraudHold))

      response.status must === (StatusCodes.OK)

      val responseOrder = parse(response.bodyText).extract[FullOrder.Root]
      responseOrder.orderStatus must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in {
      val order = Orders.save(Factories.order).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(Cart))

      response.status must === (StatusCodes.BadRequest)
      response.bodyText must include("errors")
    }

    "fails if transition from current status is not allowed" in {
      val order = Orders.save(Factories.order.copy(status = Canceled)).run().futureValue

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.BadRequest)
      response.bodyText must include("errors")
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

  "increases remorse period" - {

    "successfully" in {
      val order = Orders.save(Factories.order.copy(status = Order.RemorseHold)).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")

      val result = parse(response.bodyText).extract[NewRemorsePeriodEnd]
      result.remorsePeriodEnd must ===(order.remorsePeriodEnd.map(_.plusMinutes(15)))
    }

    "only when in RemorseHold status" in {
      val order = Orders.save(Factories.order).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")
      response.status must ===(StatusCodes.BadRequest)

      val newOrder = Orders._findById(order.id).extract.one.run().futureValue.get
      newOrder.remorsePeriodEnd must ===(order.remorsePeriodEnd)
    }
  }

  "locking" - {

    "successfully locks an order" in {
      val order = Orders.save(Factories.order).run().futureValue
      StoreAdmins.save(Factories.storeAdmin).run().futureValue

      val response = POST(s"v1/orders/${order.referenceNumber}/lock")
      response.status must === (StatusCodes.OK)

      val lockedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.head
      lockedOrder.locked must === (true)

      val locks = OrderLockEvents.findByOrder(order).result.run().futureValue
      locks.length must === (1)
      val lock = locks.head
      lock.lockedBy must === (1)
    }

    "refuses to lock an already locked order" in {
      val order = Orders.save(Factories.order.copy(locked = true)).run().futureValue

      val response = POST(s"v1/orders/${order.referenceNumber}/lock")
      response.status must === (StatusCodes.BadRequest)
      val errors = parse(response.bodyText).extract[Errors]
      errors.head must === ("errors" → Seq("Model is locked"))
    }

    "unlocks an order" in {
      StoreAdmins.save(Factories.storeAdmin).run().futureValue
      val order = Orders.save(Factories.order).run().futureValue

      POST(s"v1/orders/${order.referenceNumber}/lock")

      val response = POST(s"v1/orders/${order.referenceNumber}/unlock")
      response.status must === (StatusCodes.OK)

      val unlockedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.head
      unlockedOrder.locked must === (false)
    }

    "refuses to unlock an already unlocked order" in {
      val order = Orders.save(Factories.order).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/unlock")

      response.status must === (StatusCodes.BadRequest)
      val errors = parse(response.bodyText).extract[Errors]
      errors.head must === ("errors" → Seq("Order is not locked"))
    }

    "avoids race condition" in {
      StoreAdmins.save(Factories.storeAdmin).run().futureValue
      val order = Orders.save(Factories.order).run().futureValue

      def request = POST(s"v1/orders/${order.referenceNumber}/lock")

      val responses = Seq(0, 1).par.map(_ ⇒ request)
      responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
      OrderLockEvents.result.run().futureValue.length mustBe 1
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
      val newRemorseEnd = order2.remorsePeriodEnd.get

      originalRemorseEnd.durationUntil(newRemorseEnd).getSeconds mustBe >= (3L)
      order2.status must ===(Order.RemorseHold)
    }

    "uses most recent lock record" in new RemorseFixture {
      OrderLockEvents.save(OrderLockEvent(id = 1, lockedBy = 1, orderId = 1, lockedAt = Instant.now.minusMinutes(30)))

      POST(s"v1/orders/$refNum/lock")
      POST(s"v1/orders/$refNum/unlock")

      val newRemorseEnd = getUpdated(order.referenceNumber).remorsePeriodEnd.get
      originalRemorseEnd.durationUntil(newRemorseEnd).getMinutes mustBe 0
    }

    "adds 15 minutes to remorse if order lock event is absent" in new RemorseFixture {
      POST(s"v1/orders/$refNum/lock")
      db.run(OrderLockEvents.deleteById(1)).futureValue
      // Sanity check
      OrderLockEvents.findByOrder(order).mostRecentLock.result.headOption.run().futureValue must ===(None)
      POST(s"v1/orders/$refNum/unlock")
      getUpdated(refNum).remorsePeriodEnd.get must ===(originalRemorseEnd.plusMinutes(15))
    }
  }

  "assignees" - {

    "can be assigned to order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      response.status mustBe StatusCodes.OK

      val fullOrderWithWarnings = parse(response.bodyText).extract[FullOrderWithWarnings]
      fullOrderWithWarnings.order.assignees must not be empty
      fullOrderWithWarnings.order.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
      fullOrderWithWarnings.warnings mustBe empty
    }

    "can be assigned to locked order" in {
      val (order, storeAdmin) = (for {
        customer ← Customers.save(Factories.customer)
        order ← Orders.save(Factories.order.copy(locked = true, customerId = customer.id))
        storeAdmin ← StoreAdmins.save(authedStoreAdmin)
      } yield (order, storeAdmin)).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      response.status mustBe StatusCodes.OK
    }

    "404 if order is not found" in new Fixture {
      val response = POST(s"v1/orders/NOPE/assignees", Assignment(Seq(storeAdmin.id)))
      response.status mustBe StatusCodes.NotFound
    }

    "warning if assignee is not found" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(1, 999)))
      response.status mustBe StatusCodes.OK

      val fullOrderWithWarnings = parse(response.bodyText).extract[FullOrderWithWarnings]
      fullOrderWithWarnings.order.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))
      fullOrderWithWarnings.warnings mustBe Seq(NotFoundFailure("storeAdmin with id=999 not found"))
    }

    "can be viewed with order" in new Fixture {
      val response1 = GET(s"v1/orders/${order.referenceNumber}")
      response1.status mustBe StatusCodes.OK
      val responseOrder1 = parse(response1.bodyText).extract[FullOrder.Root]
      responseOrder1.assignees mustBe empty

      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      val response2 = GET(s"v1/orders/${order.referenceNumber}")
      response2.status mustBe StatusCodes.OK
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

      errors must === (Map("errors" -> Seq("holderName must not be empty", "cvv must match regular expression " +
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

      errors must === (Map("errors" -> Seq("Your card was declined.")))
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

  "notes" - {
    "can be created by an admin for an order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/notes",
        payloads.CreateNote(body = "Hello, FoxCommerce!"))

      response.status must === (StatusCodes.OK)

      val note = parse(response.bodyText).extract[AdminNotes.Root]

      note.body must === ("Hello, FoxCommerce!")
      note.author must === (AdminNotes.buildAuthor(storeAdmin))
    }

    "returns a validation error if failed to create" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/notes", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.BadRequest)
      response.bodyText must include ("errors")
    }

    "returns a 404 if the order is not found" in new Fixture {
      val response = POST(s"v1/orders/ABACADSF113/notes", payloads.CreateNote(body = ""))

      response.status must === (StatusCodes.NotFound)
      response.bodyText must be ('empty)
    }

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createOrderNote(order, storeAdmin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/orders/${order.referenceNumber}/notes")
      response.status must === (StatusCodes.OK)

      val notes = parse(response.bodyText).extract[Seq[AdminNotes.Root]]

      notes must have size 3
      notes.map(_.body).toSet must === (Set("abc", "123", "xyz"))
    }

    "can update the body text" in new Fixture {
      val rootNote = NoteManager.createOrderNote(order, storeAdmin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.get

      val response = PATCH(s"v1/orders/${order.referenceNumber}/notes/${rootNote.id}", payloads.UpdateNote(body = "donkey"))
      response.status must === (StatusCodes.OK)

      val note = parse(response.bodyText).extract[AdminNotes.Root]
      note.body must === ("donkey")
    }
  }

  "shipping addresses" - {

    "can soft delete note" in new Fixture {
      val note = NoteManager.createOrderNote(order, storeAdmin,
        payloads.CreateNote(body = "Hello, FoxCommerce!")).futureValue.get
      StoreAdmins.save(Factories.storeAdmin).run().futureValue

      val response = DELETE(s"v1/orders/${order.referenceNumber}/notes/${note.id}")
      response.status must ===(StatusCodes.NoContent)
      response.bodyText mustBe empty

      val updatedNote = db.run(Notes.findById(note.id)).futureValue.get
      updatedNote.deletedBy.get mustBe 1
      updatedNote.deletedAt.get.isBeforeNow mustBe true
    }

    "copying a shipping address from a customer's book" - {

      "succeeds if the address exists in their book" in new AddressFixture {
        val response = POST(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.CreateShippingAddress(addressId = Some(address.id)))

        response.status must ===(StatusCodes.OK)
        val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

        val shippingAddressMap = shippingAddress.toMap -- Seq("id", "orderId", "createdAt", "deletedAt", "updatedAt")
        val addressMap = address.toMap -- Seq("id", "customerId", "isDefaultShipping", "createdAt", "deletedAt",
          "deletedAt", "updatedAt")

        shippingAddressMap must ===(addressMap)
        shippingAddress.orderId must ===(order.id)
      }

      "removes an existing shipping address before copying new address" in new AddressFixture {
        val newAddress = Addresses.save(address.copy(name = "New", isDefaultShipping = false)).run().futureValue

        val fst :: snd :: Nil = List(address.id, newAddress.id).map { id ⇒
          POST(s"v1/orders/${order.referenceNumber}/shipping-address",
            payloads.CreateShippingAddress(addressId = Some(id)))
        }

        fst.status must === (StatusCodes.OK)
        snd.status must === (StatusCodes.OK)

        val (shippingAddress :: Nil) = OrderShippingAddresses.findByOrderId(order.id).result.run().futureValue.toList

        shippingAddress.name must === ("New")
      }

      "errors if the address does not exist" in new AddressFixture {
        val response = POST(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.CreateShippingAddress(addressId = Some(99)))

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (NotFoundFailure(Address, 99).description)
      }

      "fails if the order is not found" in new AddressFixture {
        val response = DELETE(s"v1/orders/ABC-123/shipping-address")
        response.status must === (StatusCodes.NotFound)

        db.run(OrderShippingAddresses.length.result).futureValue must === (0)
      }
    }

    "editing a shipping address by copying from a customer's address book" - {

      "succeeds when the address exists" in new ShippingAddressFixture {
        val response = PATCH(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.UpdateShippingAddress(addressId = Some(newAddress.id))
        )

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
        val response = PATCH(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.UpdateShippingAddress(addressId = Some(99)))

        response.status must === (StatusCodes.NotFound)
        parseErrors(response) must === (NotFoundFailure(Address, 99).description)
      }

      "does not change the current shipping address if the edit fails" in new ShippingAddressFixture {
        val response = PATCH(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.UpdateShippingAddress(addressId = Some(101))
        )

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

    "editing a shipping address by sending updated field information" - {

      "succeeds when a subset of the fields in the address change" in new ShippingAddressFixture {
        val updateAddressPayload = payloads.UpdateAddressPayload(name = Some("New name"), city = Some("Queen Anne"))
        val response = PATCH(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.UpdateShippingAddress(address = Some(updateAddressPayload))
        )

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
        val response = PATCH(
          s"v1/orders/${order.referenceNumber}/shipping-address",
          payloads.UpdateShippingAddress(address = Some(updateAddressPayload))
        )

        response.status must === (StatusCodes.OK)

        val addressBook = Addresses.findById(address.id).run().futureValue.get

        addressBook.name must === (address.name)
        addressBook.city must === (address.city)
      }

    }

    "deleting the shipping address from an order" - {
      "succeeds if an address exists" in new AddressFixture {
        val response = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
        response.status must === (StatusCodes.NoContent)
      }

      "fails if the order is not found" in new AddressFixture {
        val response = DELETE(s"v1/orders/ABC-123/shipping-address")
        response.status must === (StatusCodes.NotFound)
      }
    }
  }

  "shipping methods" - {

    "Evaluates shipping rule: order total is greater than $25" - {

      "Shipping method is returned when actual order total is greater than $25" in new ShippingMethodsFixture {
        val conditions = parse(
          """
            | {
            |   "comparison": "and",
            |   "conditions": [{
            |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
            |   }]
            | }
          """.stripMargin).extract[QueryStatement]

        val action = models.ShippingMethods.save(Factories.shippingMethods.head.copy(
          conditions = Some(conditions)))
        val shippingMethod = db.run(action).futureValue

        val response = GET(s"v1/orders/${order.referenceNumber}/shipping-methods")
        response.status must === (StatusCodes.OK)

        val methodResponse = parse(response.bodyText).extract[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
      }

    }

    "Evaluates shipping rule: order total is greater than $100" - {

      "No shipping rules found when order total is less than $100" in new ShippingMethodsFixture {
        val conditions = parse(
          """
            | {
            |   "comparison": "and",
            |   "conditions": [{
            |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 100
            |   }]
            | }
          """.stripMargin).extract[QueryStatement]

        val action = models.ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
        val shippingMethod = db.run(action).futureValue

        val response = GET(s"v1/orders/${order.referenceNumber}/shipping-methods")
        response.status must === (StatusCodes.OK)

        val methodResponse = parse(response.bodyText).extract[Seq[responses.ShippingMethods.Root]]
        methodResponse mustBe 'empty
      }

    }

    "Evaluates shipping rule: shipping to CA, OR, or WA" - {

      "Shipping method is returned when the order is shipped to CA" in new WestCoastShippingMethodsFixture {
        val response = GET(s"v1/orders/${order.referenceNumber}/shipping-methods")
        response.status must === (StatusCodes.OK)

        val methodResponse = parse(response.bodyText).extract[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
      }
    }

    "Evaluates shipping rule: order total is between $10 and $100, and is shipped to CA, OR, or WA" - {

      "Is true when the order total is $27 and shipped to CA" in new ShippingMethodsStateAndPriceCondition {
        val response = GET(s"v1/orders/${order.referenceNumber}/shipping-methods")
        response.status must === (StatusCodes.OK)

        val methodResponse = parse(response.bodyText).extract[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
      }

    }

    "Evaluates shipping rule: ships to CA but has a restriction for hazardous items" - {

      "Shipping method is returned when the order has no hazardous SKUs" in new ShipToCaliforniaButNotHazardous {
        val response = GET(s"v1/orders/${order.referenceNumber}/shipping-methods")
        response.status must === (StatusCodes.OK)

        val methodResponse = parse(response.bodyText).extract[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
        methodResponse.isEnabled must === (true)
      }

      "Shipping method is returned, but disabled with a hazardous SKU" in new ShipToCaliforniaButNotHazardous {
        (for {
          hazSku ← Skus.save(Sku(sku = "HAZ-SKU", name = Some("fox"), price = 56, isHazardous = true))
          lineItem ← OrderLineItems.save(OrderLineItem(orderId = order.id, skuId = hazSku.id))
        } yield lineItem).run().futureValue

        val response = GET(s"v1/orders/${order.referenceNumber}/shipping-methods")
        response.status must === (StatusCodes.OK)

        val methodResponse = parse(response.bodyText).extract[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
        methodResponse.isEnabled must === (false)
      }

    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      storeAdmin ← StoreAdmins.save(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).run().futureValue
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.save(Factories.address.copy(customerId = customer.id)).run().futureValue
  }

  trait ShippingAddressFixture extends AddressFixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      newAddress ← Addresses.save(Factories.address.copy(customerId = customer.id, isDefaultShipping = false,
        name = "New Shipping", address1 = "29918 Kenloch Dr", city = "Farmington Hills", regionId = 4177))
    } yield(orderShippingAddress, newAddress)).run().futureValue
  }

  trait PaymentMethodsFixture extends AddressFixture {
  }

  trait ShippingMethodsFixture extends Fixture {
    val californiaId = 4129
    val michiganId = 4148
    val oregonId = 4164
    val washingtonId = 4177

    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      sku ← Skus.save(Factories.skus.head.copy(name = Some("Donkey"), price = 27))
      lineItems ← OrderLineItems.save(OrderLineItem(orderId = order.id, skuId = sku.id))
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait WestCoastShippingMethodsFixture extends ShippingMethodsFixture {
    val conditions = parse(
      s"""
        | {
        |   "comparison": "or",
        |   "conditions": [
        |     {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": ${californiaId}
        |     }, {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": ${oregonId}
        |     }, {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": ${washingtonId}
        |     }
        |   ]
        | }
        """.stripMargin).extract[QueryStatement]

    val action = models.ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue
  }

  trait ShippingMethodsStateAndPriceCondition extends ShippingMethodsFixture {
    val conditions = parse(
      s"""
        | {
        |   "comparison": "and",
        |   "statements": [
        |     {
        |       "comparison": "or",
        |       "conditions": [
        |         {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": ${californiaId}
        |         }, {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": ${oregonId}
        |         }, {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": ${washingtonId}
        |         }
        |       ]
        |     }, {
        |       "comparison": "and",
        |       "conditions": [
        |         {
        |           "rootObject": "Order",
        |           "field": "grandtotal",
        |           "operator": "greaterThanOrEquals",
        |           "valInt": 10
        |         }, {
        |           "rootObject": "Order",
        |           "field": "grandtotal",
        |           "operator": "lessThan",
        |           "valInt": 100
        |         }
        |       ]
        |     }
        |   ]
        | }
      """.stripMargin).extract[QueryStatement]

    val action = models.ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue
  }

  trait ShipToCaliforniaButNotHazardous extends ShippingMethodsFixture {
    val conditions = parse(
      s"""
         |{
         |  "comparison": "and",
         |  "conditions": [
         |    {
         |      "rootObject": "ShippingAddress",
         |      "field": "regionId",
         |      "operator": "equals",
         |      "valInt": ${californiaId}
          |    }
          |  ]
          |}
       """.stripMargin).extract[QueryStatement]

    val restrictions = parse(
      """
        | {
        |   "comparison": "and",
        |   "conditions": [
        |     {
        |       "rootObject": "Order",
        |       "field": "skus.isHazardous",
        |       "operator": "equals",
        |       "valBoolean": true
        |     }
        |   ]
        | }
      """.stripMargin).extract[QueryStatement]

    val shippingMethod = (for {
      shippingMethod ← models.ShippingMethods.save(Factories.shippingMethods.head.copy(
        conditions = Some(conditions), restrictions = Some(restrictions)))
    } yield shippingMethod).run().futureValue
  }

  trait RemorseFixture {
    val (admin, order) = (for {
      admin ← StoreAdmins.save(Factories.storeAdmin)
      order ← Orders.save(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).run().futureValue

    val refNum = order.referenceNumber
    val originalRemorseEnd = order.remorsePeriodEnd.get
  }
}

