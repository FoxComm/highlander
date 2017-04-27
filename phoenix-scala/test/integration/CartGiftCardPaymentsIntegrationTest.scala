import cats.implicits._
import failures.CartFailures.OrderAlreadyPlaced
import failures.GiftCardFailures._
import failures.OrderFailures.OrderPaymentNotFoundFailure
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.Reasons
import models.cord._
import models.payment.giftcard._
import payloads.PaymentPayloads.GiftCardPayment
import slick.driver.PostgresDriver.api._
import testutils._
import utils.db._
import utils.seeds.Factories

class CartGiftCardPaymentsIntegrationTest extends CartPaymentsIntegrationTestBase {

  "POST /v1/carts/:ref/payment-methods/gift-cards" - {
    "succeeds" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      cartsApi(cart.refNum).payments.giftCard.add(payload).mustBeOk()

      val payments = giftCardPayments(cart)
      payments must have size 1
      payments.head.amount must === (payload.amount)
    }

    "fails when adding same gift card twice" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)

      cartsApi(cart.refNum).payments.giftCard.add(payload).mustBeOk()

      cartsApi(cart.refNum).payments.giftCard
        .add(payload)
        .mustFailWith400(GiftCardPaymentAlreadyAdded(cart.referenceNumber, giftCard.code))
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      cartsApi("NOPE").payments.giftCard
        .add(GiftCardPayment(code = "foo", amount = 1.some))
        .mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      cartsApi(cart.refNum).payments.giftCard
        .add(GiftCardPayment(code = "NOPE", amount = 1.some))
        .mustFailWith400(NotFoundFailure404(GiftCard, "NOPE"))

      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard does not have sufficient available balance" in new CartWithGcFixture {
      val requestedAmount = giftCard.availableBalance + 1

      cartsApi(cart.refNum).payments.giftCard
        .add(GiftCardPayment(code = giftCard.code, amount = requestedAmount.some))
        .mustFailWith400(GiftCardNotEnoughBalance(giftCard, requestedAmount))

      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the order has already been placed" in new GiftCardFixture with Order_Baked {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      cartsApi(cart.refNum).payments.giftCard
        .add(payload)
        .mustFailWith400(OrderAlreadyPlaced(cart.refNum))

      giftCardPayments(cart) must have size 0
    }

    "fails if the giftCard is inactive" in new CartWithGcFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Canceled).gimme
      val payload = GiftCardPayment(code = giftCard.code, amount = 1.some)
      cartsApi(cart.refNum).payments.giftCard
        .add(payload)
        .mustFailWith400(GiftCardIsInactive(giftCard))

      giftCardPayments(cart) mustBe 'empty
    }

    "fails to add GC with cart status as payment method" in new CartWithGcFixture {
      GiftCards.findByCode(giftCard.code).map(_.state).update(GiftCard.Cart).gimme
      val payload = GiftCardPayment(code = giftCard.code, amount = Some(15))
      cartsApi(cart.refNum).payments.giftCard
        .add(payload)
        .mustFailWith400(GiftCardMustNotBeCart(giftCard.code))
    }
  }

  "PATCH /v1/carts/:ref/payment-methods/gift-cards" - {
    "successfully updates giftCard payment" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      cartsApi(cart.refNum).payments.giftCard.add(payload).mustBeOk()
      cartsApi(cart.refNum).payments.giftCard.update(payload.copy(amount = Some(10))).mustBeOk()
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      cartsApi("NOPE").payments.giftCard
        .update(payload)
        .mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      giftCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = "NOPE", amount = 1.some)
      cartsApi(cart.refNum).payments.giftCard
        .update(payload)
        .mustFailWith400(NotFoundFailure400(GiftCard, "NOPE"))

      giftCardPayments(cart) mustBe 'empty
    }
  }

  "DELETE /v1/carts/:ref/payment-methods/gift-cards/:code" - {
    "successfully deletes a giftCard" in new CartWithGcFixture {
      val payload = GiftCardPayment(code = giftCard.code, amount = giftCard.availableBalance.some)
      cartsApi(cart.refNum).payments.giftCard.add(payload).mustBeOk()
      cartsApi(cart.refNum).payments.giftCard.delete(giftCard.code).mustBeOk()

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the cart is not found" in new CartWithGcFixture {
      cartsApi("NOPE").payments.giftCard
        .delete(giftCard.code)
        .mustFailWith404(NotFoundFailure404(Cart, "NOPE"))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard is not found" in new CartWithGcFixture {
      cartsApi(cart.refNum).payments.giftCard
        .delete("abc-123")
        .mustFailWith404(NotFoundFailure404(GiftCard, "abc-123"))

      creditCardPayments(cart) mustBe 'empty
    }

    "fails if the giftCard orderPayment is not found" in new CartWithGcFixture {
      cartsApi(cart.refNum).payments.giftCard
        .delete(giftCard.code)
        .mustFailWith400(OrderPaymentNotFoundFailure(GiftCard))

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
