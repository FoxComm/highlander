package utils

import failures.{DatabaseFailure, GeneralFailure}
import services.Result
import util.TestBase
import utils.jdbc._

class JdbcTest extends TestBase {
  import scala.concurrent.ExecutionContext.Implicits.global

  val imSorry =
    "ERROR: duplicate key value violates unique constraint 'person already has two legs"

  "swapDatabaseFailure" - {
    "replaces requested error" in {
      val fail = Result.failure(DatabaseFailure(imSorry))
      val result = swapDatabaseFailure(fail) {
        (NotUnique, GeneralFailure("IM AN OCTOPUS"))
      }.futureValue
      result.leftVal must === (GeneralFailure("IM AN OCTOPUS").single)
    }

    "ignores non-db errors" in {
      val fail = Result.failure(GeneralFailure(imSorry))
      val result = swapDatabaseFailure(fail) {
        (NotUnique, GeneralFailure("IM AN OCTOPUS"))
      }.futureValue
      result.leftVal must === (GeneralFailure(imSorry).single)
    }
  }
}
