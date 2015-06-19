package utils

import models._

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

object Seeds {
  val today = new DateTime

  case class TheWorld(customer: Customer,order: Order, address: Address, cc: CreditCardGateway,
                      storeAdmin: StoreAdmin)

  def run(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val db = Database.forURL("jdbc:postgresql://localhost/phoenix_development?user=phoenix", driver = "slick.driver.PostgresDriver")

    val s = TheWorld(
      customer = Factories.customer,
      storeAdmin = Factories.storeAdmin,
      order = Factories.order,
      address = Factories.address,
      cc = Factories.creditCard
    )

    val failures = List(s.customer.validate, s.storeAdmin.validate, s.order.validate, s.address.validate, s.cc.validate).
      filterNot(_.isValid)

    if (failures.nonEmpty)
      throw new Exception(failures.map(_.messages).mkString("\n"))

    val actions = for {
      customer ← (Customers.returningId += s.customer).map(id => s.customer.copy(id = id))
      storeAdmin ← (StoreAdmins.returningId += s.storeAdmin).map(id => s.storeAdmin.copy(id = id))
      order ← Orders.save(s.order.copy(customerId = customer.id))
      address ← Addresses.save(s.address.copy(customerId = customer.id))
      gateway ← CreditCardGateways.save(s.cc.copy(customerId = customer.id))
    } yield (customer, order, address, gateway)

    Await.result(actions.run(), 1.second)
  }

  object Factories {
    def customer = Customer(email = "yax@yax.com", password = "password", firstName = "Yax", lastName = "Fuentes")

    def storeAdmin = StoreAdmin(email = "admin@admin.com", password = "password", firstName = "Frankly", lastName = "Admin")

    def order = Order(customerId = 0)

    def address =
      Address(customerId = 0, stateId = 1, name = "Home", street1 = "555 E Lake Union St.",
        street2 = None, city = "Seattle", zip = "12345")

    def creditCard =
      CreditCardGateway(customerId = 0, gatewayCustomerId = "", lastFour = "4242",
        expMonth = today.getMonthOfYear, expYear = today.getYear + 2)
  }
}
