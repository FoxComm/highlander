package utils

import scala.concurrent.Future

import util.TestBase
import utils.jdbc._
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

class JdbcTest extends TestBase {
  import scala.concurrent.ExecutionContext.Implicits.global

  "withUniqueConstraint" in {
    val state = new PSQLState("23505")
    val ex = withUniqueConstraint {
      val msg = "ERROR: duplicate key value violates unique constraint 'person already has two legs"
      Future.failed(new PSQLException(msg, state))
    }.failed.futureValue

    ex match {
      case _: jdbc.RecordNotUnique ⇒
      case _ ⇒ fail("throwable was not RecordNotUnique")
    }
  }
}
