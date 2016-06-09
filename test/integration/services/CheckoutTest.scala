package services

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import failures.CartFailures._
import failures.GeneralFailure
import models.customer.Customers
import models.order.Orders.scope._
import models.order._
import models.order.lineitems._
import models.payment.giftcard._
import models.payment.storecredit._
import models.{Reasons, StoreAdmins}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import slick.driver.PostgresDriver.api._
import util._
import utils.Money.Currency
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutTest
    extends IntegrationTestBase
    with MockitoSugar
    with MockedApis
    with TestActivityContext.AdminAC {

  def cartValidator(resp: CartValidatorResponse = CartValidatorResponse()): CartValidation = {
    val m = mock[CartValidation]
    when(m.validate(isCheckout = false, fatalWarnings = true)).thenReturn(DbResult.good(resp))
    when(m.validate(isCheckout = false, fatalWarnings = false)).thenReturn(DbResult.good(resp))
    when(m.validate(isCheckout = true, fatalWarnings = true)).thenReturn(DbResult.good(resp))
    when(m.validate(isCheckout = true, fatalWarnings = false)).thenReturn(DbResult.good(resp))
    m
  }

  "Checkout" - {
    "fails if the order is not a cart" in new Fixture {
      val nonCart = cart.copy(state = Order.RemorseHold)
      val result  = Checkout(nonCart, CartValidator(nonCart)).checkout.run().futureValue.leftVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      result must ===(OrderMustBeCart(nonCart.refNum).single)
      current.state must ===(cart.state)
    }

    "fails if the cart validator fails" in new CustomerFixture {
      val failure       = GeneralFailure("scalac").single
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate(isCheckout = false, fatalWarnings = true))
        .thenReturn(DbResult.failures(failure))

      val cart = Orders.create(Factories.cart).run().futureValue.rightVal
      val result = Checkout(cart.copy(customerId = customer.id), mockValidator).checkout
        .run()
        .futureValue
        .leftVal
      result must ===(failure)
    }

    "fails if the cart validator has warnings" in new CustomerFixture {
      val failure       = GeneralFailure("scalac")
      val mockValidator = mock[CartValidation]
      val liftedFailure = DbResult.failure(failure)
      when(mockValidator.validate(isCheckout = false, fatalWarnings = true))
        .thenReturn(liftedFailure)
      when(mockValidator.validate(isCheckout = true, fatalWarnings = true))
        .thenReturn(liftedFailure)

      val cart = Orders.create(Factories.cart).run().futureValue.rightVal
      val result = Checkout(cart.copy(customerId = customer.id), mockValidator).checkout
        .run()
        .futureValue
        .leftVal
      result must ===(failure.single)
    }

    "updates state to RemorseHold and touches placedAt" in new Fixture {
      val before  = Instant.now
      val result  = Checkout(cart, cartValidator()).checkout.run().futureValue.rightVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      current.state must ===(Order.RemorseHold)
      current.placedAt.value mustBe >=(before)
    }

    "creates new cart for user at the end" in new Fixture {
      val result  = Checkout(cart, cartValidator()).checkout.run().futureValue.rightVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val newCart = Orders.findByCustomerId(cart.customerId).cartOnly.one.run().futureValue.value

      newCart.id must !==(cart.id)
      newCart.state must ===(Order.Cart)
      newCart.isLocked mustBe false
      newCart.placedAt mustBe 'empty
      newCart.remorsePeriodEnd mustBe 'empty
    }

    "sets all gift card line item purchases as GiftCard.OnHold" in new GCLineItemFixture {
      val result  = Checkout(cart, cartValidator()).checkout.run().futureValue.rightVal
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val gc      = GiftCards.findById(giftCard.id).extract.one.run().futureValue.value

      gc.state must ===(GiftCard.OnHold)
    }

    "authorizes payments" - {
      "for all gift cards" in new PaymentFixture {
        val gcAmount  = cart.grandTotal - 1
        val gcPayment = Factories.giftCardPayment.copy(orderId = cart.id, amount = gcAmount.some)

        val adjustments = (for {
          ids ← * <~ generateGiftCards(List.fill(3)(gcAmount))
          _ ← * <~ OrderPayments.createAllReturningIds(
                 ids.map(id ⇒ gcPayment.copy(paymentMethodId = id)))
          _           ← * <~ Checkout(cart, cartValidator()).checkout
          adjustments ← * <~ GiftCardAdjustments.filter(_.giftCardId.inSet(ids)).result
        } yield adjustments).runTxn().futureValue.rightVal

        import GiftCardAdjustment._

        adjustments.map(_.state).toSet must ===(Set[State](Auth))
        adjustments.map(_.debit) must ===(List(gcAmount, cart.grandTotal - gcAmount))
      }

      "for all store credits" in new PaymentFixture {
        val scAmount = cart.grandTotal - 1
        val scPayment =
          Factories.storeCreditPayment.copy(orderId = cart.id, amount = scAmount.some)

        val adjustments = (for {
          ids ← * <~ generateStoreCredits(List.fill(3)(scAmount))
          _ ← * <~ OrderPayments.createAllReturningIds(
                 ids.map(id ⇒ scPayment.copy(paymentMethodId = id)))
          _           ← * <~ Checkout(cart, cartValidator()).checkout
          adjustments ← * <~ StoreCreditAdjustments.filter(_.storeCreditId.inSet(ids)).result
        } yield adjustments).runTxn().futureValue.rightVal

        import StoreCreditAdjustment._

        adjustments.map(_.state).toSet must ===(Set[State](Auth))
        adjustments.map(_.debit) must ===(List(scAmount, cart.grandTotal - scAmount))
      }
    }
  }

  trait Fixture {
    val (customer, cart) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Orders.create(Factories.cart.copy(customerId = customer.id))
    } yield (customer, cart)).runTxn().futureValue.rightVal
  }

  trait CustomerFixture {
    val customer = Customers.create(Factories.customer).run().futureValue.rightVal
  }

  trait GCLineItemFixture {
    val (customer, cart, giftCard) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Orders.create(Factories.cart.copy(customerId = customer.id))
      origin   ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = cart.id))
      giftCard ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = 150,
                                                              originId = origin.id,
                                                              currency = Currency.USD))
      lineItemGc ← * <~ OrderLineItemGiftCards.create(
                      OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = cart.id))
      lineItem ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(cart, lineItemGc))
    } yield (customer, cart, giftCard)).runTxn().futureValue.rightVal
  }

  trait PaymentFixture {
    val (admin, customer, cart, reason) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Orders.create(Factories.cart.copy(customerId = customer.id, grandTotal = 1000))
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, cart, reason)).runTxn().futureValue.rightVal

    def generateGiftCards(amount: Seq[Int]) =
      for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = admin.id, reasonId = reason.id))
        ids ← * <~ GiftCards.createAllReturningIds(amount.map(gcAmount ⇒
                       Factories.giftCard.copy(originalBalance = gcAmount, originId = origin.id)))
      } yield ids

    def generateStoreCredits(amount: Seq[Int]) =
      for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = admin.id, reasonId = reason.id))
        ids ← * <~ StoreCredits.createAllReturningIds(amount.map(scAmount ⇒
                       Factories.storeCredit.copy(originalBalance = scAmount, originId = origin.id)))
      } yield ids
  }
}
