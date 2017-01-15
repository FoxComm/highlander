package services

import cats.implicits._
import failures.GeneralFailure
import faker.Lorem
import models.Reasons
import models.account.Scope
import models.cord._
import models.inventory.Skus
import models.objects.ObjectContexts
import models.payment.InStorePaymentStates
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.{Mvp, SimpleContext}
import models.shipping.ShippingMethods
import org.mockito.Mockito._
import org.scalacheck.Prop.BooleanOperators
import org.scalacheck.{Gen, Prop, Test ⇒ QTest}
import org.scalatest.mock.MockitoSugar
import payloads.LineItemPayloads.UpdateLineItemsPayload
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.fixtures.BakedFixtures
import utils.MockedApis
import utils.db._
import utils.seeds.Seeds.Factories

class CheckoutTest
    extends IntegrationTestBase
    with MockitoSugar
    with MockedApis
    with TestObjectContext
    with TestActivityContext.AdminAC
    with BakedFixtures {

  def cartValidator(resp: CartValidatorResponse = CartValidatorResponse()): CartValidation = {
    val m = mock[CartValidation]
    when(m.validate(isCheckout = false, fatalWarnings = true)).thenReturn(DbResultT.good(resp))
    when(m.validate(isCheckout = false, fatalWarnings = false)).thenReturn(DbResultT.good(resp))
    when(m.validate(isCheckout = true, fatalWarnings = true)).thenReturn(DbResultT.good(resp))
    when(m.validate(isCheckout = true, fatalWarnings = false)).thenReturn(DbResultT.good(resp))
    m
  }

  "Checkout" - {

    "fails if the cart validator fails" in new EmptyCustomerCart_Baked {
      val failure       = GeneralFailure("scalac")
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate(isCheckout = false, fatalWarnings = true))
        .thenReturn(DbResultT.failure[CartValidatorResponse](failure))

      val result = Checkout(cart, mockValidator).checkout.futureValue.leftVal
      result must === (failure.single)
    }

    "fails if the cart validator has warnings" in new EmptyCustomerCart_Baked {
      val failure       = GeneralFailure("scalac")
      val mockValidator = mock[CartValidation]
      val liftedFailure = DbResultT.failure[CartValidatorResponse](failure)
      when(mockValidator.validate(isCheckout = false, fatalWarnings = true))
        .thenReturn(liftedFailure)
      when(mockValidator.validate(isCheckout = true, fatalWarnings = true))
        .thenReturn(liftedFailure)

      val result = Checkout(cart, mockValidator).checkout.futureValue.leftVal
      result must === (failure.single)
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

        adjustments.map(_.state).toSet must === (
            Set[InStorePaymentStates.State](InStorePaymentStates.Auth))
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

        adjustments.map(_.state).toSet must === (
            Set[InStorePaymentStates.State](InStorePaymentStates.Auth))
        adjustments.map(_.debit) must === (List(scAmount, cart.grandTotal - scAmount))
      }
    }

    // TODO: rewrite as proper API integration test
    "GC/SC payments limited by grand total" in new PaymentFixture {
      pending

      val paymentAmountGen = Gen.choose(1, 2000)
      val cartTotalGen     = Gen.choose(500, 1000)

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
        case (gcData, scData, total) ⇒
          val dbResultT = for {
            // If any of checkouts fail, rest of for comprehension is ignored and scalacheck just starts a new one.
            // Hence you have an attempt to create a second cart for customer which is prohibited.
            // This is a silly guard to see real errors, not customer_has_only_one_cart constraint.
            _ ← * <~ Carts.deleteAll(DbResultT.unit, DbResultT.unit)

            cart ← * <~ Carts.create(Cart(accountId = customer.accountId, scope = Scope.current))

            _ ← * <~ LineItemUpdater.updateQuantitiesOnCart(storeAdmin,
                                                            cart.refNum,
                                                            lineItemPayload(total))

            c ← * <~ Carts.refresh(cart)
            _ = println(c.grandTotal)

            _ ← * <~ OrderShippingMethods.create(
                   OrderShippingMethod.build(cart.refNum, shipMethod))
            _ ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                            cordRef = cart.refNum)

            gcIds ← * <~ generateGiftCards(gcData.map(_.cardAmount))
            scIds ← * <~ generateStoreCredits(scData.map(_.cardAmount))

            _ ← * <~ OrderPayments.createAllReturningIds(gcIds.zip(gcData.map(_.payAmount)).map {
                 case (id, amount) ⇒ genGCPayment(cart.refNum, id, amount)
               })
            _ ← * <~ OrderPayments.createAllReturningIds(scIds.zip(scData.map(_.payAmount)).map {
                 case (id, amount) ⇒ genSCPayment(cart.refNum, id, amount)
               })

            // Do not mock validator because OrderResponse requires that data anyway
            _ ← * <~ Checkout.fromCart(cart.refNum)

            scAdjustments ← * <~ StoreCreditAdjustments.filter(_.storeCreditId.inSet(scIds)).result

            totalAdjustments = scAdjustments.map(_.getAmount.abs).sum
          } yield totalAdjustments

          dbResultT
            .fold(failures ⇒ false :| "\nFailures:\n" + failures.toList.mkString("\n"),
                  result ⇒ Prop(result == total))
            .gimme
      }
      val qr = QTest.check(checkoutTests)(_.withWorkers(1))

      if (!qr.passed) {
        fail(qr.status.toString)
      }
    }
  }

  trait PaymentFixture extends CustomerAddress_Baked with StoreAdmin_Seed {
    val (reason, shipMethod) = (for {
      reason     ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      shipMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head)
    } yield (reason, shipMethod)).gimme

    def lineItemPayload(cost: Int) = {
      val sku = (for {
        productCtx ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
        product ← * <~ Mvp.insertProduct(
                     productCtx.id,
                     Factories.products.head.copy(price = cost, code = Lorem.letterify("?????")))
        sku ← * <~ Skus.mustFindById404(product.skuId)
      } yield sku).gimme
      Seq(UpdateLineItemsPayload(sku.code, 1))
    }

    def generateGiftCards(amount: Seq[Int]) =
      for {
        origin ← * <~ GiftCardManuals.create(
                    GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
        ids ← * <~ GiftCards.createAllReturningIds(amount.map(gcAmount ⇒
                       Factories.giftCard.copy(originalBalance = gcAmount, originId = origin.id)))
      } yield ids

    def generateStoreCredits(amount: Seq[Int]) =
      for {
        origin ← * <~ StoreCreditManuals.create(
                    StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
        ids ← * <~ StoreCredits.createAllReturningIds(
                 amount.map(scAmount ⇒
                       Factories.storeCredit.copy(originalBalance = scAmount,
                                                  originId = origin.id,
                                                  accountId = customer.accountId)))
      } yield ids
  }

  trait PaymentFixtureWithCart extends PaymentFixture with EmptyCart_Raw {
    override val cart = super.cart.copy(grandTotal = 1000)
    (for {
      _ ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(cart.refNum, shipMethod))
      _ ← * <~ OrderShippingAddresses.copyFromAddress(address = address, cordRef = cart.refNum)
    } yield {}).gimme
  }
}
