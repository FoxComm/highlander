package utils

import cats.implicits._
import models.account._
import slick.driver.PostgresDriver.api._
import testutils._
import utils.db.UpdateReturning._
import utils.db._
import utils.seeds.Seeds.Factories

class SlickTest extends IntegrationTestBase {

  "supports update with returning query for a single column" in {
    val account = Accounts.create(Account()).gimme
    val customer =
      Users.create(Factories.customer.copy(accountId = account.id, name = "Jane".some)).gimme
    val update =
      Users.filter(_.id === 1).map(_.name).updateReturningHead(Users.map(_.name), "Sally".some)

    val firstName = update.gimme
    firstName must === ("Sally".some)
  }

  "supports update with returning query for a multiple columns" in {
    val account = Accounts.create(Account()).gimme
    Users.create(Factories.customer.copy(accountId = account.id, name = "Jane".some)).gimme
    val update = Users
      .filter(_.id === 1)
      .map { _.name }
      .updateReturningHead(Users.map { _.name }, ("Sally".some))

    val names = update.gimme
    names must === ("Sally".some)
  }

  "supports update with returning query for mapping to a new model" in {
    val (customer, updatedCustomer) = (for {
      account ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(
        Factories.customer.copy(accountId = account.id, name = "Jane".some))
      updatedCustomer ← * <~ Users
        .filter(_.id === 1)
        .map(_.name)
        .updateReturningHead(Users.map(identity), "Sally".some)
    } yield (customer, updatedCustomer.value)).gimme

    customer must !==(updatedCustomer)
    updatedCustomer.name must === ("Sally".some)
  }

  "supports update with returning query for mapping to a new model for multiple columns" in {
    val (customer, updatedCustomer) = (for {
      account ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(
        Factories.customer.copy(accountId = account.id, name = "Jane".some))
      updatedCustomer ← * <~ Users
        .filter(_.id === 1)
        .map(_.name)
        .updateReturningHead(Users.map(identity), "Sally".some)
    } yield (customer, updatedCustomer.value)).gimme

    customer must !==(updatedCustomer)
    updatedCustomer.name must === ("Sally".some)
  }
}
