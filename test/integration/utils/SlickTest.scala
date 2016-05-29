package utils

import cats.implicits._
import models.customer.Customers
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import util.SlickSupport.implicits._
import utils.db.DbResultT._
import utils.db.UpdateReturning._
import utils.db._
import utils.seeds.Seeds.Factories

class SlickTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "supports update with returning query for a single column" in {
    val customer =
      Customers.create(Factories.customer.copy(name = "Jane".some)).run().futureValue.rightVal
    val update = Customers
      .filter(_.id === 1)
      .map(_.name)
      .updateReturningHead(Customers.map(_.name), "Sally".some)

    val firstName = update.futureValue.rightVal
    firstName must ===("Sally".some)
  }

  "supports update with returning query for a multiple columns" in {
    Customers.create(Factories.customer.copy(name = "Jane".some)).run().futureValue.rightVal
    val update = Customers
      .filter(_.id === 1)
      .map { c ⇒
        (c.name, c.hashedPassword)
      }
      .updateReturningHead(Customers.map { c ⇒
        (c.name, c.hashedPassword)
      }, ("Sally".some, "123qwe".some))

    val names = update.futureValue.rightVal
    names must ===(("Sally".some, "123qwe".some))
  }

  "supports update with returning query for mapping to a new model" in {
    val (customer, updatedCustomer) = (for {
      customer ← * <~ Customers.create(Factories.customer.copy(name = "Jane".some))
      updatedCustomer ← * <~ Customers
                         .filter(_.id === 1)
                         .map(_.name)
                         .updateReturningHead(Customers.map(identity), "Sally".some)
    } yield (customer, updatedCustomer.value)).runTxn().futureValue.rightVal

    customer must !==(updatedCustomer)
    updatedCustomer.name must ===("Sally".some)
  }

  "supports update with returning query for mapping to a new model for multiple columns" in {
    val (customer, updatedCustomer) = (for {
      customer ← * <~ Customers.create(Factories.customer.copy(name = "Jane".some))
      updatedCustomer ← * <~ Customers
                         .filter(_.id === 1)
                         .map { c ⇒
                           (c.name, c.hashedPassword)
                         }
                         .updateReturningHead(Customers.map(identity),
                                              ("Sally".some, "123qwe".some))
    } yield (customer, updatedCustomer.value)).runTxn().futureValue.rightVal

    customer must !==(updatedCustomer)
    updatedCustomer.name must ===("Sally".some)
    updatedCustomer.hashedPassword must ===("123qwe".some)
  }
}
