package utils

import cats.implicits._
import phoenix.models.account._
import phoenix.utils.seeds.Factories
import slick.jdbc.PostgresProfile.api._
import testutils._
import core.db.UpdateReturning._
import core.db._

class SlickTest extends IntegrationTestBase {

  "supports update with returning query for a single column" in {
    val account = Accounts.create(Account()).gimme
    val user =
      Users.create(Factories.customer.copy(accountId = account.id, name = "Jane".some)).gimme
    val update = Users
      .filter(_.id === user.id)
      .map(_.name)
      .updateReturningHead(Users.map(_.name), "Sally".some)

    val firstName = update.gimme
    firstName must === ("Sally".some)
  }

  "supports update with returning query for a multiple columns" in {
    val account = Accounts.create(Account()).gimme
    val user =
      Users.create(Factories.customer.copy(accountId = account.id, name = "Jane".some)).gimme
    val update = Users
      .filter(_.id === user.id)
      .map { _.name }
      .updateReturningHead(Users.map { _.name }, ("Sally".some))

    val names = update.gimme
    names must === ("Sally".some)
  }

  "supports update with returning query for mapping to a new model" in {
    val (customer, updatedCustomer) = (for {
      account  ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id, name = "Jane".some))
      updatedCustomer ← * <~ Users
                         .filter(_.id === customer.id)
                         .map(_.name)
                         .updateReturningHead(Users.map(identity), "Sally".some)
    } yield (customer, updatedCustomer)).gimme

    customer must !==(updatedCustomer)
    updatedCustomer.name must === ("Sally".some)
  }

  "supports update with returning query for mapping to a new model for multiple columns" in {
    val (customer, updatedCustomer) = (for {
      account  ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id, name = "Jane".some))
      updatedCustomer ← * <~ Users
                         .filter(_.id === customer.id)
                         .map(_.name)
                         .updateReturningHead(Users.map(identity), "Sally".some)
    } yield (customer, updatedCustomer)).gimme

    customer must !==(updatedCustomer)
    updatedCustomer.name must === ("Sally".some)
  }
}
