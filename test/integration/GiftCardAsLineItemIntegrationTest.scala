import Extensions._
import akka.http.scaladsl.model.StatusCodes
import util._
import failures.CartFailures.OrderAlreadyPlaced
import failures.GiftCardFailures.GiftCardMustBeCart
import failures.{GeneralFailure, NotFoundFailure404}
import models.cord._
import models.cord.lineitems._
import models.customer.Customers
import models.payment.giftcard._
import payloads.LineItemPayloads.AddGiftCardLineItem
import responses.cord.CartResponse
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Money._
import utils.db._
import utils.seeds.Seeds.Factories

import scala.concurrent.ExecutionContext.Implicits.global

class GiftCardAsLineItemIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures {

  "POST /v1/orders/:refNum/gift-cards" - {
    "successfully creates new GC as line item" in new Fixture {
      val response =
        POST(s"v1/orders/${cart.refNum}/gift-cards", AddGiftCardLineItem(balance = 100))
      response.status must === (StatusCodes.OK)

      val root = response.ignoreFailuresAndGiveMe[CartResponse]
      root.lineItems.giftCards.size must === (2)

      val newGiftCard = root.lineItems.giftCards.tail.head
      newGiftCard.originalBalance must === (100)
      newGiftCard.currentBalance must === (100)
      newGiftCard.availableBalance must === (100)
      newGiftCard.state must === (GiftCard.Cart)
    }

    "fails to create new GC as line item for invalid order" in new Fixture {
      val response = POST(s"v1/orders/ABC-666/gift-cards", AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "ABC-666").description)
    }

    "fails to create new GC as line item if order has already been placed" in new Fixture
    with Order_Baked {
      val response =
        POST(s"v1/orders/${cart.refNum}/gift-cards", AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
    }

    "fails to create new GC with invalid balance" in new Fixture {
      val response =
        POST(s"v1/orders/${cart.refNum}/gift-cards", AddGiftCardLineItem(balance = -100))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          GeneralFailure("Balance got -100, expected more than 0").description)
    }
  }

  "PATCH /v1/orders/:refNum/gift-cards/:code" - {
    "successfully updates GC as line item" in new Fixture {
      val response = PATCH(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}",
                           AddGiftCardLineItem(balance = 555))

      response.status must === (StatusCodes.OK)
      val root = response.ignoreFailuresAndGiveMe[CartResponse]
      root.lineItems.giftCards.size must === (1)

      val newGiftCard = root.lineItems.giftCards.head
      newGiftCard.originalBalance must === (555)
      newGiftCard.currentBalance must === (555)
      newGiftCard.availableBalance must === (555)
      newGiftCard.state must === (GiftCard.Cart)
    }

    "fails to update new GC as line item for invalid order" in new Fixture {
      val response =
        PATCH(s"v1/orders/ABC-666/gift-cards/${giftCard.code}", AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "ABC-666").description)
    }

    "fails to update GC as line item for already placed order" in new Fixture with Order_Baked {
      val response = PATCH(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}",
                           AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
    }

    "fails to update GC as line item for GC not in Cart state" in new Fixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).run().futureValue
      val response = PATCH(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}",
                           AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardMustBeCart(giftCard.code).description)
    }

    "fails to update GC as line item for invalid GC" in new Fixture {
      val response =
        PATCH(s"v1/orders/${cart.refNum}/gift-cards/ABC-666", AddGiftCardLineItem(balance = 100))

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(GiftCard, "ABC-666").description)
    }

    "fails to update GC setting invalid balance" in new Fixture {
      val response = PATCH(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}",
                           AddGiftCardLineItem(balance = -100))

      response.status must === (StatusCodes.BadRequest)
      response.error must === (
          GeneralFailure("Balance got -100, expected more than 0").description)
    }
  }

  "DELETE /v1/orders/:refNum/gift-cards/:code" - {
    "successfully deletes GC as line item" in new Fixture {
      val response = DELETE(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}")

      response.status must === (StatusCodes.OK)
      val root = response.ignoreFailuresAndGiveMe[CartResponse]
      root.lineItems.giftCards.size must === (0)

      GiftCards.findByCode(giftCard.code).one.run().futureValue.isEmpty mustBe true
    }

    "fails to delete new GC as line item for invalid cart" in new Fixture {
      val response = DELETE(s"v1/orders/ABC-666/gift-cards/${giftCard.code}")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "ABC-666").description)
    }

    "fails to delete GC as line item for already placed order" in new Fixture with Order_Baked {
      val response = DELETE(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}")

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
    }

    "fails to delete GC as line item for GC not in Cart state" in new Fixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).run().futureValue
      val response = DELETE(s"v1/orders/${cart.refNum}/gift-cards/${giftCard.code}")

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardMustBeCart(giftCard.code).description)
    }

    "fails to delete GC as line item for invalid GC" in new Fixture {
      val response = DELETE(s"v1/orders/${cart.refNum}/gift-cards/ABC-666")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(GiftCard, "ABC-666").description)
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked {
    val (giftCard) = (for {
      gcOrigin ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = cart.refNum))
      giftCard ← * <~ GiftCards.create(
                    GiftCard.buildLineItem(balance = 150,
                                           originId = gcOrigin.id,
                                           currency = Currency.USD))
      lineItemGc ← * <~ OrderLineItemGiftCards.create(
                      OrderLineItemGiftCard(giftCardId = giftCard.id, cordRef = cart.refNum))
      lineItem ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(cart, lineItemGc))
    } yield giftCard).gimme
  }
}
