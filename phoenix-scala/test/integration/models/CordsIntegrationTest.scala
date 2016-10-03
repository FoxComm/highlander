package models

import models.cord._
import slick.driver.PostgresDriver.api._
import testutils._
import testutils.fixtures.BakedFixtures

class CordsIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "should not override cart's reference_number" in new Customer_Seed {
    Carts.create(Cart(referenceNumber = "foo", customerId = customer.id)).gimme
    val cord = Cords.result.headOption.gimme.value
    cord.referenceNumber must === ("foo")
    cord.isCart mustBe true
  }

  "should generate and increment reference_number if empty" in new Customer_Seed {
    (1 to 3).map { i ⇒
      val cart = Carts.create(Cart(customerId = customer.id)).gimme
      val cord = Cords.findOneByRefNum(cart.refNum).gimme.value
      cord.referenceNumber must === (s"BR1000$i")
      cart.referenceNumber must === (cord.referenceNumber)
      cord.isCart mustBe true
      Orders.createFromCart(cart).gimme
    }
  }

  "cord should be updated and cart should be deleted on order creation" in new Customer_Seed {
    val cart  = Carts.create(Cart(customerId = customer.id)).gimme
    val order = Orders.createFromCart(cart).gimme
    val cord  = Cords.result.headOption.gimme.value
    cart.referenceNumber must === (cord.referenceNumber)
    order.referenceNumber must === (cord.referenceNumber)
    cord.isCart mustBe false
    Carts.findOneByRefNum(cart.referenceNumber).gimme must not be defined
  }
}
