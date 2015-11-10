package services

import java.time.Instant

import models.{Orders, Order}
import models.Orders.scope._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.CartFailures._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import cats.implicits._
import slick.driver.PostgresDriver.api._

class CheckoutTest
  extends IntegrationTestBase
  with MockitoSugar {
  import concurrent.ExecutionContext.Implicits.global

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
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate).thenReturn(Result.good(CartValidatorResponse()))
      val result = rightValue(Checkout(cart, mockValidator).checkout.futureValue)
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value

      current.status must === (Order.RemorseHold)
      current.placedAt.value mustBe >= (before)
    }

    "creates new cart for user at the end" in new Fixture {
      val mockValidator = mock[CartValidation]
      when(mockValidator.validate).thenReturn(Result.good(CartValidatorResponse()))
      val result = rightValue(Checkout(cart, mockValidator).checkout.futureValue)
      val current = Orders.findById(cart.id).extract.one.run().futureValue.value
      val newCart = Orders.findByCustomerId(cart.customerId).cartOnly.one.run().futureValue.value

      newCart.id must !== (cart.id)
      newCart.status must === (Order.Cart)
      newCart.locked mustBe false
      newCart.placedAt mustBe 'empty
      newCart.remorsePeriodEnd mustBe 'empty
    }
  }

  trait Fixture {
    val cart = Orders.saveNew(Factories.cart).run().futureValue
  }
}
