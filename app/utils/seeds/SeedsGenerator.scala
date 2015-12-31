package utils.seeds

import scala.util.Random

import utils.seeds.generators.CustomerGenerator
import utils.seeds.generators.AddressGenerator
import models.{Addresses, CreditCard, CreditCards, Customer, Customers, Order, OrderPayment, OrderPayments, Orders, PaymentMethod}
import slick.driver.PostgresDriver.api._
import utils.ModelWithIdParameter
import utils.DbResultT._
import utils.DbResultT.implicits._
import Seeds.Factories
import scala.concurrent.ExecutionContext
import faker.Faker;

object SeedsGenerator extends CustomerGenerator with AddressGenerator{

  def generateOrder(status: Order.Status, customerId: Int): Order = {
    Order(customerId = customerId, referenceNumber = randomString(8) + "-17", status = status)
  }

  def generateOrderPayment[A <: PaymentMethod with ModelWithIdParameter[A]](orderId: Int,
    paymentMethod: A, amount: Int = 100): OrderPayment = {
    Factories.orderPayment.copy(orderId = orderId, amount = Some(amount),
      paymentMethodId = paymentMethod.id)
  }

  def generateCreditCards(customers: Seq[Customer]) = { 
    customers.map { c ⇒ 
      Factories.creditCard.copy(customerId = c.id, holderName = c.name.getOrElse(""))
    }
  }

  def generateAddresses(customers: Seq[Customer]) = { 
    customers.flatMap { c ⇒ 
        generateAddress(customerId = c.id, isDefault = true) +: 
        ((0 to (Random.nextInt(2))) map { i ⇒ 
          generateAddress(customerId = c.id, isDefault = false)
        })
    }
  }

  def makePayment(o: Order, pm: CreditCard) = {
    Console.err.println(s"payment: $o $pm")
    generateOrderPayment(o.id, pm, Random.nextInt(20000) + 100)
  }

  def makeOrders(c: Customer) = {
    (1 to 5 + Random.nextInt(20)).map { i ⇒ generateOrder(Order.Shipped, c.id) }
  }
  
  def insertRandomizedSeeds(customersCount: Int)(implicit db: Database, ec: ExecutionContext) = {
    Faker.locale("en")
    val location = "Arkham"

    for {
      _ ← * <~ Customers.createAll(generateCustomers(customersCount, location))
      customers ← * <~ Customers.filter(_.location === location).result
      addresses = generateAddresses(customers)
      _ ← * <~ Addresses.createAll(addresses)
      creditCards = generateCreditCards(customers)
      _ ← * <~ CreditCards.createAll(creditCards)
      orders = customers.flatMap(makeOrders)
      _ ← * <~ Orders.createAll(orders)
      ordersWithCc ← * <~ Orders.join(CreditCards).on(_.customerId === _.customerId).result
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
