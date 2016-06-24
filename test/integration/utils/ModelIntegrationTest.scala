package utils

import scala.concurrent.ExecutionContext.Implicits.global

import failures.{DatabaseFailure, GeneralFailure, StateTransitionNotAllowed}
import models.customer.{Customer, Customers}
import models.location.Addresses
import models.order.{Order, Orders}
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class ModelIntegrationTest extends IntegrationTestBase {

  "New model create" - {
    "validates model" in {
      val failures = leftValue(
          Addresses.create(Factories.address.copy(zip = "totallyNotAValidZip")).run().futureValue)
      failures must === (
          GeneralFailure("zip must fully match regular expression '^\\d{5}(?:\\d{4})?$'").single)
    }

    "sanitizes model" in {
      val result = (for {
        customer ← * <~ Customers.create(Factories.customer)
        address ← * <~ Addresses.create(
                     Factories.address.copy(zip = "123-45", customerId = customer.id))
      } yield address).gimme
      result.zip must === ("12345")
    }

    "catches exceptions from DB" in {
      val result = (for {
        customer ← * <~ Customers.create(Factories.customer)
        original ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
        copycat  ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      } yield copycat).runTxn().futureValue
      result.leftVal must === (
          DatabaseFailure(
              "ERROR: duplicate key value violates unique constraint \"address_shipping_default_idx\"\n" +
                "  Detail: Key (customer_id, is_default_shipping)=(1, t) already exists.").single)
    }

    "fails if model already exists" in {
      val orig = Customers.create(Factories.customer).gimme
      Customers.create(orig.copy(name = Some("Derp"))).run().futureValue mustBe 'left
      Customers.gimme must === (Seq(orig))
    }
  }

  "Model delete" - {
    "returns value for successful delete" in {
      val customer = Customers.create(Factories.customer).gimme
      val success  = "Success"
      val failure  = (id: Customer#Id) ⇒ GeneralFailure("Should not happen")
      val delete   = Customers.deleteById(customer.id, DbResult.good(success), failure).gimme
      delete must === (success)
    }

    "returns failure for unsuccessful delete" in {
      val success = DbResult.good("Should not happen")
      val failure = (id: Customer#Id) ⇒ GeneralFailure("Boom")
      val delete  = Customers.deleteById(13, success, failure).run().futureValue
      leftValue(delete) must === (failure(13).single)
    }
  }

  "Model update" - {
    "model decides if it can be updated successfully" in {
      val origin      = Factories.order
      val destination = origin.copy(customerId = 123)
      origin.updateTo(destination).rightVal must === (destination)
    }

    "model refuses to update if FSM check fails" in {
      val origin      = Factories.order
      val destination = origin.copy(state = Order.Cart)
      val failure     = leftValue(origin.updateTo(destination))
      failure must === (
          StateTransitionNotAllowed(origin.state, destination.state, origin.refNum).single)
    }

    "must update model successfully" in {
      val customer = Customers.create(Factories.customer).gimme
      customer.isNew must === (false)
      val updated = Customers.update(customer, customer.copy(name = Some("Derp"))).gimme
      Customers.findOneById(customer.id).run().futureValue.value must === (updated)
    }

    "must run FSM check if applicable" in {
      val order = Orders.create(Factories.order).run.gimme
      order.isNew must === (false)
      val updateFailure =
        leftValue(Orders.update(order, order.copy(state = Order.Cart)).run().futureValue)
      updateFailure must === (
          StateTransitionNotAllowed(order.state, Order.Cart, order.refNum).single)
      Orders.gimme.headOption.value must === (order)
    }

    "won't update unsaved model" in {
      Orders.update(Factories.order, Factories.order).run().futureValue mustBe 'left
    }
  }

  "Model save" - {
    "saves new model" in {
      Customers.gimme mustBe empty
      val customer = Customers.create(Factories.customer).gimme
      Customers.gimme must === (Seq(customer))
    }

    "updates old model" in {
      val orig = Customers.create(Factories.customer).gimme
      val copy = orig.copy(name = Some("Derp"))
      Customers.update(orig, copy).run().futureValue mustBe 'right
      Customers.gimme must === (Seq(copy))
    }
  }
}
