package services

import models.{CreditCardGateways, CreditCardGateway, BillingAddress, BillingAddresses, AppliedPayments, AppliedPayment, Orders, Order, Address, Addresses, Customers, Customer}
import org.scalactic.ErrorMessage
import util.IntegrationTestBase

class CheckoutTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Checkout" - {
    "Authorizes each payment" in {
      val customerStub = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")
      val orderStub    = Order(id = 0, customerId = 0)
      val addressStub  = Address(id = 0, customerId = 0, stateId = 1, name = "Yax Home", street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "12345")
      val paymentStub  = AppliedPayment(id = 0, orderId = 0, paymentMethodId = 1, paymentMethodType = "stripe", appliedAmount = 10, status = "auth", responseCode = "ok")
      val gatewayStub  = CreditCardGateway(id = 0, customerId = 0, gatewayCustomerId = "cus_6Rh139qdpaFdRP", lastFour = "4242", expMonth = 11, expYear = 18)

      val (payment, order) = (for {
        customer ← (Customers.returningId += customerStub).map(id ⇒ customerStub.copy(id = id))
        order    ← Orders.save(orderStub.copy(customerId = customer.id))
        address  ← Addresses.save(addressStub.copy(customerId = customer.id))
        payment  ← AppliedPayments.save(paymentStub.copy(orderId = order.id))
        billingAddress ← BillingAddresses.save(BillingAddress(addressId = address.id, paymentId = payment.id))
        gateway ← CreditCardGateways.save(gatewayStub.copy(customerId = customer.id))
      } yield (payment, order)).run().futureValue

      val checkout = new Checkout(order)
      val result   = checkout.authorizePayments.futureValue

      val (authedPayment, errors) = result.collectFirst {
        case (p, e) if p.id == payment.id ⇒ (p, e)
      }.get

      authedPayment.chargeId mustNot be ('empty)
      errors mustBe ('empty)
    }
  }
}
