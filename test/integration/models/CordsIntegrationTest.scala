package models

import models.cord._
import slick.driver.PostgresDriver.api._
import util.{Fixtures, IntegrationTestBase, TestObjectContext}
import utils.seeds.Seeds.Factories

class CordsIntegrationTest extends IntegrationTestBase with TestObjectContext with Fixtures {

  "should not override cart's reference_number" in new CustomerFixture {
    Carts.create(Factories.cart.copy(referenceNumber = "foo")).gimme
    val cord = Cords.result.headOption.gimme.value
    cord.referenceNumber must === ("foo")
    cord.isCart mustBe true
  }

  "should generate and increment reference_number if empty" in new CustomerFixture {
    (1 to 3).map { i ⇒
      val cart = Carts.create(Factories.cart.copy(referenceNumber = "")).gimme
      val cord = Cords.findOneByRefNum(cart.refNum).gimme.value
      cord.referenceNumber must === (s"BR1000$i")
      cart.referenceNumber must === (cord.referenceNumber)
      cord.isCart mustBe true
      Orders.create(cart.toOrder()).gimme
    }
  }

  "cord should be updated and cart should be deleted on order creation" in new CustomerFixture {
    val cart  = Carts.create(Factories.cart).gimme
    val order = Orders.create(cart.toOrder()).gimme
    val cord  = Cords.result.headOption.gimme.value
    cart.referenceNumber must === (cord.referenceNumber)
    order.referenceNumber must === (cord.referenceNumber)
    cord.isCart mustBe false
    Carts.findOneByRefNum(cart.referenceNumber).gimme must not be defined
  }
}
