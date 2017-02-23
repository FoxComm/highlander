package utils

import scala.concurrent.ExecutionContext

import cats.data._
import cats.implicits._
import failures.GeneralFailure
import testutils._
import utils.aliases._
import utils.db._
import utils.seeds.Seeds.Factories

class DbResultTTest extends TestBase with DbTestSupport with CatsHelpers with GimmeSupport {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "DbResultT" - {
    "when we lift (do you even?)" - {
      "succeeds when everything is Xor.Right" in {

        val transformer = for {
          _ ← DbResultT.fromXor(Xor.right(Factories.rma))
          _ ← DbResultT.good(Factories.rma)
          c ← DbResultT.good(Factories.address)
        } yield c

        val result = db.run(transformer.runEmptyA.value).futureValue
        result mustBe 'right
        result.rightVal.zip must === (Factories.address.zip)
      }

      "fails when anything is Xor.Left" in {
        val failure = GeneralFailure("¬(monads)")

        val transformer = for {
          _ ← DbResultT.fromXor(Xor.right(Factories.rma))
          _ ← DbResultT.failures[Unit](failure.single)
          c ← DbResultT.good(Factories.address)
        } yield c

        db.run(transformer.runEmptyA.value).futureValue.leftVal.head must === (failure)
      }
    }
  }
}
