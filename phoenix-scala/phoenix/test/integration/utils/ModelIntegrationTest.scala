package utils

import cats.implicits._
import core.failures.{DatabaseFailure, Failures, GeneralFailure}
import phoenix.failures.StateTransitionNotAllowed
import phoenix.models.account._
import phoenix.models.cord.Order.Shipped
import phoenix.models.cord._
import phoenix.models.customer._
import phoenix.models.location.Addresses
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.BakedFixtures
import core.db._

class ModelIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "New model create" - {
    "validates model" in {
      val failures =
        Addresses.create(Factories.address.copy(zip = "totallyNotAValidZip")).gimmeFailures
      failures must === (
        GeneralFailure("zip must fully match regular expression '^\\d{5}(?:\\d{4})?$'").single)
    }

    "sanitizes model" in {
      val result = (for {
        account  ← * <~ Accounts.create(Account())
        scope    ← * <~ Scopes.forOrganization(TENANT)
        customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
        _ ← * <~ CustomersData.create(
             CustomerData(userId = customer.id, accountId = account.id, scope = scope))
        address ← * <~ Addresses.create(
                   Factories.address.copy(zip = "123-45", accountId = customer.accountId))
      } yield address).gimme
      result.zip must === ("12345")
    }

    "catches exceptions from DB" in {
      val account = Accounts.create(Account()).gimme

      val result: Failures = (for {
        customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
        scope    ← * <~ Scopes.forOrganization(TENANT)
        _ ← * <~ CustomersData.create(
             CustomerData(userId = customer.id, accountId = account.id, scope = scope))
        _       ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId))
        copycat ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId))
      } yield copycat).gimmeTxnFailures

      result.toList.onlyElement.description contains
      s"Key (account_id, is_default_shipping)=(${account.id}, t) already exists." must === (true)
    }

    "fails if model already exists" in {
      val account = Accounts.create(Account()).gimme
      val orig    = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      Users.create(orig.copy(name = Some("Derp"))).gimmeFailures
      Users.gimme must === (Seq(orig))
    }
  }

  "Model delete" - {
    "returns value for successful delete" in {
      val account  = Accounts.create(Account()).gimme
      val customer = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      val success  = "Success"
      val failure  = (_: User#Id) ⇒ GeneralFailure("Should not happen")
      val delete   = Users.deleteById(customer.id, success.pure[DbResultT], failure).gimme
      delete must === (success)
    }

    "returns failure for unsuccessful delete" in {
      val success = "Should not happen".pure[DbResultT]
      val failure = (_: User#Id) ⇒ GeneralFailure("Boom")
      val delete  = Users.deleteById(13, success, failure).gimmeFailures
      delete must === (failure(13).single)
    }
  }

  "Model update" - {
    "model decides if it can be updated successfully" in new Customer_Seed {
      implicit val au = customerAuthData
      val origin      = Factories.order(Scope.current)
      val destination = origin.copy(accountId = 123)
      origin.updateTo(destination).rightVal must === (destination)
    }

    "model refuses to update if FSM check fails" in new Customer_Seed {
      implicit val au = customerAuthData
      val origin      = Factories.order(Scope.current)
      val destination = origin.copy(state = Shipped)
      val failure     = leftValue(origin.updateTo(destination))
      failure must === (StateTransitionNotAllowed(origin.state, destination.state, origin.refNum).single)
    }

    "must update model successfully" in {
      val account  = Accounts.create(Account()).gimme
      val customer = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      customer.isNew must === (false)
      val updated = Users.update(customer, customer.copy(name = Some("Derp"))).gimme
      Users.findOneById(customer.id).run().futureValue.value must === (updated)
    }

    "must run FSM check if applicable" in new Order_Baked {

      val failure = Orders.update(order, order.copy(state = Shipped)).gimmeFailures
      failure must === (StateTransitionNotAllowed(order.state, Shipped, order.refNum).single)
      Orders.findOneByRefNum(order.refNum).gimme.value must === (order)
    }

    "won't update unsaved model" in new Customer_Seed {
      implicit val au = customerAuthData
      Orders.update(Factories.order(Scope.current), Factories.order(Scope.current)).gimmeFailures
    }
  }

  "Model save" - {
    "saves new model" in {
      Users.gimme mustBe empty
      val account  = Accounts.create(Account()).gimme
      val customer = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      Users.gimme must === (Seq(customer))
    }

    "updates old model" in {
      val account = Accounts.create(Account()).gimme
      val orig    = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      val copy    = orig.copy(name = Some("Derp"))
      Users.update(orig, copy).gimme
      Users.gimme must === (Seq(copy))
    }
  }
}
