package services

import models.{Address, Addresses, OrderPayment, OrderPayments, CreditCard, CreditCards, Customer, Customers, Order, OrderLineItem, OrderLineItems, Orders}
import org.scalactic.{Bad, TypeCheckedTripleEquals}
import org.scalatest.Inside
import util.IntegrationTestBase
import utils._

class CheckoutTest extends IntegrationTestBase with Inside with TypeCheckedTripleEquals {
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
        result mustBe 'bad

        inside(result) {
          case Bad(NotFoundFailure(message) :: Nil) ⇒
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

    "Authorizes each payment" ignore { /** Needs Stripe mocks */
      val (order, payment) = testData()

      val checkout = new Checkout(order)
      val result   = checkout.authorizePayments.futureValue

      val (authedPayment, errors) = result.collectFirst {
        case (p, e) if p.id == payment.id ⇒ (p, e)
      }.get

      authedPayment.chargeId mustNot be ('empty)
      errors mustBe ('empty)
    }
  }

  def testData(gatewayCustomerId:String = "cus_6Rh139qdpaFdRP") = {
    val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
    val orderStub    = Order(id = 0, customerId = 0)
    val addressStub  = Address(id = 0, customerId = 0, stateId = 1, name = "Yax Home", street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "12345")
    val paymentStub  = OrderPayment(id = 0, orderId = 0, paymentMethodId = 1, paymentMethodType = "stripe", appliedAmount = 10, status = "auth", responseCode = "ok")
    val gatewayStub  = CreditCard(id = 0, customerId = 0, gatewayCustomerId = gatewayCustomerId, lastFour = "4242", expMonth = 11, expYear = 2018)

    val (payment, order) = (for {
      customer ← (Customers.returningId += customerStub).map(id ⇒ customerStub.copy(id = id))
      order    ← Orders.save(orderStub.copy(customerId = customer.id))
      address  ← Addresses.save(addressStub.copy(customerId = customer.id))
      payment  ← OrderPayments.save(paymentStub.copy(orderId = order.id))
      gateway ← CreditCards.save(gatewayStub.copy(customerId = customer.id, billingAddressId = address.id))
    } yield (payment, order)).run().futureValue

    (order, payment)
  }
}
