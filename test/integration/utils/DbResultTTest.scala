package utils

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.Xor
import failures.GeneralFailure
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class DbResultTTest extends TestBase with DbTestSupport with CatsHelpers with GimmeSupport {

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
        result.rightVal.zip must === (Factories.address.zip)
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
        result.leftVal.head must === (failure)
      }
    }
  }
}
