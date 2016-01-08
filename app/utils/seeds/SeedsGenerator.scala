package utils.seeds

import scala.util.Random

import utils.seeds.generators.{CustomerGenerator, AddressGenerator, CreditCardGenerator, 
OrderGenerator, InventoryGenerator}
import models.{Address, Addresses, CreditCard, CreditCards, Customer, Customers, 
Order, OrderPayment, OrderPayments, Orders, PaymentMethod, Skus, Sku}
import slick.driver.PostgresDriver.api._
import utils.ModelWithIdParameter
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import Seeds.Factories
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import faker.Faker;

object SeedsGenerator extends CustomerGenerator with AddressGenerator 
  with CreditCardGenerator with OrderGenerator with InventoryGenerator{

  def generateAddresses(customerIds: Seq[Int]): Seq[Address] = { 
    customerIds.flatMap { id ⇒ 
        generateAddress(customerId = id, isDefault = true) +: 
        ((0 to (Random.nextInt(2))) map { i ⇒ 
          generateAddress(customerId = id, isDefault = false)
        })
    }
  }

  def makeSkus = (1 to 1 + Random.nextInt(100)).map { i ⇒  generateSku }

  def randomSubset[T](vals: Seq[T]) : Seq[T] = {
    require(vals.length > 0)
    val size = Math.max(Random.nextInt(Math.min(vals.length, 5)), 1)
    (1 to size) map { 
      i ⇒  vals(i * Random.nextInt(vals.length) % vals.length) 
    }
  }

  def insertRandomizedSeeds(customersCount: Int)(implicit db: Database, ec: ExecutionContext) = {
    Faker.locale("en")
    val location = "Random"

    for {
      shipMethods ← * <~ createShipmentRules
      _ ← * <~  generateWarehouses
      skuIds ← * <~  generateInventory(makeSkus)
      skus  ← * <~ Skus.filter(_.id.inSet(skuIds)).result
      customerIds ← * <~ Customers.createAllReturningIds(generateCustomers(customersCount, location))
      customers  ← * <~ Customers.filter(_.id.inSet(customerIds)).result
      _ ← * <~ Addresses.createAll(generateAddresses(customerIds))
      _ ← * <~ CreditCards.createAll(generateCreditCards(customerIds))
      orders ← * <~ DbResultT.sequence(customers.map{ c ⇒ generateOrder(c.id, randomSubset(skus))})
    } yield {}
  }

  def randomOrderStatus: Order.Status = {
    val types = Order.Status.types.filterNot(_ == Order.Cart)
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}
