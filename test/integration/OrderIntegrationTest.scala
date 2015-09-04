import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.testkit.TestActorRef

import models.Order._
import models._
import org.joda.time.Minutes.minutesBetween
import org.joda.time.{Minutes, DateTime}
import org.joda.time.Seconds.secondsBetween
import payloads.UpdateOrderPayload
import responses.{AdminNotes, FullOrder}
import services.OrderUpdater.NewRemorsePeriodEnd
import services.{NotFoundFailure, NoteManager}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.{RemorseTimer, Tick}
import models.OrderLockEvents.scope._

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
         | [ { "skuId": 1, "quantity": 1 },
         |   { "skuId": 5, "quantity": 2 } ]
       """.stripMargin)

    val order = parse(response.bodyText).extract[FullOrder.Root]
    order.lineItems.map(_.skuId).sortBy(identity) must === (List(1, 5, 5))
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

    "increases remorse period" in {
      val order = Orders.save(Factories.order.copy(status = Order.RemorseHold)).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")

      val result = parse(response.bodyText).extract[NewRemorsePeriodEnd]
      result.remorsePeriodEnd must ===(order.remorsePeriodEnd.map(_.plusMinutes(15)))
    }

    "increases remorse period only when in RemorseHold status" in {
      val order = Orders.save(Factories.order).run().futureValue
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")
      response.status must === (StatusCodes.BadRequest)

      val newOrder = Orders._findById(order.id).extract.result.headOption.run().futureValue.get
      newOrder.remorsePeriodEnd must ===(order.remorsePeriodEnd)
    }

    "locks an order" in {
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
      errors.head must === ("errors" → Seq("Order is locked"))
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

      secondsBetween(originalRemorseEnd, newRemorseEnd).getSeconds mustBe >=(3)
      order2.status must ===(Order.RemorseHold)
    }

    "use most recent lock record" in new RemorseFixture {
      OrderLockEvents.save(OrderLockEvent(id = 1, lockedBy = 1, orderId = 1, lockedAt = DateTime.now.minusMinutes(30)))

      POST(s"v1/orders/$refNum/lock")
      POST(s"v1/orders/$refNum/unlock")

      val newRemorseEnd = getUpdated(order.referenceNumber).remorsePeriodEnd.get
      minutesBetween(originalRemorseEnd, newRemorseEnd).getMinutes mustBe 0
    }

    "adds 15 minutes if order lock event is absent" in new RemorseFixture {
      POST(s"v1/orders/$refNum/lock")
      db.run(OrderLockEvents.deleteById(1)).futureValue
      // Sanity check
      OrderLockEvents.findByOrder(order).mostRecentLock.result.headOption.run().futureValue must ===(None)
      POST(s"v1/orders/$refNum/unlock")
      getUpdated(refNum).remorsePeriodEnd.get must ===(originalRemorseEnd.plusMinutes(15))
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

//    "are soft deleted" in {
//      val response = DELETE(s"v1/orders/${order.id}/notes/${note.id}")
//    }

    "can be listed" in new Fixture {
      List("abc", "123", "xyz").map { body ⇒
        NoteManager.createOrderNote(order, storeAdmin, payloads.CreateNote(body = body)).futureValue
      }

      val response = GET(s"v1/orders/${order.referenceNumber}/notes")
      response.status must === (StatusCodes.OK)

      val notes = parse(response.bodyText).extract[Seq[AdminNotes.Root]]

      notes must have size (3)
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
        shippingAddress.street1 must === (address.street1)
        shippingAddress.street2 must === (address.street2)
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
        name = "New Shipping", street1 = "29918 Kenloch Dr", city = "Farmington Hills", regionId = 4177))
    } yield(orderShippingAddress, newAddress)).run().futureValue
  }

  trait PaymentMethodsFixture extends AddressFixture {
  }

  trait RemorseFixture {
    val (admin, order) = (for {
      admin ← StoreAdmins.save(Factories.storeAdmin)
      order ← Orders.save(Factories.order.copy(
        status = Order.RemorseHold,
        remorsePeriodEnd = Some(DateTime.now.plusMinutes(30))))
    } yield (admin, order)).run().futureValue

    val refNum = order.referenceNumber
    val originalRemorseEnd = order.remorsePeriodEnd.get
  }
}

