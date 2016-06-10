package utils

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.Xor
import failures.GeneralFailure
import models.customer.Customers
import models.location.Addresses
import models.order.Orders
import slick.driver.PostgresDriver.api._
import util.CustomMatchers._
import util.SlickSupport.implicits._
import util._
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class DbResultTTest extends TestBase with DbTestSupport with CatsHelpers {

  "DbResultT" - {
    "when we lift (do you even?)" - {
      "succeeds when everything is Xor.Right" in {
        val transformer = for {
          a ← DbResultT.fromXor(Xor.right(Factories.order))
          b ← DbResultT.rightLift(Factories.rma)
          c ← DbResultT.rightLift(Factories.address)
        } yield c

        val result = db.run(transformer.value).futureValue
        result mustBe 'right
        result.rightVal.zip must ===(Factories.address.zip)
      }

      "fails when anything is Xor.Left" in {
        val failure = GeneralFailure("¬(monads)")

        val transformer = for {
          a ← DbResultT.fromXor(Xor.right(Factories.order))
          b ← DbResultT.leftLift[Unit](failure.single)
          c ← DbResultT.rightLift(Factories.address)
        } yield c

        val result = db.run(transformer.value).futureValue
        result mustBe 'left
        result.leftVal.head must ===(failure)
      }
    }

    "using toXorT on a DbResult[A]" - {
      "succeeds when everything is Xor.Right" in {
        val transformer = for {
          customer ← Customers.create(Factories.customer).toXorT
          order    ← Orders.create(Factories.order.copy(customerId = customer.id)).toXorT
        } yield (customer, order)

        val result = db.run(transformer.value.transactionally).futureValue
        result mustBe 'right
      }

      "fails when anything is Xor.Left" in {
        val transformer = for {
          customer ← Customers.create(Factories.customer).toXorT
          // force validation failure
          address ← Addresses
                     .create(Factories.address.copy(name = "", customerId = customer.id))
                     .toXorT
        } yield (customer, address)

        val result = db.run(transformer.value.transactionally).futureValue
        result mustBe 'left
        result.leftVal must includeFailure("name must not be empty")

        // creates the customer
        Customers.length.result.futureValue must ===(1)

        // won't create address
        Addresses.length.result.futureValue must ===(0)
      }
    }
  }
}
