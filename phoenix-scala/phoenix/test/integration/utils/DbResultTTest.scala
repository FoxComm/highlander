package utils

import cats.implicits._
import core.failures.GeneralFailure
import phoenix.utils.seeds.Factories
import testutils._
import core.db._

import scala.concurrent.ExecutionContext

class DbResultTTest extends TestBase with DbTestSupport with CatsHelpers with GimmeSupport {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "DbResultT" - {
    "when we lift (do you even?)" - {
      "succeeds when everything is Either.right" in {

        val transformer = for {
          _ ← DbResultT.fromEither(Either.right(Factories.rma))
          _ ← DbResultT.good(Factories.rma)
          c ← DbResultT.good(Factories.address)
        } yield c

        val result = db.run(transformer.runEmptyA.value).futureValue
        result mustBe 'right
        result.rightVal.zip must === (Factories.address.zip)
      }

      "fails when anything is Either.left" in {
        val failure = GeneralFailure("¬(monads)")

        val transformer = for {
          _ ← DbResultT.fromEither(Either.right(Factories.rma))
          _ ← DbResultT.failures[Unit](failure.single)
          c ← DbResultT.good(Factories.address)
        } yield c

        db.run(transformer.runEmptyA.value).futureValue.leftVal.head must === (failure)
      }
    }
  }
}
