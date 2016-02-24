package utils.seeds

import scala.util.Random

import utils.seeds.generators.{CustomerGenerator, AddressGenerator, CreditCardGenerator, 
  OrderGenerator, InventoryGenerator, ProductGenerator}
import models.{Address, Addresses, CreditCard, CreditCards, Customer, Customers, 
  Order, OrderPayment, OrderPayments, Orders, PaymentMethod, CustomerDynamicGroup,
  OrderLineItemSku, OrderLineItemSkus}
import models.product.{Sku, Skus, ProductContext, ProductContexts, SimpleContext}

import utils.ModelWithIdParameter
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import Seeds.Factories

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import faker.Faker;
import org.json4s.JObject

object RankingSeedsGenerator {
  def fakeJson = JObject()

  def generateCustomer: Customer = Customer(email = s"${randomString(10)}@email.com",
    password = Some(randomString(10)), name = Some(randomString(10)))

  def generateOrder(state: Order.State, customerId: Int, productContext: ProductContext): Order = {
    Order(customerId = customerId, 
      referenceNumber = randomString(8) + "-17", 
      state = state, productContextId = productContext.id)
  }

  def generateOrderPayment[A <: PaymentMethod with ModelWithIdParameter[A]](orderId: Int,
    paymentMethod: A, amount: Int = 100): OrderPayment = {
      Factories.orderPayment.copy(orderId = orderId, amount = Some(amount),
        paymentMethodId = paymentMethod.id)
  }

  def generateAddress: Address = Address(customerId = 0, regionId = 4177, name = randomString(10),
    address1 = randomString(30), address2 = None, city = "Seattle", zip = "12345", isDefaultShipping = false,
    phoneNumber = None)

  def generateGroup(ownerId: Int): CustomerDynamicGroup = CustomerDynamicGroup(name = s"${randomString(10)}Group",
    createdBy = ownerId, clientState = fakeJson, elasticRequest = fakeJson,
    customersCount = Some(Random.nextInt))

  def insertRankingSeeds(customersCount: Int)(implicit db: Database) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val location = "Arkham"

    def makeOrders(c: Customer, productContext: ProductContext) = {
      (1 to 5 + Random.nextInt(20)).map { i ⇒ generateOrder(Order.Shipped, c.id, productContext) }
    }

    def makePayment(o: Order, pm: CreditCard) = {
      generateOrderPayment(o.id, pm, Random.nextInt(20000) + 100)
    }

    val insertCustomers = Customers.createAll((1 to customersCount).map { i ⇒
      val s = randomString(15)
      Customer(name = Some(s), email = s"$s-$i@email.com", password = Some(s), location = Some(location))
    })

    val insertOrders = for { 
      productContext ← * <~ ProductContexts.create(SimpleContext.create)
      customers ← * <~ Customers.filter(_.location === location).result
      newCreditCards ← * <~  customers.map { c ⇒
        Factories.creditCard.copy(customerId = c.id, holderName = c.name.getOrElse(""))
      }
      _ ← * <~ CreditCards.createAll(newCreditCards)
      orders ← * <~ customers.flatMap{c ⇒ makeOrders(c, productContext)}
      _  ← * <~  Orders.createAll(orders)
    } yield {}

    val insertPayments = {
      val action = (for {
        (o, cc) ← Orders.join(CreditCards).on(_.customerId === _.customerId)
      } yield (o, cc)).result

      action.flatMap { ordersWithCc ⇒
        OrderPayments.createAll(ordersWithCc.map { case (order, cc) ⇒ makePayment(order, cc) })
      }
    }

    for {
      _  ← * <~  insertCustomers
      _  ← * <~  insertOrders
      _  ← * <~  insertPayments
    } yield {}
  }

  def randomOrderState: Order.State = {
    val types = Order.State.types.filterNot(_ == Order.Cart)
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}

object SeedsGenerator extends CustomerGenerator with AddressGenerator 
  with CreditCardGenerator with OrderGenerator with ProductGenerator 
  with InventoryGenerator{

  import org.json4s.JObject

  def generateAddresses(customerIds: Seq[Int]): Seq[Address] = { 
    customerIds.flatMap { id ⇒ 
        generateAddress(customerId = id, isDefault = true) +: 
        ((0 to (Random.nextInt(2))) map { i ⇒ 
          generateAddress(customerId = id, isDefault = false)
        })
    }
  }

  def makeProducts(productCount: Int) = (1 to productCount).map { i ⇒  generateProduct }

  def randomSubset[T](vals: Seq[T]) : Seq[T] = {
    require(vals.length > 0)
    val size = Math.max(Random.nextInt(Math.min(vals.length, 5)), 1)
    (1 to size).map { 
      i ⇒  vals(i * Random.nextInt(vals.length) % vals.length) 
    }.distinct
  }

  def insertRandomizedSeeds(customersCount: Int, productCount: Int)(implicit db: Database, ec: ExecutionContext) = {
    Faker.locale("en")
    val location = "Random"

    for {
      
      productContext ← * <~ ProductContexts.create(SimpleContext.create)
      shipMethods ← * <~ createShipmentRules
      _ ← * <~  generateWarehouses
      products ← * <~ generateProducts(makeProducts(productCount))
      _ ← * <~  generateInventory(products)
      customerIds ← * <~ Customers.createAllReturningIds(generateCustomers(customersCount, location))
      customers  ← * <~ Customers.filter(_.id.inSet(customerIds)).result
      _ ← * <~ Addresses.createAll(generateAddresses(customerIds))
      _ ← * <~ CreditCards.createAll(generateCreditCards(customerIds))
      orders ← * <~ DbResultT.sequence(customers.map{ c ⇒ generateOrder(c.id, productContext, randomSubset(products))})
    } yield {}
  }

}

