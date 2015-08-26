package utils

import scala.concurrent.Future

import services.GeneralFailure
import util.TestBase
import utils.jdbc._
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

class JdbcTest extends TestBase {
  import scala.concurrent.ExecutionContext.Implicits.global

  "withUniqueConstraint" in {
    val state = new PSQLState("23505")
    val future = Future {
      val msg = "ERROR: duplicate key value violates unique constraint 'person already has two legs"
      throw new PSQLException(msg, state)
    }

    val or = withUniqueConstraint(future) { notUnique ⇒ GeneralFailure("record was not unique") }.futureValue
    or mustBe 'left

    or.fold(_.description,  _ ⇒ Seq("wrong")) must === (Seq("record was not unique"))
  }
}
