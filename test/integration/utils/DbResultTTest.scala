package utils

import cats.data.{XorT, Xor}
import cats.implicits._
import services.{GeneralFailure, Failures}
import util.{DbTestSupport, TestBase, CatsHelpers}
import utils.Seeds.Factories
import slick.driver.PostgresDriver._
import slick.driver.PostgresDriver.api._
import models.{Customers, Orders, Addresses}
import utils.Slick.implicits._
import util.SlickSupport.implicits._

class DbResultTTest
  extends TestBase
  with DbTestSupport
  with CatsHelpers{

  import scala.concurrent.ExecutionContext.Implicits.global
  import DbResultT.implicits._
  import util.CustomMatchers._

  "DbResultT" - {
    "when we lift (do you even?)" - {
      "succeeds when everything is Xor.Right" in {
        val transformer = for {
          a ← DbResultT.fromXor(Xor.right(Factories.order))
          b ← DbResultT.rightLift(Factories.customer)
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
          customer  ← Customers.create(Factories.customer).toXorT
          order     ← Orders.create(Factories.order.copy(customerId = customer.id)).toXorT
        } yield (customer, order)

        val result = db.run(transformer.value.transactionally).futureValue
        result mustBe 'right
      }

      "fails when anything is Xor.Left" in {
        val transformer = for {
          customer  ← Customers.create(Factories.customer).toXorT
          // force validation failure
          address   ← Addresses.create(Factories.address.copy(name = "", customerId = customer.id)).toXorT
        } yield (customer, address)

        val result = db.run(transformer.value.transactionally).futureValue
        result mustBe 'left
        result.leftVal must includeFailure("name must not be empty")

        // creates the customer
        Customers.length.result.futureValue must === (1)

        // won't create address
        Addresses.length.result.futureValue must === (0)
      }
    }
  }
}
