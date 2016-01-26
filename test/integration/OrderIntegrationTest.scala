import java.time.Instant

import Extensions._
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.testkit.TestActorRef
import models.Order._
import models.rules.QueryStatement
import models.{OrderAssignment, Address, Addresses, CreditCardCharge, CreditCardCharges, CreditCards, Customers,
Order, OrderAssignments, OrderLineItem, OrderLineItemSkus, OrderLineItems, OrderLockEvent,
OrderLockEvents, OrderPayments, OrderShippingAddresses, OrderShippingMethod, OrderShippingMethods, Orders, Regions,
Shipment, Shipments, ShippingMethods, Skus, StoreAdmin, StoreAdmins, OrderWatcher, OrderWatchers}
import org.json4s.jackson.JsonMethods._
import payloads.{Assignment, Watchers, UpdateLineItemsPayload, UpdateOrderPayload}
import responses.{FullOrder, StoreAdminResponse}
import services.CartFailures._
import services.orders.OrderTotaler
import services.{LockedFailure, NotFoundFailure404, NotLockedFailure, StateTransitionNotAllowed,
OrderAssigneeNotFound, OrderWatcherNotFound}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories
import utils.time._
import utils.{RemorseTimer, Tick}

import scala.concurrent.ExecutionContext.Implicits.global

class OrderIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth {

  def getUpdated(refNum: String) = db.run(Orders.findByRefNum(refNum).result.headOption).futureValue.value

  "GET /v1/orders/:refNum" - {
    "payment status" - {
      "does not display payment status if no cc" in new Fixture {
        pending
        Orders.findByRefNum(order.refNum).map(_.state).update(Order.ManualHold).run.futureValue

        val response = GET(s"v1/orders/${order.refNum}")
        response.status must === (StatusCodes.OK)
        val fullOrder = response.withResultTypeOf[FullOrder.Root].result
        fullOrder.paymentState must not be defined
        fullOrder.paymentMethods mustBe empty
      }

      "displays payment status if cc present" in new PaymentStatusFixture {
        pending
        Orders.findByRefNum(order.refNum).map(_.state).update(Order.ManualHold).run.futureValue
        CreditCardCharges.findById(ccc.id).extract.map(_.status).update(CreditCardCharge.Auth).run.futureValue

        val response = GET(s"v1/orders/${order.refNum}")
        response.status must === (StatusCodes.OK)
        val fullOrder = response.withResultTypeOf[FullOrder.Root].result

        fullOrder.paymentState.value must === (CreditCardCharge.Auth)
        // fullOrder.paymentMethods.head.value.status must === (CreditCardCharge.Auth)
      }

      "displays 'cart' payment status if order is cart and cc present" in new PaymentStatusFixture {
        pending
        Orders.findByRefNum(order.refNum).map(_.state).update(Order.Cart).run.futureValue
        CreditCardCharges.findById(ccc.id).extract.map(_.status).update(CreditCardCharge.Auth).run.futureValue

        val response = GET(s"v1/orders/${order.refNum}")
        response.status must === (StatusCodes.OK)
        val fullOrder = response.withResultTypeOf[FullOrder.Root].result

        fullOrder.paymentState.value must === (CreditCardCharge.Cart)
        // fullOrder.payment.value.status must === (CreditCardCharge.Auth)
      }
    }
  }

  // FIXME just fix me :(
  "POST /v1/orders/:refNum/line-items" - {
    val payload = Seq(UpdateLineItemsPayload("SKU-YAX", 2))

    "should successfully update line items" in new OrderShippingMethodFixture with ShippingAddressFixture with
      PaymentStatusFixture {
      val response = POST(s"v1/orders/${order.refNum}/line-items", payload)

      response.status must === (StatusCodes.OK)
      val root = response.ignoreFailuresAndGiveMe[FullOrder.Root]
      val skus = root.lineItems.skus
      skus must have size 2
      skus.map(_.sku).toSet must === (Set("SKU-YAX"))
      skus.map(_.quantity).toSet must === (Set(1))
    }

    "should run cart validator" in new Fixture {
      val response = POST(s"v1/orders/${order.refNum}/line-items", payload)

      response.status must === (StatusCodes.OK)
      val ref = order.refNum
      val expectedWarnings = List(EmptyCart(ref), NoShipAddress(ref), NoShipMethod(ref)).map(_.description)
      val responseWithValidation = response.withResultTypeOf[FullOrder.Root]
      responseWithValidation.alerts must not be defined
      responseWithValidation.warnings.value must contain theSameElementsAs expectedWarnings
    }

    "should respond with 404 if order is not found" in {
      val response = POST(s"v1/orders/NOPE/line-items", payload)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }
  }

  "PATCH /v1/orders/:refNum" - {

    "successfully" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(FraudHold))

      response.status must === (StatusCodes.OK)

      val responseOrder = response.as[FullOrder.Root]
      responseOrder.orderState must === (FraudHold)
    }

    "fails if transition to destination status is not allowed" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(Cart))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (StateTransitionNotAllowed(order.state, Cart, order.refNum).description)
    }

    "fails if transition from current status is not allowed" in {
      val order = Orders.create(Factories.order.copy(state = Canceled)).run().futureValue.rightVal

      val response = PATCH(
        s"v1/orders/${order.referenceNumber}",
        UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (StateTransitionNotAllowed(order.state, ManualHold, order.refNum).description)
    }

    "fails if the order is not found" in {
      Orders.create(Factories.order).run().futureValue.rightVal

      val response = PATCH(s"v1/orders/NOPE", UpdateOrderPayload(ManualHold))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
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
      responseOrder.orderState must === (Canceled)
      responseOrder.lineItems.head.status must === (OrderLineItem.Canceled)

      // Testing via DB as currently FullOrder returns 'order.status' as 'payment.status'
      // OrderPayments.findAllByOrderId(order.id).futureValue.head.status must === ("cancelAuth")
    }
    */
  }

  "POST /v1/orders/:refNum/increase-remorse-period" - {

    "successfully" in {
      val order = Orders.create(Factories.order.copy(state = Order.RemorseHold)).run().futureValue.rightVal
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")

      val result = response.as[FullOrder.Root]
      result.remorsePeriodEnd must ===(order.remorsePeriodEnd.map(_.plusMinutes(15)))
    }

    "only when in RemorseHold status" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal
      val response = POST(s"v1/orders/${order.referenceNumber}/increase-remorse-period")
      response.status must ===(StatusCodes.BadRequest)

      val newOrder = Orders.findById(order.id).extract.one.run().futureValue.value
      newOrder.remorsePeriodEnd must ===(order.remorsePeriodEnd)
    }
  }

  "POST /v1/orders/:refNum/lock" - {
    "successfully locks an order" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal
      StoreAdmins.create(Factories.storeAdmin).run().futureValue.rightVal

      val response = POST(s"v1/orders/${order.referenceNumber}/lock")
      response.status must === (StatusCodes.OK)

      val lockedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.head
      lockedOrder.isLocked must === (true)

      val locks = OrderLockEvents.findByOrder(order.id).result.run().futureValue
      locks.length must === (1)
      val lock = locks.head
      lock.lockedBy must === (1)
    }

    "refuses to lock an already locked order" in {
      val order = Orders.create(Factories.order.copy(isLocked = true)).run().futureValue.rightVal

      val response = POST(s"v1/orders/${order.referenceNumber}/lock")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (LockedFailure(Order, order.referenceNumber).description)
    }

    "avoids race condition" in {
      pending // FIXME when DbResultT gets `select for update` https://github.com/FoxComm/phoenix-scala/issues/587
      StoreAdmins.create(Factories.storeAdmin).run().futureValue.rightVal
      val order = Orders.create(Factories.order).run().futureValue.rightVal

      def request = POST(s"v1/orders/${order.referenceNumber}/lock")

      val responses = Seq(0, 1).par.map(_ ⇒ request)
      responses.map(_.status) must contain allOf(StatusCodes.OK, StatusCodes.BadRequest)
      OrderLockEvents.result.run().futureValue.length mustBe 1
    }
  }

  "POST /v1/orders/:refNum/unlock" - {
    "unlocks an order" in {
      StoreAdmins.create(Factories.storeAdmin).run().futureValue.rightVal
      val order = Orders.create(Factories.order).run().futureValue.rightVal

      val lock = POST(s"v1/orders/${order.referenceNumber}/lock")
      lock.status must === (StatusCodes.OK)

      val unlock = POST(s"v1/orders/${order.referenceNumber}/unlock")
      unlock.status must === (StatusCodes.OK)

      val unlockedOrder = Orders.findByRefNum(order.referenceNumber).result.run().futureValue.head
      unlockedOrder.isLocked must === (false)
    }

    "refuses to unlock an already unlocked order" in {
      val order = Orders.create(Factories.order).run().futureValue.rightVal
      val response = POST(s"v1/orders/${order.referenceNumber}/unlock")

      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotLockedFailure(Order, order.refNum).description)
    }

    "adjusts remorse period when order is unlocked" in new RemorseFixture {
      val timer = TestActorRef(new RemorseTimer())

      POST(s"v1/orders/$refNum/lock")

      (timer ? Tick).futureValue // Nothing should happen
      val order1 = getUpdated(refNum)
      order1.remorsePeriodEnd must ===(order.remorsePeriodEnd)
      order1.state must ===(Order.RemorseHold)

      Thread.sleep(3000)

      POST(s"v1/orders/$refNum/unlock")

      (timer ? Tick).futureValue

      val order2 = getUpdated(refNum)
      val newRemorseEnd = order2.remorsePeriodEnd.value

      originalRemorseEnd.durationUntil(newRemorseEnd).getSeconds mustBe >= (3L)
      order2.state must ===(Order.RemorseHold)
    }

    "uses most recent lock record" in new RemorseFixture {
      OrderLockEvents.create(
        OrderLockEvent(lockedBy = admin.id, orderId = order.id, lockedAt = Instant.now.plusMinutes(30))
      ).run().futureValue.rightVal

      POST(s"v1/orders/$refNum/lock")
      POST(s"v1/orders/$refNum/unlock")

      val newRemorseEnd = getUpdated(order.referenceNumber).remorsePeriodEnd.value
      originalRemorseEnd.durationUntil(newRemorseEnd).getMinutes mustBe 0
    }

    "adds 15 minutes to remorse if order lock event is absent" in new RemorseFixture {
      POST(s"v1/orders/$refNum/lock")
      db.run(OrderLockEvents.findById(1).delete).futureValue
      // Sanity check
      OrderLockEvents.latestLockByOrder(order.id).result.headOption.run().futureValue must ===(None)
      POST(s"v1/orders/$refNum/unlock")
      getUpdated(refNum).remorsePeriodEnd.value must ===(originalRemorseEnd.plusMinutes(15))
    }
  }

  "POST /v1/orders/:refNum/assignees" - {

    "can be assigned to order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.warnings mustBe empty

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "can be assigned to locked order" in {
      val (order, storeAdmin) = (for {
        customer   ← * <~ Customers.create(Factories.customer)
        order      ← * <~ Orders.create(Factories.order.copy(isLocked = true, customerId = customer.id))
        storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      } yield (order, storeAdmin)).runTxn().futureValue.rightVal
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "404 if order is not found" in new Fixture {
      val response = POST(s"v1/orders/NOPE/assignees", Assignment(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "warning if assignee is not found" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.assignees.map(_.assignee) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.warnings.value must === (List(NotFoundFailure404(StoreAdmin, 999).description))
    }

    "can be viewed with order" in new Fixture {
      val response1 = GET(s"v1/orders/${order.referenceNumber}")
      response1.status must === (StatusCodes.OK)
      val responseOrder1 = response1.withResultTypeOf[FullOrder.Root].result
      responseOrder1.assignees mustBe empty

      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      val response2 = GET(s"v1/orders/${order.referenceNumber}")
      response2.status must === (StatusCodes.OK)
      val responseOrder2 = response2.withResultTypeOf[FullOrder.Root].result
      responseOrder2.assignees must not be empty
      responseOrder2.assignees.map(_.assignee) mustBe Seq(StoreAdminResponse.build(storeAdmin))

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "do not create duplicate records" in new Fixture {
      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))
      POST(s"v1/orders/${order.referenceNumber}/assignees", Assignment(Seq(storeAdmin.id)))

      OrderAssignments.byOrder(order).result.run().futureValue.size mustBe 1
    }
  }

  "DELETE /v1/orders/:refNum/assignees/:assigneeId/delete" - {

    "can be unassigned from order" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/assignees/${storeAdmin.id}")
      response.status must === (StatusCodes.OK)

      val root = response.as[FullOrder.Root]
      root.assignees.filter(_.assignee.id == storeAdmin.id) mustBe empty

      OrderAssignments.byOrder(order).result.run().futureValue mustBe empty
    }

    "400 if assignee is not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/assignees/${secondAdmin.id}")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAssigneeNotFound(order.referenceNumber, secondAdmin.id).description)
    }

    "404 if order is not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/NOPE/assignees/${storeAdmin.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "404 if storeAdmin is not found" in new AssignmentFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/assignees/555")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 555).description)
    }
  }

  "POST /v1/orders/:refNum/watchers" - {

    "can be added to order" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/watchers", Watchers(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.warnings mustBe empty

      OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "can be added to locked order" in {
      val (order, storeAdmin) = (for {
        customer   ← * <~ Customers.create(Factories.customer)
        order      ← * <~ Orders.create(Factories.order.copy(isLocked = true, customerId = customer.id))
        storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      } yield (order, storeAdmin)).runTxn().futureValue.rightVal
      val response = POST(s"v1/orders/${order.referenceNumber}/watchers", Watchers(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.OK)

      OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "404 if order is not found" in new Fixture {
      val response = POST(s"v1/orders/NOPE/watchers", Watchers(Seq(storeAdmin.id)))
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "warning if watcher is not found" in new Fixture {
      val response = POST(s"v1/orders/${order.referenceNumber}/watchers", Watchers(Seq(1, 999)))
      response.status must === (StatusCodes.OK)

      val fullOrderWithWarnings = response.withResultTypeOf[FullOrder.Root]
      fullOrderWithWarnings.result.watchers.map(_.watcher) must === (Seq(StoreAdminResponse.build(storeAdmin)))
      fullOrderWithWarnings.warnings.value must === (List(NotFoundFailure404(StoreAdmin, 999).description))
    }

    "can be viewed with order" in new Fixture {
      val response1 = GET(s"v1/orders/${order.referenceNumber}")
      response1.status must === (StatusCodes.OK)
      val responseOrder1 = response1.withResultTypeOf[FullOrder.Root].result
      responseOrder1.watchers mustBe empty

      POST(s"v1/orders/${order.referenceNumber}/watchers", Watchers(Seq(storeAdmin.id)))
      val response2 = GET(s"v1/orders/${order.referenceNumber}")
      response2.status must === (StatusCodes.OK)
      val responseOrder2 = response2.withResultTypeOf[FullOrder.Root].result
      responseOrder2.watchers must not be empty
      responseOrder2.watchers.map(_.watcher) mustBe Seq(StoreAdminResponse.build(storeAdmin))

      OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
    }

    "do not create duplicate records" in new Fixture {
      POST(s"v1/orders/${order.referenceNumber}/watchers", Watchers(Seq(storeAdmin.id)))
      POST(s"v1/orders/${order.referenceNumber}/watchers", Watchers(Seq(storeAdmin.id)))

      OrderWatchers.byOrder(order).result.run().futureValue.size mustBe 1
    }
  }

  "DELETE /v1/orders/:refNum/watchers/:watcherId/delete" - {

    "can be removed from order watchers" in new WatcherFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/watchers/${storeAdmin.id}")
      response.status must === (StatusCodes.OK)

      val root = response.as[FullOrder.Root]
      root.assignees.filter(_.assignee.id == storeAdmin.id) mustBe empty

      OrderWatchers.byOrder(order).result.run().futureValue mustBe empty
    }

    "400 if watcher is not found" in new WatcherFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/watchers/${secondAdmin.id}")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderWatcherNotFound(order.referenceNumber, secondAdmin.id).description)
    }

    "404 if order is not found" in new WatcherFixture {
      val response = DELETE(s"v1/orders/NOPE/watchers/${storeAdmin.id}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "NOPE").description)
    }

    "404 if storeAdmin is not found" in new WatcherFixture {
      val response = DELETE(s"v1/orders/${order.referenceNumber}/watchers/555")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(StoreAdmin, 555).description)
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
        val newAddress = Addresses.create(address.copy(name = "New", isDefaultShipping = false)).run().futureValue.rightVal

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
        response.error must === (NotFoundFailure404(Address, 99).description)
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
        response.error must === (NotFoundFailure404(Address, 99).description)
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

    "full order returns updated shipping address" in new ShippingAddressFixture {
      val name = "Even newer name"
      val city = "Queen Max"
      val updateAddressPayload = payloads.UpdateAddressPayload(name = Some(name), city = Some(city))
      val addressUpdateResponse = PATCH(s"v1/orders/${order.referenceNumber}/shipping-address", updateAddressPayload)
      addressUpdateResponse.status must === (StatusCodes.OK)
      checkOrder(addressUpdateResponse.ignoreFailuresAndGiveMe[FullOrder.Root])

      val fullOrderResponse = GET(s"v1/orders/${order.referenceNumber}")
      fullOrderResponse.status must === (StatusCodes.OK)
      checkOrder(fullOrderResponse.withResultTypeOf[FullOrder.Root].result)

      def checkOrder(fullOrder: FullOrder.Root) = {
        val addr = fullOrder.shippingAddress.value
        addr.name must === (name)
        addr.city must === (city)
        addr.address1 must === (address.address1)
        addr.address2 must === (address.address2)
        val region = Regions.findOneById(address.regionId).run().futureValue.value
        addr.region must === (region)
        addr.zip must === (address.zip)
      }
    }
  }

  "DELETE /v1/orders/:refNum/shipping-address" - {
    "succeeds if an address exists" in new ShippingAddressFixture {
      val noShipAddressFailure = NoShipAddress(order.refNum).description

      //get order and make sure it has a shipping address
      val fullOrderResponse = GET(s"v1/orders/${order.referenceNumber}")
      fullOrderResponse.status must === (StatusCodes.OK)
      val fullOrder = fullOrderResponse.withResultTypeOf[FullOrder.Root].result
      fullOrder.shippingAddress mustBe defined

      //delete the shipping address
      val deleteResponse = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
      deleteResponse.status must === (StatusCodes.OK)

      //shipping address must not be defined
      val lessThanFullOrder = deleteResponse.withResultTypeOf[FullOrder.Root]
      lessThanFullOrder.result.shippingAddress must not be defined
      lessThanFullOrder.warnings.value must contain (noShipAddressFailure)

      //fails if the order does not have shipping address
      val deleteFailedResponse = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
      deleteFailedResponse.status must === (StatusCodes.BadRequest)
    }

    "fails if the order is not found" in new ShippingAddressFixture {
      val response = DELETE(s"v1/orders/ABC-123/shipping-address")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Order, "ABC-123").description)

      db.run(OrderShippingAddresses.length.result).futureValue must === (1)
    }

    "fails if the order is not in cart status" in new ShippingAddressFixture {
      Orders.update(order.copy(state = Order.FulfillmentStarted)).run().futureValue

      val response = DELETE(s"v1/orders/${order.referenceNumber}/shipping-address")
      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderMustBeCart(order.refNum).description)

      db.run(OrderShippingAddresses.length.result).futureValue must === (1)
    }
  }

  "PATCH /v1/orders/:refNum/shipping-method" - {
    "succeeds if the order meets the shipping restrictions" in new ShippingMethodFixture {
      val response = PATCH(s"v1/orders/${order.referenceNumber}/shipping-method",
        payloads.UpdateShippingMethod(shippingMethodId = lowShippingMethod.id))

      response.status must === (StatusCodes.OK)
      val fullOrder = response.ignoreFailuresAndGiveMe[FullOrder.Root]
      fullOrder.shippingMethod.value.name must === (lowShippingMethod.adminDisplayName)

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
      customer   ← * <~ Customers.create(Factories.customer)
      order      ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, state = Order.Cart))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).runTxn().futureValue.rightVal
  }

  trait AssignmentFixture extends Fixture {
    val (assignee, secondAdmin) = (for {
      assignee    ← * <~ OrderAssignments.create(OrderAssignment(orderId = order.id, assigneeId = storeAdmin.id))
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (assignee, secondAdmin)).runTxn().futureValue.rightVal
  }

  trait WatcherFixture extends Fixture {
    val (watcher, secondAdmin) = (for {
      watcher     ← * <~ OrderWatchers.create(OrderWatcher(orderId = order.id, watcherId = storeAdmin.id))
      secondAdmin ← * <~ StoreAdmins.create(Factories.storeAdmin)
    } yield (watcher, secondAdmin)).runTxn().futureValue.rightVal
  }

  trait AddressFixture extends Fixture {
    val address = Addresses.create(Factories.address.copy(customerId = customer.id)).run().futureValue.rightVal
  }

  trait ShippingAddressFixture extends AddressFixture {
    val (orderShippingAddress, newAddress) = (for {
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      newAddress ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, isDefaultShipping = false,
        name = "New Shipping", address1 = "29918 Kenloch Dr", city = "Farmington Hills", regionId = 4177))
    } yield(orderShippingAddress, newAddress)).runTxn().futureValue.rightVal
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

    val lowSm = Factories.shippingMethods.head.copy(adminDisplayName = "Low", conditions = Some(lowConditions))
    val highSm = Factories.shippingMethods.head.copy(adminDisplayName = "High", conditions = Some(highConditions))

    val (lowShippingMethod, inactiveShippingMethod, highShippingMethod) = (for {
      sku         ← * <~ Skus.create(Factories.skus.head.copy(price = 100))
      lineItemSku ← * <~ OrderLineItemSkus.safeFindBySkuId(sku.id).toXor
      lineItem    ← * <~ OrderLineItems.create(OrderLineItem(orderId = order.id, originId = lineItemSku.id,
        originType = OrderLineItem.SkuItem))

      lowShippingMethod      ← * <~ ShippingMethods.create(lowSm)
      inactiveShippingMethod ← * <~ ShippingMethods.create(lowShippingMethod.copy(isActive = false))
      highShippingMethod     ← * <~ ShippingMethods.create(highSm)

      _                      ← * <~ OrderTotaler.saveTotals(order)
    } yield (lowShippingMethod, inactiveShippingMethod, highShippingMethod)).runTxn().futureValue.rightVal
  }

  trait OrderShippingMethodFixture extends ShippingMethodFixture {
    val shipment = (for {
      orderShipMethod ← * <~ OrderShippingMethods.create(
        OrderShippingMethod(orderId = order.id, shippingMethodId = highShippingMethod.id))
      shipment ← * <~ Shipments.create(Shipment(orderId = order.id, orderShippingMethodId = Some(orderShipMethod.id)))
    } yield shipment).runTxn().futureValue
  }

  trait RemorseFixture {
    val (admin, order) = (for {
      admin ← * <~ StoreAdmins.create(Factories.storeAdmin)
      order ← * <~ Orders.create(Factories.order.copy(
        state = Order.RemorseHold,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).runTxn().futureValue.rightVal

    val refNum = order.referenceNumber
    val originalRemorseEnd = order.remorsePeriodEnd.value
  }

  trait PaymentStatusFixture extends Fixture {
    val (cc, op, ccc) = (for {
      cc  ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      op  ← * <~ OrderPayments.create(Factories.orderPayment.copy(orderId = order.id, paymentMethodId = cc.id))
      ccc ← * <~ CreditCardCharges.create(Factories.creditCardCharge.copy(creditCardId = cc.id, orderPaymentId = op.id))
    } yield (cc, op, ccc)).runTxn().futureValue.rightVal
  }
}
