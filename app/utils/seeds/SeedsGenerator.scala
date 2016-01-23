package utils.seeds

import scala.util.Random

import models.{Address, CreditCard, CreditCards, Customer, Customers, Order, OrderPayment, OrderPayments,
  Orders, PaymentMethod, CustomerDynamicGroup}
import slick.driver.PostgresDriver.api._
import utils.ModelWithIdParameter
import Seeds.Factories
import org.json4s.JObject

object SeedsGenerator {

  def fakeJson = JObject()

  def generateCustomer: Customer = Customer(email = s"${randomString(10)}@email.com",
    password = Some(randomString(10)), name = Some(randomString(10)))

  def generateOrder(state: Order.State, customerId: Int): Order = {
    Order(customerId = customerId, referenceNumber = randomString(8) + "-17", state = state)
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

    def makeOrders(c: Customer) = {
      (1 to 5 + Random.nextInt(20)).map { i ⇒ generateOrder(Order.Shipped, c.id) }
    }

    def makePayment(o: Order, pm: CreditCard) = {
      generateOrderPayment(o.id, pm, Random.nextInt(20000) + 100)
    }

    val insertCustomers = Customers.createAll((1 to customersCount).map { i ⇒
      val s = randomString(15)
      Customer(name = Some(s), email = s"$s-$i@email.com", password = Some(s), location = Some(location))
    })

    val insertOrders = Customers.filter(_.location === location).result.flatMap { customers ⇒
      val newCustomers = customers.map { c ⇒
        Factories.creditCard.copy(customerId = c.id, holderName = c.name.getOrElse(""))
      }
      DBIO.sequence(Seq(
        CreditCards.createAll(newCustomers),
        Orders.createAll(customers.flatMap(makeOrders))))
    }

    val insertPayments = {
      val action = (for {
        (o, cc) ← Orders.join(CreditCards).on(_.customerId === _.customerId)
      } yield (o, cc)).result

      action.flatMap { ordersWithCc ⇒
        OrderPayments.createAll(ordersWithCc.map { case (order, cc) ⇒ makePayment(order, cc) })
      }
    }

    DBIO.sequence(Seq(insertCustomers, insertOrders, insertPayments))
  }

  def randomOrderState: Order.State = {
    val types = Order.State.types.filterNot(_ == Order.Cart)
    val index = Random.nextInt(types.size)
    types.drop(index).head
  }

  def randomString(len: Int) = Random.alphanumeric.take(len).mkString.toLowerCase
}
