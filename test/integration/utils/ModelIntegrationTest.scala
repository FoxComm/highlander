package utils

import scala.concurrent.ExecutionContext.Implicits.global

import models._
import services.{GeneralFailure, DatabaseFailure, StatusTransitionNotAllowed}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.DbResult
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._

class ModelIntegrationTest extends IntegrationTestBase {

  "New model create" - {
    "validates model" in {
      val failures = leftValue(Addresses.create(Factories.address.copy(zip = "totallyNotAValidZip")).run().futureValue)
      failures must === (GeneralFailure("zip must fully match regular expression '^\\d{5}(?:\\d{4})?$'").single)
    }

    "sanitizes model" in {
      val result = (for {
        customer ← Customers.saveNew(Factories.customer)
        address ← Addresses.create(Factories.address.copy(zip = "123-45", customerId = customer.id))
      } yield address).run().futureValue.rightVal
      result.zip must === ("12345")
    }

    "catches exceptions from DB" in {
      val result = (for {
        customer ← Customers.saveNew(Factories.customer)
        original ← Addresses.create(Factories.address.copy(customerId = customer.id))
        copycat ← Addresses.create(Factories.address.copy(customerId = customer.id))
      } yield copycat).run().futureValue
      leftValue(result) must === (DatabaseFailure(
        "ERROR: duplicate key value violates unique constraint \"address_shipping_default_idx\"\n" +
        "  Detail: Key (customer_id, is_default_shipping)=(1, t) already exists.").single)
    }

    "fails if model already exists" in {
      val orig = Customers.create(Factories.customer).run().futureValue.rightVal
      Customers.create(orig.copy(name = Some("Derp"))).run().futureValue mustBe 'left
      Customers.result.run().futureValue must === (Seq(orig))
    }
  }

  "Model delete" - {
    "returns value for successful delete" in {
      val customer = Customers.create(Factories.customer).run().futureValue.rightVal
      val success = "Success"
      val failure = DbResult.failure(GeneralFailure("Should not happen"))
      val delete = Customers.deleteById(customer.id, DbResult.good(success), failure).run().futureValue.rightVal
      delete must === (success)
    }

    "returns failure for unsuccessful delete" in {
      val success = DbResult.good("Should not happen")
      val failure = GeneralFailure("Boom")
      val delete = Customers.deleteById(13, success, DbResult.failure(failure)).run().futureValue
      leftValue(delete) must === (failure.single)
    }
  }

  "Model update" - {
    "model decides if it can be updated successfully" in {
      val origin = Factories.order
      val destination = origin.copy(customerId = 123)
      origin.updateTo(destination).rightVal must === (destination)
    }

    "model refuses to update if FSM check fails" in {
      val origin = Factories.order
      val destination = origin.copy(status = Order.Cart)
      val failure = leftValue(origin.updateTo(destination))
      failure must === (StatusTransitionNotAllowed(origin.status,destination.status, origin.refNum).single)
    }

    "must update model successfully" in {
      val customer = Customers.create(Factories.customer).run().futureValue.rightVal
      customer.isNew must === (false)
      val updated = Customers.update(customer, customer.copy(name = Some("Derp"))).run().futureValue.rightVal
      Customers.findOneById(customer.id).run().futureValue.value must === (updated)
    }

    "must run FSM check if applicable" in {
      val order = Orders.create(Factories.order).run.futureValue.rightVal
      order.isNew must === (false)
      val updateFailure = leftValue(Orders.update(order, order.copy(status = Order.Cart)).run().futureValue)
      updateFailure must === (StatusTransitionNotAllowed(order.status, Order.Cart, order.refNum).single)
      Orders.result.run().futureValue.headOption.value must === (order)
    }
  }

  "Model save" - {
    "saves new model" in {
      Customers.result.run().futureValue mustBe empty
      val customer = Customers.create(Factories.customer).run().futureValue.rightVal
      Customers.result.run().futureValue must === (Seq(customer))
    }

    "updates old model" in {
      val orig = Factories.customer
      val id = Customers.create(orig).run().futureValue.rightVal.id
      val copy = orig.copy(id = id, name = Some("Derp"))
      Customers.update(orig, copy).run().futureValue mustBe 'right
      Customers.result.run().futureValue must === (Seq(copy))
    }
  }

}
