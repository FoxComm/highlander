package utils

import models.Customers
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import util.SlickSupport.implicits._
import cats.implicits._

class SlickTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "supports update with returning query for a single column" in {
    val customer = Customers.save(Factories.customer.copy(name = "Jane".some)).run().futureValue
    val update = Customers.filter(_.id === 1).map(_.name).
      updateReturning(Customers.map(_.name), "Sally".some)

    val firstName = update.one.futureValue.value
    firstName must === ("Sally".some)
  }

  "supports update with returning query for a multiple columns" in {
    val customer = Customers.save(Factories.customer.copy(name = "Jane".some)).run().futureValue
    val update = Customers.filter(_.id === 1).map { c ⇒ (c.name, c.password) }.
      updateReturning(Customers.map { c ⇒ (c.name, c.password) }, ("Sally".some, "123qwe".some))

    val names = update.one.futureValue.value
    names must === (("Sally".some, "123qwe".some))
  }

  "supports update with returning query for mapping to a new model" in {
    val (customer, updatedCustomer) = db.run(for {
      customer ← Customers.save(Factories.customer.copy(name = "Jane".some))
      updatedCustomer ← Customers.filter(_.id === 1).map(_.name).
        updateReturning(Customers.map(identity), "Sally".some).headOption
    } yield (customer, updatedCustomer.value)).futureValue

    customer must !== (updatedCustomer)
    updatedCustomer.name must === ("Sally".some)
  }

  "supports update with returning query for mapping to a new model for multiple columns" in {
    val (customer, updatedCustomer) = db.run(for {
      customer ← Customers.save(Factories.customer.copy(name = "Jane".some))
      updatedCustomer ← Customers.filter(_.id === 1).map{c ⇒ (c.name, c.password) }.
        updateReturning(Customers.map(identity), ("Sally".some, "123qwe".some)).headOption
    } yield (customer, updatedCustomer.value)).futureValue

    customer must !== (updatedCustomer)
    updatedCustomer.name must === ("Sally".some)
    updatedCustomer.password must === ("123qwe".some)
  }
}
