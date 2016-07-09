package models

import scala.concurrent.ExecutionContext.Implicits.global

import models.cord.{Carts, Cords, Orders}
import slick.driver.PostgresDriver.api._
import util.{IntegrationTestBase, TestObjectContext}
import utils.seeds.Seeds.Factories

class CordsIntegrationTest extends IntegrationTestBase with TestObjectContext {

  "cord should be created on cart creation" in {
    val cart = Carts.create(Factories.cart).gimme
    val cord = Cords.result.headOption.gimme.value
    cord.referenceNumber must === (cart.referenceNumber)
    cord.cartId must === (cart.id)
  }

  "cord and cart should be updated on order creation" in {
    val cart  = Carts.create(Factories.cart).gimme
    val order = Orders.create(cart.toOrder()).gimme
    val cord  = Cords.result.headOption.gimme.value
    cord.referenceNumber must === (cart.referenceNumber)
    cord.cartId must === (cart.id)
    cord.orderId.value must === (order.id)
    val updatedCart = Carts.refresh(cart).gimme
    updatedCart.isActive mustBe false
  }
}
