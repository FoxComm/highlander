import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.CartFailures.OrderAlreadyPlaced
import failures.GiftCardFailures._
import failures.NotFoundFailure404
import failures.OrderFailures.OrderPaymentNotFoundFailure
import models.Reasons
import models.cord._
import models.payment.giftcard._
import payloads.PaymentPayloads.GiftCardPayment
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.seeds.Seeds.Factories

class CartGiftCardPaymentsIntegrationTest extends CartPaymentsIntegrationTestBase {

  "POST /v1/orders/:ref/payment-methods/gift-cards" - {
    "succeeds" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.OK)

      val payments = giftCardPayments(cart)
      payments must have size 1
      payments.head.amount must === (payload.amount)
    }

    "fails when adding same gift card twice" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
      response.status must === (StatusCodes.OK)

      val secondResponse =
        POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
      secondResponse.status must === (StatusCodes.BadRequest)
      secondResponse.error must === (
          GiftCardPaymentAlreadyAdded(cart.referenceNumber, giftCard.code).description)
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = POST(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.NotFound)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val payload =
        GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance.some)
      val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard does not have sufficient available balance" in new CartWithGcFixture {
      val requestedAmount = giftCard.availableBalance + 1
      val payload         = GiftCardPayment(code = giftCard.code, amount = requestedAmount.some)
      val response        = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardNotEnoughBalance(giftCard, requestedAmount).description)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new GiftCardFixture with Order_Baked {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
      giftCardPayments(cart) must have size 0
    }

    "fails if the giftCard is inactive" in new CartWithGcFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).gimme
      val payload =
        GiftCardPayment(code = giftCard.code, amount = (giftCard.availableBalance + 1).some)
      val response = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardIsInactive(giftCard).description)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails to add GC with cart status as payment method" in new CartWithGcFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Cart).gimme
      val payload  = GiftCardPayment(code = giftCard.code, amount = Some(15))
      val response = POST(s"v1/orders/${cart.refNum}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardMustNotBeCart(giftCard.code).description)
    }
  }

  "PATCH /v1/orders/:ref/payment-methods/gift-cards" - {
    "successfully updates giftCard payment" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val create  = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
      create.status must === (StatusCodes.OK)

      val update = PATCH(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards",
                         payload.copy(amount = Some(10)))
      update.status must === (StatusCodes.OK)
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = PATCH(s"v1/orders/ABC123/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.NotFound)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val payload =
        GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance.some)
      val response =
        PATCH(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
      giftCardPayments(cart) mustBe 'empty
    }
  }

  "DELETE /v1/orders/:ref/payment-methods/gift-cards/:code" - {
    "successfully deletes a giftCard" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val create  = POST(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards", payload)
      create.status must === (StatusCodes.OK)

      val response =
        DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")
      val payments = creditCardPayments(cart)

      response.status must === (StatusCodes.OK)
      payments mustBe 'empty
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val response = DELETE(s"v1/orders/99/payment-methods/gift-cards/123")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, 99).description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val response =
        DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards/abc-123")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(GiftCard, "abc-123").description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard orderPayment is not found" in new CartWithGcFixture {
      val response =
        DELETE(s"v1/orders/${cart.referenceNumber}/payment-methods/gift-cards/${giftCard.code}")

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderPaymentNotFoundFailure(GiftCard).description)
      creditCardPayments(cart) mustBe 'empty
    }
  }

  trait CartWithGcFixture extends Fixture with GiftCardFixture

  trait GiftCardFixture extends StoreAdmin_Seed {
    val giftCard = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }

}
