package services

import cats.data.Xor
import models.{Address, Addresses, OrderPayment, OrderPayments, CreditCard, CreditCards, Customer, Customers, Order, OrderLineItem, OrderLineItems, Orders}

import org.scalatest.Inside
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._

class CheckoutTest extends IntegrationTestBase with Inside {
  import concurrent.ExecutionContext.Implicits.global

  "Checkout" - {
    "checkout" - {
      "returns a new Order with the 'Cart' status" ignore { /** Needs Stripe mocks */
        val (order, _) = testData()

        val lineItemStub1 = OrderLineItem(id = 0, orderId = 0, skuId = 1)
        val lineItemStub2 = OrderLineItem(id = 0, orderId = 0, skuId = 2)

        val actions = for {
          _ ← OrderLineItems.returningId += lineItemStub1.copy(orderId = order.id)
          _ ← OrderLineItems.returningId += lineItemStub2.copy(orderId = order.id)
        } yield ()

        actions.run()

        val checkout = new Checkout(order)
        val result   = checkout.checkout.futureValue

        val newOrder = result.get
        newOrder.status must === (Order.Cart)
        newOrder.id     must !== (order.id)
      }

      "returns an errors if it has no line items" in {
        val (order, _) = testData()
        val checkout = new Checkout(order)

        val result = checkout.checkout.futureValue
        result mustBe 'left

        inside(result) {
          case Xor.Left(NotFoundFailure(message) :: Nil) ⇒
            message must include ("No Line Items")
        }
      }

      /** Test data leak? */

      "returns an error if authorizePayments fails" in {
        pending /** Need a way to mock Stripe */

        val (order, _) = testData(gatewayCustomerId = "")

        val lineItemStub1 = OrderLineItem(id = 0, orderId = 0, skuId = 1)
        val lineItemStub2 = OrderLineItem(id = 0, orderId = 0, skuId = 2)

        val actions = for {
          _ ← OrderLineItems.returningId += lineItemStub1.copy(orderId = order.id)
          _ ← OrderLineItems.returningId += lineItemStub2.copy(orderId = order.id)
        } yield ()

        actions.run()

        val checkout = new Checkout(order)
        val result   = checkout.checkout.futureValue

        val (StripeFailure(errorMessage) :: Nil) = result.swap.get
        errorMessage.getMessage must include ("cannot set 'customer' to an empty string.")
      }
    }

    "Authorizes each payment" in pendingUntilFixed { /** Needs Stripe mocks */
      fail("fix me")
    }
  }

  def testData(gatewayCustomerId:String = "cus_6Rh139qdpaFdRP") = {
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val orderStub    = Order(id = 0, customerId = 0)
    val addressStub  = Address(id = 0, customerId = 0, regionId = 1, name = "Yax Home", street1 = "555 E Lake Union " +
      "St.", street2 = None, city = "Seattle", zip = "12345", phoneNumber = None)
    val gatewayStub  = Factories.creditCard.copy(gatewayCustomerId = gatewayCustomerId, lastFour = "4242",
      expMonth = 11, expYear = 2018)

    val (payment, order) = (for {
      customer ← (Customers.returningId += customerStub).map(id ⇒ customerStub.copy(id = id))
      order    ← Orders.save(orderStub.copy(customerId = customer.id))
      address  ← Addresses.save(addressStub.copy(customerId = customer.id))
      creditCard ← CreditCards.save(gatewayStub.copy(customerId = customer.id, billingAddressId = address.id))
      payment  ← OrderPayments.save(Factories.orderPayment.copy(orderId = order.id, paymentMethodId = creditCard.id))
    } yield (payment, order)).run().futureValue

    (order, payment)
  }
}
