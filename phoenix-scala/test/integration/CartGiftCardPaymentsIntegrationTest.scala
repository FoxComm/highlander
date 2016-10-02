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
      val response = cartsApi(cart.refNum).payments.giftCard.add(payload)

      response.status must === (StatusCodes.OK)

      val payments = giftCardPayments(cart)
      payments must have size 1
      payments.head.amount must === (payload.amount)
    }

    "fails when adding same gift card twice" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = cartsApi(cart.refNum).payments.giftCard.add(payload)
      response.status must === (StatusCodes.OK)

      val secondResponse = cartsApi(cart.refNum).payments.giftCard.add(payload)
      secondResponse.status must === (StatusCodes.BadRequest)
      secondResponse.error must === (
          GiftCardPaymentAlreadyAdded(cart.referenceNumber, giftCard.code).description)
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = cartsApi("NOPE").payments.giftCard.add(payload)

      response.status must === (StatusCodes.NotFound)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val payload =
        GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance.some)
      val response = cartsApi(cart.refNum).payments.giftCard.add(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard does not have sufficient available balance" in new CartWithGcFixture {
      val requestedAmount = giftCard.availableBalance + 1
      val payload         = GiftCardPayment(code = giftCard.code, amount = requestedAmount.some)
      val response        = cartsApi(cart.refNum).payments.giftCard.add(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardNotEnoughBalance(giftCard, requestedAmount).description)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new GiftCardFixture with Order_Baked {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = cartsApi(cart.refNum).payments.giftCard.add(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderAlreadyPlaced(cart.refNum).description)
      giftCardPayments(cart) must have size 0
    }

    "fails if the giftCard is inactive" in new CartWithGcFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).gimme
      val payload =
        GiftCardPayment(code = giftCard.code, amount = (giftCard.availableBalance + 1).some)
      val response = cartsApi(cart.refNum).payments.giftCard.add(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardIsInactive(giftCard).description)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails to add GC with cart status as payment method" in new CartWithGcFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Cart).gimme
      val payload  = GiftCardPayment(code = giftCard.code, amount = Some(15))
      val response = cartsApi(cart.refNum).payments.giftCard.add(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (GiftCardMustNotBeCart(giftCard.code).description)
    }
  }

  "PATCH /v1/orders/:ref/payment-methods/gift-cards" - {
    "successfully updates giftCard payment" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val create  = cartsApi(cart.refNum).payments.giftCard.add(payload)
      create.status must === (StatusCodes.OK)

      val update = cartsApi(cart.refNum).payments.giftCard.update(payload.copy(amount = Some(10)))
      update.status must === (StatusCodes.OK)
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val payload  = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val response = cartsApi("NOPE").payments.giftCard.update(payload)

      response.status must === (StatusCodes.NotFound)
      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val payload =
        GiftCardPayment(code = giftCard.code ++ "xyz", amount = giftCard.availableBalance.some)
      val response = cartsApi(cart.refNum).payments.giftCard.update(payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (NotFoundFailure404(GiftCard, payload.code).description)
      giftCardPayments(cart) mustBe 'empty
    }
  }

  "DELETE /v1/orders/:ref/payment-methods/gift-cards/:code" - {
    "successfully deletes a giftCard" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      val create  = cartsApi(cart.refNum).payments.giftCard.add(payload)
      create.status must === (StatusCodes.OK)

      val response = cartsApi(cart.refNum).payments.giftCard.delete(giftCard.code)
      response.status must === (StatusCodes.OK)

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val response = cartsApi("NOPE").payments.giftCard.delete(giftCard.code)

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Cart, "NOPE").description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val response = cartsApi(cart.refNum).payments.giftCard.delete("abc-123")

      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(GiftCard, "abc-123").description)
      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard orderPayment is not found" in new CartWithGcFixture {
      val response = cartsApi(cart.refNum).payments.giftCard.delete(giftCard.code)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (OrderPaymentNotFoundFailure(GiftCard).description)
      creditCardPayments(cart) mustBe 'empty
    }
  }

  trait CartWithGcFixture extends Fixture with GiftCardFixture

  trait GiftCardFixture extends StoreAdmin_Seed {
    val giftCard = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.id))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
    } yield giftCard).gimme
  }

}
