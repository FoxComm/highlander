import akka.http.scaladsl.model.StatusCodes
import models.{Customers, GiftCard, GiftCardOrder, GiftCardOrders, GiftCards, Order, OrderLineItem,
OrderLineItemGiftCard, OrderLineItemGiftCards, OrderLineItems, Orders}
import org.scalatest.BeforeAndAfterEach
import responses.FullOrder
import services.CartFailures.OrderMustBeCart
import services.{GeneralFailure, GiftCardMustBeCart, NotFoundFailure404}
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money._
import utils.Slick.implicits._
import utils.seeds.Seeds
import utils.seeds.Seeds.Factories

class GiftCardAsLineItemIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import Extensions._

  import concurrent.ExecutionContext.Implicits.global

  "POST /v1/orders/:refNum/gift-cards" - {
    "successfully creates new GC as line item" in new LineItemFixture {
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = 100))
      response.status must ===(StatusCodes.OK)

      val root = response.ignoreFailuresAndGiveMe[FullOrder.Root]
      root.lineItems.giftCards.size must === (2)

      val newGiftCard = root.lineItems.giftCards.tail.head
      newGiftCard.originalBalance must === (100)
      newGiftCard.currentBalance must === (100)
      newGiftCard.availableBalance must === (100)
      newGiftCard.state must === (GiftCard.Cart)
    }

    "fails to create new GC as line item for invalid order" in new LineItemFixture {
      val response = POST(s"v1/orders/ABC-666/gift-cards", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Order, "ABC-666").description)
    }

    "fails to create new GC as line item if no cart order is present" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.state).update(Order.ManualHold).run().futureValue
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderMustBeCart(order.refNum).description)
    }

    "fails to create new GC with invalid balance" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.state).update(Order.ManualHold).run().futureValue
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = -100))

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===(GeneralFailure("Balance got -100, expected more than 0").description)
    }
  }

  "PATCH /v1/orders/:refNum/gift-cards/:code" - {
    "successfully updates GC as line item" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 555))

      response.status must ===(StatusCodes.OK)
      val root = response.ignoreFailuresAndGiveMe[FullOrder.Root]
      root.lineItems.giftCards.size must === (1)

      val newGiftCard = root.lineItems.giftCards.head
      newGiftCard.originalBalance must === (555)
      newGiftCard.currentBalance must === (555)
      newGiftCard.availableBalance must === (555)
      newGiftCard.state must === (GiftCard.Cart)
    }

    "fails to update new GC as line item for invalid order" in new LineItemFixture {
      val response = PATCH(s"v1/orders/ABC-666/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Order, "ABC-666").description)
    }

    "fails to update GC as line item for order not in Cart state" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.state).update(Order.ManualHold).run().futureValue
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===(OrderMustBeCart(order.refNum).description)
    }

    "fails to update GC as line item for GC not in Cart state" in new LineItemFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).run().futureValue
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===(GiftCardMustBeCart(giftCard.code).description)
    }

    "fails to update GC as line item for invalid GC" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/ABC-666", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(GiftCard, "ABC-666").description)
    }

    "fails to update GC setting invalid balance" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = -100))

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===(GeneralFailure("Balance got -100, expected more than 0").description)
    }
  }

  "DELETE /v1/orders/:refNum/gift-cards/:code" - {
    "successfully deletes GC as line item" in new LineItemFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.OK)
      val root = response.ignoreFailuresAndGiveMe[FullOrder.Root]
      root.lineItems.giftCards.size must === (0)

      GiftCards.findByCode(giftCard.code).one.run().futureValue.isEmpty mustBe true
    }

    "fails to delete new GC as line item for invalid order" in new LineItemFixture {
      val response = DELETE(s"v1/orders/ABC-666/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Order, "ABC-666").description)
    }

    "fails to delete GC as line item for order not in Cart state" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.state).update(Order.ManualHold).run().futureValue
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===(OrderMustBeCart(order.refNum).description)
    }

    "fails to delete GC as line item for GC not in Cart state" in new LineItemFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).run().futureValue
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.BadRequest)
      response.error must ===(GiftCardMustBeCart(giftCard.code).description)
    }

    "fails to delete GC as line item for invalid GC" in new LineItemFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/ABC-666")

      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(GiftCard, "ABC-666").description)
    }
  }

  trait LineItemFixture {
    val (customer, order, giftCard) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, state = Order.Cart))
      gcOrigin ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
      giftCard ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = 150, originId = gcOrigin.id, currency = Currency.USD))
      lineItemGc ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))
      lineItem ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, lineItemGc))
    } yield (customer, order, giftCard)).runTxn().futureValue.rightVal
  }
}
