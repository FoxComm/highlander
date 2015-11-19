package services

import java.time.Instant

import cats.implicits._
import models.Orders.scope._
import models.{Customers, GiftCard, GiftCardAdjustment, GiftCardAdjustments, GiftCardManuals, GiftCardOrder,
GiftCardOrders, GiftCards, Order, OrderLineItem, OrderLineItemGiftCard, OrderLineItemGiftCards, OrderLineItems,
OrderPayments, Orders, Reasons, StoreAdmins, StoreCreditAdjustment, StoreCreditAdjustments, StoreCreditManuals,
StoreCredits}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.CartFailures._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT.implicits._
import utils.DbResultT._
import utils.{StripeApi, Apis}
import utils.Money.Currency
import utils.Seeds.Factories
import utils.Slick.DbResult
import utils.Slick.implicits._

class CheckoutTest
  extends IntegrationTestBase
  with MockitoSugar {
  import concurrent.ExecutionContext.Implicits.global

  implicit val apis: Apis = Apis(mock[StripeApi])

  def cartValidator(resp: CartValidatorResponse = CartValidatorResponse()): CartValidation = {
    val m = mock[CartValidation]
    when(m.validate).thenReturn(DbResult.good(resp))
    m
  }

  "Checkout" - {
    "fails if the order is not a cart" in new Fixture {
      val nonCart = cart.copy(status = Order.RemorseHold)
      val result = Checkout(nonCart, CartValidator(nonCart)).checkout.futureValue.leftVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      result must === (OrderMustBeCart(nonCart.refNum).single)
      current.status must === (cart.status)
    }

    "fails if the cart validator fails" in {
      val failure = GeneralFailure("scalac").single
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate).thenReturn(DbResult.failures(failure))

      val result = Checkout(Factories.cart, mockValidator).checkout.futureValue.leftVal
      result must === (failure)
    }

    "fails if the cart validator has warnings" in {
      val failure = GeneralFailure("scalac").single
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate).thenReturn(DbResult.good(CartValidatorResponse(warnings = failure.some)))

      val result = Checkout(Factories.cart, mockValidator).checkout.futureValue.leftVal
      result must === (failure)
    }

    "updates status to RemorseHold and touches placedAt" in new Fixture {
      val before = Instant.now
      val result = Checkout(cart, cartValidator()).checkout.futureValue.rightVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      current.status must === (Order.RemorseHold)
      current.placedAt.value mustBe >= (before)
    }

    "creates new cart for user at the end" in new Fixture {
      val result = Checkout(cart, cartValidator()).checkout.futureValue.rightVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val newCart = Orders.findByCustomerId(cart.customerId).cartOnly.one.run().futureValue.value

      newCart.id must !== (cart.id)
      newCart.status must === (Order.Cart)
      newCart.locked mustBe false
      newCart.placedAt mustBe 'empty
      newCart.remorsePeriodEnd mustBe 'empty
    }

    "sets all gift card line item purchases as GiftCard.OnHold" in new GCLineItemFixture {
      val result = Checkout(cart, cartValidator()).checkout.futureValue.rightVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val gc = GiftCards.findById(giftCard.id).extract.one.run().futureValue.value

      gc.status must ===(GiftCard.OnHold)
    }

    "authorizes payments" - {
      "for all gift cards" in new PaymentFixture {
        val ids = (for {
          origin ← * <~ GiftCardManuals.create(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
          ids    ← * <~ (GiftCards.returningId ++= (1 to 3).map { _ ⇒
            Factories.giftCard.copy(originalBalance = 25, originId = origin.id)
          })
          _      ← * <~ (OrderPayments.returningId ++= ids.map { id ⇒
            Factories.giftCardPayment.copy(orderId = cart.id, paymentMethodId = id, amount = 25.some)
          })
        } yield ids).runT().futureValue.rightVal

        val result = Checkout(cart, cartValidator()).checkout.futureValue.rightVal
        val current = Orders.findById(cart.id).extract.one.run().futureValue.value
        val adjustments = GiftCardAdjustments.filter(_.giftCardId.inSet(ids)).result.run().futureValue

        import GiftCardAdjustment._

        adjustments.map(_.status).toSet must === (Set[Status](Auth))
        adjustments.map(_.debit) must === (List(25, 25, 25))
      }

      "for all store credits" in new PaymentFixture {
        val ids = (for {
          origin ← * <~ StoreCreditManuals.create(Factories.storeCreditManual.copy(adminId = admin.id, reasonId = reason.id))
          ids    ← * <~ (StoreCredits.returningId ++= (1 to 3).map { _ ⇒
            Factories.storeCredit.copy(originalBalance = 25, originId = origin.id)
          })
          _      ← * <~ (OrderPayments.returningId ++= ids.map { id ⇒
            Factories.storeCreditPayment.copy(orderId = cart.id, paymentMethodId = id, amount = 25.some)
          })
        } yield ids).runT().futureValue.rightVal

        val result = Checkout(cart, cartValidator()).checkout.futureValue.rightVal
        val current = Orders.findById(cart.id).extract.one.run().futureValue.value
        val adjustments = StoreCreditAdjustments.filter(_.storeCreditId.inSet(ids)).result.run().futureValue

        import StoreCreditAdjustment._

        adjustments.map(_.status).toSet must === (Set[Status](Auth))
        adjustments.map(_.debit) must === (List(25, 25, 25))
      }
    }
  }

  trait Fixture {
    val cart = Orders.create(Factories.cart).run().futureValue.rightVal
  }

  trait GCLineItemFixture {
    val (customer, cart, giftCard) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      cart       ← * <~ Orders.create(Factories.cart.copy(customerId = customer.id))
      origin     ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = cart.id))
      giftCard   ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      lineItemGc ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = cart.id))
      lineItem   ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(cart, lineItemGc))
    } yield (customer, cart, giftCard)).runT().futureValue.rightVal
  }

  trait PaymentFixture {
    val (admin, customer, cart, reason) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Orders.create(Factories.cart.copy(customerId = customer.id))
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, cart, reason)).runT().futureValue.rightVal
  }
}
