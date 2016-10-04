package utils

import failures.{DatabaseFailure, GeneralFailure, StateTransitionNotAllowed}
import models.cord.Order.Shipped
import models.cord._
import models.account._
import models.customer._
import models.location.Addresses
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class ModelIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "New model create" - {
    "validates model" in {
      val failures = leftValue(
          Addresses.create(Factories.address.copy(zip = "totallyNotAValidZip")).run().futureValue)
      failures must === (
          GeneralFailure("zip must fully match regular expression '^\\d{5}(?:\\d{4})?$'").single)
    }

    "sanitizes model" in {
      val result = (for {
        account  ← * <~ Accounts.create(Account())
        customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
        _        ← * <~ CustomersData.create(CustomerData(userId = customer.id, accountId = account.id))
        address ← * <~ Addresses.create(
                     Factories.address.copy(zip = "123-45", accountId = customer.accountId))
      } yield address).gimme
      result.zip must === ("12345")
    }

    "catches exceptions from DB" in {
      val result = (for {
        account  ← * <~ Accounts.create(Account())
        customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
        _        ← * <~ CustomersData.create(CustomerData(userId = customer.id, accountId = account.id))
        original ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId))
        copycat  ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId))
      } yield copycat).runTxn().futureValue
      result.leftVal must === (
          DatabaseFailure(
              "ERROR: duplicate key value violates unique constraint \"address_shipping_default_idx\"\n" +
                "  Detail: Key (account_id, is_default_shipping)=(1, t) already exists.").single)
    }

    "fails if model already exists" in {
      val account = Accounts.create(Account()).gimme
      val orig    = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      Users.create(orig.copy(name = Some("Derp"))).run().futureValue mustBe 'left
      Users.gimme must === (Seq(orig))
    }
  }

  "Model delete" - {
    "returns value for successful delete" in {
      val account  = Accounts.create(Account()).gimme
      val customer = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      val success  = "Success"
      val failure  = (id: User#Id) ⇒ GeneralFailure("Should not happen")
      val delete   = Users.deleteById(customer.accountId, DbResultT.good(success), failure).gimme
      delete must === (success)
    }

    "returns failure for unsuccessful delete" in {
      val success = DbResultT.good("Should not happen")
      val failure = (id: User#Id) ⇒ GeneralFailure("Boom")
      val delete  = Users.deleteById(13, success, failure).run().futureValue
      leftValue(delete) must === (failure(13).single)
    }
  }

  "Model update" - {
    "model decides if it can be updated successfully" in {
      val origin      = Factories.order
      val destination = origin.copy(accountId = 123)
      origin.updateTo(destination).rightVal must === (destination)
    }

    "model refuses to update if FSM check fails" in {
      val origin      = Factories.order
      val destination = origin.copy(state = Shipped)
      val failure     = leftValue(origin.updateTo(destination))
      failure must === (
          StateTransitionNotAllowed(origin.state, destination.state, origin.refNum).single)
    }

    "must update model successfully" in {
      val account  = Accounts.create(Account()).gimme
      val customer = Users.create(Factories.customer.copy(accountId = account.id)).gimme
      customer.isNew must === (false)
      val updated = Users.update(customer, customer.copy(name = Some("Derp"))).gimme
      Users.findOneById(customer.accountId).run().futureValue.value must === (updated)
    }

    "must run FSM check if applicable" in new Order_Baked {

      val failure = Orders.update(order, order.copy(state = Shipped)).run().futureValue.leftVal
      failure must === (StateTransitionNotAllowed(order.state, Shipped, order.refNum).single)
      Orders.findOneByRefNum(order.refNum).gimme.value must === (order)
    }

    "won't update unsaved model" in {
      Orders.update(Factories.order, Factories.order).run().futureValue mustBe 'left
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
      Users.update(orig, copy).run().futureValue mustBe 'right
      Users.gimme must === (Seq(copy))
    }
  }
}
