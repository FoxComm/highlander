package utils

import models.Customers
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.UpdateReturning._

class SlickTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "supports update with returning query for a single column" in {
    val customer = Customers.save(Factories.customer.copy(firstName = "Jane")).run().futureValue
    val update = Customers.filter(_.id === 1).map(_.firstName).
      updateReturning(Customers.map(_.firstName), "Sally")

    val firstName = db.run(update.headOption).futureValue.get
    firstName must === ("Sally")
  }

  "supports update with returning query for a multiple columns" in {
    val customer = Customers.save(Factories.customer.copy(firstName = "Jane")).run().futureValue
    val update = Customers.filter(_.id === 1).map { c ⇒ (c.firstName, c.lastName) }.
      updateReturning(Customers.map { c ⇒ (c.firstName, c.lastName) }, ("Sally", "Doe"))

    val names = db.run(update.headOption).futureValue.get
    names must === (("Sally", "Doe"))
  }

  "supports update with returning query for mapping to a new model" in {
    val (customer, updatedCustomer) = db.run(for {
      customer ← Customers.save(Factories.customer.copy(firstName = "Jane"))
      updatedCustomer ← Customers.filter(_.id === 1).map(_.firstName).
        updateReturning(Customers.map(identity), "Sally").headOption
    } yield (customer, updatedCustomer.get)).futureValue

    customer must !== (updatedCustomer)
    updatedCustomer.firstName must === ("Sally")
  }

  "supports update with returning query for mapping to a new model for multiple columns" in {
    val (customer, updatedCustomer) = db.run(for {
      customer ← Customers.save(Factories.customer.copy(firstName = "Jane"))
      updatedCustomer ← Customers.filter(_.id === 1).map{c ⇒ (c.firstName, c.lastName) }.
        updateReturning(Customers.map(identity), ("Sally", "Doe")).headOption
    } yield (customer, updatedCustomer.get)).futureValue

    customer must !== (updatedCustomer)
    updatedCustomer.firstName must === ("Sally")
    updatedCustomer.lastName must === ("Doe")
  }
}
