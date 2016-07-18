package services

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import failures.GeneralFailure
import models.cord._
import models.cord.lineitems._
import models.customer.Customers
import models.payment.giftcard._
import models.payment.storecredit._
import models.{Reasons, StoreAdmins}
import org.mockito.Mockito._
import org.scalacheck.{Gen, Prop, Test ⇒ QTest}
import org.scalatest.mock.MockitoSugar
import slick.driver.PostgresDriver.api._
import util._
import utils.Money.Currency
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutTest
    extends IntegrationTestBase
    with MockitoSugar
    with MockedApis
    with TestObjectContext
    with TestActivityContext.AdminAC {

  def cartValidator(resp: CartValidatorResponse = CartValidatorResponse()): CartValidation = {
    val m = mock[CartValidation]
    when(m.validate(isCheckout = false, fatalWarnings = true)).thenReturn(DbResultT.good(resp))
    when(m.validate(isCheckout = false, fatalWarnings = false)).thenReturn(DbResultT.good(resp))
    when(m.validate(isCheckout = true, fatalWarnings = true)).thenReturn(DbResultT.good(resp))
    when(m.validate(isCheckout = true, fatalWarnings = false)).thenReturn(DbResultT.good(resp))
    m
  }

  "Checkout" - {

    "fails if the cart validator fails" in new CustomerFixture {
      val failure       = GeneralFailure("scalac")
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate(isCheckout = false, fatalWarnings = true))
        .thenReturn(DbResultT.failure[CartValidatorResponse](failure))

      val cart = Carts.create(Factories.cart).gimme
      val result = Checkout(cart.copy(customerId = customer.id), mockValidator).checkout
        .run()
        .futureValue
        .leftVal
      result must === (failure.single)
    }

    "fails if the cart validator has warnings" in new CustomerFixture {
      val failure       = GeneralFailure("scalac")
      val mockValidator = mock[CartValidation]
      val liftedFailure = DbResultT.failure[CartValidatorResponse](failure)
      when(mockValidator.validate(isCheckout = false, fatalWarnings = true))
        .thenReturn(liftedFailure)
      when(mockValidator.validate(isCheckout = true, fatalWarnings = true))
        .thenReturn(liftedFailure)

      val cart = Carts.create(Factories.cart).gimme
      val result = Checkout(cart.copy(customerId = customer.id), mockValidator).checkout
        .run()
        .futureValue
        .leftVal
      result must === (failure.single)
    }

    "updates state to RemorseHold and touches placedAt" in new Fixture {
      val before  = Instant.now
      val result  = Checkout(cart, cartValidator()).checkout.gimme
      val current = Orders.findOneByRefNum(cart.refNum).gimme.value

      current.state must === (Order.RemorseHold)
      current.placedAt mustBe >=(before)
    }

    "sets all gift card line item purchases as GiftCard.OnHold" in new GCLineItemFixture {
      Checkout(cart, cartValidator()).checkout.gimme
      val gc = GiftCards.mustFindById404(giftCard.id).gimme
      gc.state must === (GiftCard.OnHold)
    }

    "authorizes payments" - {
      "for all gift cards" in new PaymentFixtureWithCart {
        val gcAmount = cart.grandTotal - 1
        val gcPayment =
          Factories.giftCardPayment.copy(cordRef = cart.refNum, amount = gcAmount.some)

        val adjustments = (for {
          ids ← * <~ generateGiftCards(List.fill(3)(gcAmount))
          _ ← * <~ OrderPayments.createAllReturningIds(
                 ids.map(id ⇒ gcPayment.copy(paymentMethodId = id)))
          _           ← * <~ Checkout(cart, cartValidator()).checkout
          adjustments ← * <~ GiftCardAdjustments.filter(_.giftCardId.inSet(ids)).result
        } yield adjustments).gimme

        import GiftCardAdjustment._

        adjustments.map(_.state).toSet must === (Set[State](Auth))
        adjustments.map(_.debit) must === (List(gcAmount, cart.grandTotal - gcAmount))
      }

      "for all store credits" in new PaymentFixtureWithCart {
        val scAmount = cart.grandTotal - 1
        val scPayment =
          Factories.storeCreditPayment.copy(cordRef = cart.refNum, amount = scAmount.some)

        val adjustments = (for {
          ids ← * <~ generateStoreCredits(List.fill(3)(scAmount))
          _ ← * <~ OrderPayments.createAllReturningIds(
                 ids.map(id ⇒ scPayment.copy(paymentMethodId = id)))
          _           ← * <~ Checkout(cart, cartValidator()).checkout
          adjustments ← * <~ StoreCreditAdjustments.filter(_.storeCreditId.inSet(ids)).result
        } yield adjustments).gimme

        import StoreCreditAdjustment._

        adjustments.map(_.state).toSet must === (Set[State](Auth))
        adjustments.map(_.debit) must === (List(scAmount, cart.grandTotal - scAmount))
      }
    }

    "GC/SC payments limited by grand total" in new PaymentFixture {
      val paymentAmountGen = Gen.choose(1, 2000)
      val cartTotalGen     = Gen.choose(0, 1000)

      case class CardPayment(cardAmount: Int, payAmount: Int)

      val cardWithPaymentGen = for {
        payment ← paymentAmountGen
        amount  ← Gen.choose(payment, payment * 2)
      } yield CardPayment(amount, payment)

      val inputGen = for {
        cardsCount ← Gen.choose(1, 10)

        gcCount ← Gen.choose(0, cardsCount)
        gc      ← Gen.listOfN(gcCount, cardWithPaymentGen)

        scCount = cardsCount - gcCount
        sc ← Gen.listOfN(scCount, cardWithPaymentGen)

        grandTotal ← cartTotalGen
        if (gc.map(_.payAmount).sum + sc.map(_.payAmount).sum) >= grandTotal
      } yield (gc, sc, grandTotal)

      def genGCPayment(cordRef: String, id: Int, amount: Int) =
        Factories.giftCardPayment
          .copy(cordRef = cordRef, paymentMethodId = id, amount = amount.some)

      def genSCPayment(cordRef: String, id: Int, amount: Int) =
        Factories.storeCreditPayment
          .copy(cordRef = cordRef, paymentMethodId = id, amount = amount.some)

      val checkoutTests = Prop.forAll(inputGen) {
        case (gcData, scData, orderTotal) ⇒
          val checkoutAmount = (for {
            cart ← * <~ Carts.create(
                      Factories.cart.copy(customerId = customer.id,
                                          grandTotal = orderTotal,
                                          // reset refnum so it's generated on insert
                                          referenceNumber = ""))

            gcIds ← * <~ generateGiftCards(gcData.map(_.cardAmount))
            scIds ← * <~ generateStoreCredits(scData.map(_.cardAmount))

            _ ← * <~ OrderPayments.createAllReturningIds(gcIds.zip(gcData.map(_.payAmount)).map {
                 case (id, amount) ⇒ genGCPayment(cart.refNum, id, amount)
               })
            _ ← * <~ OrderPayments.createAllReturningIds(scIds.zip(scData.map(_.payAmount)).map {
                 case (id, amount) ⇒ genSCPayment(cart.refNum, id, amount)
               })

            _ ← * <~ Checkout(cart, cartValidator()).checkout

            gcAdjustments ← * <~ GiftCardAdjustments.filter(_.giftCardId.inSet(gcIds)).result
            scAdjustments ← * <~ StoreCreditAdjustments.filter(_.storeCreditId.inSet(scIds)).result

            totalAdjustments = gcAdjustments.map(_.getAmount.abs).sum +
              scAdjustments.map(_.getAmount.abs).sum
          } yield totalAdjustments).gimme

          checkoutAmount must === (orderTotal)
          true
      }
      val qr = QTest.check(checkoutTests) {
        _.withWorkers(1)
      }

      if (!qr.passed) {
        fail(qr.status.toString)
      }
    }
  }

  trait Fixture {
    val (customer, cart) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
    } yield (customer, cart)).gimme
  }

  trait CustomerFixture {
    val customer = Customers.create(Factories.customer).gimme
  }

  trait GCLineItemFixture {
    val (customer, cart, giftCard) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
      origin   ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = cart.refNum))
      giftCard ← * <~ GiftCards.create(
                    GiftCard
                      .buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      lineItemGc ← * <~ OrderLineItemGiftCards.create(
                      OrderLineItemGiftCard(giftCardId = giftCard.id, cordRef = cart.refNum))
      lineItem ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(cart, lineItemGc))
    } yield (customer, cart, giftCard)).gimme
  }

  trait PaymentFixture {
    val (admin, customer, reason) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
    } yield (admin, customer, reason)).gimme

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
        ids ← * <~ StoreCredits.createAllReturningIds(
                 amount.map(scAmount ⇒
                       Factories.storeCredit.copy(originalBalance = scAmount,
                                                  originId = origin.id)))
      } yield ids
  }

  trait PaymentFixtureWithCart extends PaymentFixture {
    val cart = Carts.create(Factories.cart.copy(customerId = customer.id, grandTotal = 1000)).gimme
  }
}
