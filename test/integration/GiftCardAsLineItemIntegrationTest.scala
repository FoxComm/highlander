import akka.http.scaladsl.model.StatusCodes
import models._
import responses._
import org.scalatest.BeforeAndAfterEach
import services._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Money._
import utils.Seeds.Factories
import utils.Slick.implicits._

class GiftCardAsLineItemIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with BeforeAndAfterEach {

  import concurrent.ExecutionContext.Implicits.global
  import Extensions._
  import org.json4s.jackson.JsonMethods._

  "POST /v1/orders/:refNum/gift-cards" - {
    "successuflly creates new GC as line item" in new LineItemFixture {
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = 100))
      val root = response.as[FullOrder.Root]

      response.status must ===(StatusCodes.OK)
      root.lineItems.giftCards.size must === (2)

      val newGiftCard = root.lineItems.giftCards.tail.head
      newGiftCard.originalBalance must === (100)
      newGiftCard.currentBalance must === (100)
      newGiftCard.availableBalance must === (100)
      newGiftCard.status must === (GiftCard.Cart)
    }

    "fails to create new GC as line item for invalid order" in new LineItemFixture {
      val response = POST(s"v1/orders/ABC-666/gift-cards", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure404(Order, "ABC-666").description)
    }

    "fails to create new GC as line item if no cart order is present" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure404(Order, order.refNum).description)
    }

    "fails to create new GC with invalid balance" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = POST(s"v1/orders/${order.refNum}/gift-cards", payloads.AddGiftCardLineItem(balance = -100))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance got -100, expected more than 0").description)
    }
  }

  "PATCH /v1/orders/:refNum/gift-cards/:code" - {
    "successuflly updates GC as line item" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 555))
      val root = response.as[FullOrder.Root]

      response.status must ===(StatusCodes.OK)
      root.lineItems.giftCards.size must === (1)

      val newGiftCard = root.lineItems.giftCards.head
      newGiftCard.originalBalance must === (555)
      newGiftCard.currentBalance must === (555)
      newGiftCard.availableBalance must === (555)
      newGiftCard.status must === (GiftCard.Cart)
    }

    "fails to update new GC as line item for invalid order" in new LineItemFixture {
      val response = PATCH(s"v1/orders/ABC-666/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure404(Order, "ABC-666").description)
    }

    "fails to update GC as line item for order not in Cart state" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure404(Order, order.refNum).description)
    }

    "fails to update GC as line item for GC not in Cart state" in new LineItemFixture {
      GiftCards.findByCode(giftCard.code).map(_.status).update(GiftCard.Canceled).run().futureValue
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      // TODO: proper error message
      response.errors must ===(GeneralFailure("Not found").description)
    }

    "fails to update GC as line item for invalid GC" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/ABC-666", payloads.AddGiftCardLineItem(balance = 100))

      response.status must ===(StatusCodes.NotFound)
      // TODO: proper error message
      response.errors must ===(GeneralFailure("Not found").description)
    }

    "fails to update GC setting invalid balance" in new LineItemFixture {
      val response = PATCH(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}", payloads.AddGiftCardLineItem(balance = -100))

      response.status must ===(StatusCodes.BadRequest)
      response.errors must ===(GeneralFailure("Balance got -100, expected more than 0").description)
    }
  }

  "DELETE /v1/orders/:refNum/gift-cards/:code" - {
    "successuflly deletes GC as line item" in new LineItemFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")
      val root = response.as[FullOrder.Root]

      response.status must ===(StatusCodes.OK)
      root.lineItems.giftCards.size must === (0)

      GiftCards.findByCode(giftCard.code).one.run().futureValue.isEmpty mustBe true
    }

    "fails to delete new GC as line item for invalid order" in new LineItemFixture {
      val response = DELETE(s"v1/orders/ABC-666/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure404(Order, "ABC-666").description)
    }

    "fails to delete GC as line item for order not in Cart state" in new LineItemFixture {
      Orders.findActiveOrderByCustomer(customer).map(_.status).update(Order.ManualHold).run().futureValue
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.NotFound)
      response.errors must ===(NotFoundFailure404(Order, order.refNum).description)
    }

    "fails to delete GC as line item for GC not in Cart state" in new LineItemFixture {
      GiftCards.findByCode(giftCard.code).map(_.status).update(GiftCard.Canceled).run().futureValue
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/${giftCard.code}")

      response.status must ===(StatusCodes.NotFound)
      // TODO: proper error message
      response.errors must ===(GeneralFailure("Not found").description)
    }

    "fails to delete GC as line item for invalid GC" in new LineItemFixture {
      val response = DELETE(s"v1/orders/${order.refNum}/gift-cards/ABC-666")

      response.status must ===(StatusCodes.NotFound)
      // TODO: proper error message
      response.errors must ===(GeneralFailure("Not found").description)
    }
  }

  trait LineItemFixture {
    val (customer, order, giftCard) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id, status = Order.Cart))

      gcOrigin ← GiftCardOrders.save(GiftCardOrder(orderId = order.id))
      giftCard ← GiftCards.save(GiftCard.buildLineItem(balance = 150, originId = gcOrigin.id, currency = Currency.USD))
      lineItemGc ← OrderLineItemGiftCards.save(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))
      lineItem ← OrderLineItems.save(OrderLineItem.buildGiftCard(order, lineItemGc))
    } yield (customer, order, giftCard)).run().futureValue
  }
}
