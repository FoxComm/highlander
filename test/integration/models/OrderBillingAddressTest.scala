package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import services.GeneralFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.jdbc.withUniqueConstraint
import utils.Slick.implicits._

class OrderBillingAddressTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderBillingAddress" - {
    "has only one billing address per order payment" in new Fixture {
      val result = withUniqueConstraint {
        OrderBillingAddresses.save(billingAddress.copy(name = "Jeff")).run()
      } { notUnique ⇒ GeneralFailure("There was already a billing address") }

      result.futureValue mustBe 'left
    }
  }

  trait Fixture {
    val (order, billingAddress) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      creditCard ← CreditCards.save(Factories.creditCard.copy(customerId = customer.id))
      orderPayment ← OrderPayments.save(Factories.orderPayment.copy(orderId = order.id,
        paymentMethodId = creditCard.id))
      billingAddress ← OrderBillingAddresses.save(Factories.billingAddress.copy(orderPaymentId = orderPayment.id))
    } yield (order, billingAddress)).run().futureValue
  }
}
