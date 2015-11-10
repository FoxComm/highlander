package services

import java.time.Instant

import models.{GiftCardAdjustment, GiftCardAdjustments, GiftCardManuals, StoreCreditAdjustment,
StoreCreditAdjustments, Reasons, OrderPayments, OrderLineItem, OrderLineItems, OrderLineItemGiftCard,
OrderLineItemGiftCards, GiftCard, GiftCards, GiftCardOrder, GiftCardOrders, Customers, Orders, Order, StoreAdmins,
StoreAdmin, StoreCredits, StoreCreditManuals}
import models.Orders.scope._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.CartFailures._
import util.IntegrationTestBase
import utils.Money.Currency
import utils.Seeds.Factories
import utils.Slick.implicits._
import cats.implicits._
import slick.driver.PostgresDriver.api._

class CheckoutTest
  extends IntegrationTestBase
  with MockitoSugar {
  import concurrent.ExecutionContext.Implicits.global

  type SCA = StoreCreditAdjustment

  def cartValidator(resp: CartValidatorResponse = CartValidatorResponse()): CartValidation = {
    val m = mock[CartValidation]
    when(m.validate).thenReturn(Result.good(resp))
    m
  }

  "Checkout" - {
    "fails if the order is not a cart" in new Fixture {
      val nonCart = cart.copy(status = Order.RemorseHold)
      val result = leftValue(Checkout(nonCart, CartValidator(nonCart)).checkout.futureValue)
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      result must === (OrderMustBeCart(nonCart.refNum).single)
      current.status must === (cart.status)
    }

    "fails if the cart validator fails" in {
      val failure = GeneralFailure("scalac").single
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate).thenReturn(Result.failures(failure))

      val result = leftValue(Checkout(Factories.cart, mockValidator).checkout.futureValue)
      result must === (failure)
    }

    "fails if the cart validator has warnings" in {
      val failure = GeneralFailure("scalac").single
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate).thenReturn(Result.good(CartValidatorResponse(warnings = failure.toList)))

      val result = leftValue(Checkout(Factories.cart, mockValidator).checkout.futureValue)
      result must === (failure)
    }

    "updates status to RemorseHold and touches placedAt" in new Fixture {
      val before = Instant.now
      val result = rightValue(Checkout(cart, cartValidator()).checkout.futureValue)
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      current.status must === (Order.RemorseHold)
      current.placedAt.value mustBe >= (before)
    }

    "creates new cart for user at the end" in new Fixture {
      val result = rightValue(Checkout(cart, cartValidator()).checkout.futureValue)
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val newCart = Orders.findByCustomerId(cart.customerId).cartOnly.one.run().futureValue.value

      newCart.id must !== (cart.id)
      newCart.status must === (Order.Cart)
      newCart.locked mustBe false
      newCart.placedAt mustBe 'empty
      newCart.remorsePeriodEnd mustBe 'empty
    }

    "sets all gift card line item purchases as GiftCard.OnHold" in new GCLineItemFixture {
      val result = rightValue(Checkout(cart, cartValidator()).checkout.futureValue)
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val gc = GiftCards.findById(giftCard.id).extract.one.run().futureValue.value

      gc.status must ===(GiftCard.OnHold)
    }

    "authorizes payments" - {
      "for all gift cards" in new PaymentFixture {
        val (ids, _) = (for {
          origin ← GiftCardManuals.saveNew(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
          ids ← GiftCards.returningId ++= (1 to 3).map { _ ⇒
            Factories.giftCard.copy(originalBalance = 25, originId = origin.id)
          }
          pmts ← OrderPayments.returningId ++= ids.map { id ⇒
            Factories.giftCardPayment.copy(orderId = cart.id, paymentMethodId = id, amount = 25.some)
          }
        } yield (ids, pmts)).run().futureValue

        val result = rightValue(Checkout(cart, cartValidator()).checkout.futureValue)
        val current = Orders.findById(cart.id).extract.one.run().futureValue.value
        val adjustments = GiftCardAdjustments.filter(_.giftCardId.inSet(ids)).result.run().futureValue

        import GiftCardAdjustment._

        adjustments.map(_.status).toSet must === (Set[Status](Auth))
        adjustments.map(_.debit) must === (List(25, 25, 25))
      }

      "for all store credits" in new PaymentFixture {
        val (ids, _) = (for {
          origin ← StoreCreditManuals.saveNew(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
          ids ← StoreCredits.returningId ++= (1 to 3).map { _ ⇒
            Factories.storeCredit.copy(originalBalance = 25, originId = origin.id)
          }
          pmts ← OrderPayments.returningId ++= ids.map { id ⇒
            Factories.storeCreditPayment.copy(orderId = cart.id, paymentMethodId = id, amount = 25.some)
          }
        } yield (ids, pmts)).run().futureValue

        val result = rightValue(Checkout(cart, cartValidator()).checkout.futureValue)
        val current = Orders.findById(cart.id).extract.one.run().futureValue.value
        val adjustments = StoreCreditAdjustments.filter(_.storeCreditId.inSet(ids)).result.run().futureValue

        import StoreCreditAdjustment._

        adjustments.map(_.status).toSet must === (Set[Status](Auth))
        adjustments.map(_.debit) must === (List(25, 25, 25))
      }
    }
  }

  trait Fixture {
    val cart = Orders.saveNew(Factories.cart).run().futureValue
  }

  trait GCLineItemFixture {
    val (customer, cart, giftCard) = (for {
      customer ← Customers.saveNew(Factories.customer)
      cart ← Orders.saveNew(Factories.cart.copy(customerId = customer.id))
      origin ← GiftCardOrders.saveNew(GiftCardOrder(orderId = cart.id))
      giftCard ← GiftCards.saveNew(GiftCard.buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      lineItemGc ← OrderLineItemGiftCards.saveNew(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = cart.id))
      lineItem ← OrderLineItems.saveNew(OrderLineItem.buildGiftCard(cart, lineItemGc))
    } yield (customer, cart, giftCard)).run().futureValue
  }

  trait PaymentFixture {
    val (admin, customer, cart, reason) = (for {
      admin ← StoreAdmins.saveNew(Factories.storeAdmin)
      customer ← Customers.saveNew(Factories.customer)
      cart ← Orders.saveNew(Factories.cart.copy(customerId = customer.id))
      reason ← Reasons.saveNew(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, cart, reason)).run().futureValue
  }
}
