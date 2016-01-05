package utils.seeds

import scala.util.Random

import utils.seeds.generators.CustomerGenerator
import utils.seeds.generators.AddressGenerator
import utils.seeds.generators.CreditCardGenerator
import models.{Address, Addresses, CreditCard, CreditCards, Customer, Customers, Order, OrderPayment, OrderPayments, Orders, PaymentMethod}
import slick.driver.PostgresDriver.api._
import utils.ModelWithIdParameter
import utils.DbResultT._
import utils.DbResultT.implicits._
import Seeds.Factories
import scala.concurrent.ExecutionContext
import faker.Faker;

object SeedsGenerator extends CustomerGenerator with AddressGenerator with CreditCardGenerator {

  def generateOrder(status: Order.Status, customerId: Int): Order = {
    Order(customerId = customerId, referenceNumber = randomString(8) + "-17", status = status)
  }

  def generateOrderPayment[A <: PaymentMethod with ModelWithIdParameter[A]](orderId: Int,
    paymentMethod: A, amount: Int = 100): OrderPayment = {
    Factories.orderPayment.copy(orderId = orderId, amount = Some(amount),
      paymentMethodId = paymentMethod.id)
  }

  def generateAddresses(customerIds: Seq[Int]): Seq[Address] = { 
    customerIds.flatMap { id ⇒ 
        generateAddress(customerId = id, isDefault = true) +: 
        ((0 to (Random.nextInt(2))) map { i ⇒ 
          generateAddress(customerId = id, isDefault = false)
        })
    }
  }

  def makePayment(o: Order, pm: CreditCard) = {
    generateOrderPayment(o.id, pm, Random.nextInt(20000) + 100)
  }

  def getOrdersWithCC(customerIds: Seq[Int]) = 
      Orders.join(CreditCards.filter(_.customerId.inSet(customerIds))).on(_.customerId === _.customerId).result

  def makeOrders(customerId: Int) = {
    (1 to 5 + Random.nextInt(20)).map { i ⇒ generateOrder(Order.Shipped, customerId) }
  }
  
  def insertRandomizedSeeds(customersCount: Int)(implicit db: Database, ec: ExecutionContext) = {
    Faker.locale("en")
    val location = "Random"

    for {
      customerIds ← * <~ Customers.createAllReturningIds(generateCustomers(customersCount, location))
      _ ← * <~ Addresses.createAll(generateAddresses(customerIds))
      _ ← * <~ CreditCards.createAll(generateCreditCards(customerIds))
      _ ← * <~ Orders.createAll(customerIds.flatMap(makeOrders))
      ordersWithCc ← * <~ getOrdersWithCC(customerIds)
      _ ← * <~ OrderPayments.createAll(ordersWithCc.map { case (order, cc) ⇒ makePayment(order, cc) })
    } yield {}
  }

  def randomOrderStatus: Order.Status = {
    val types = Order.Status.types.filterNot(_ == Order.Cart)
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}
