package utils

import org.postgresql.util.{PSQLException, PSQLState}
import scala.language.implicitConversions
import scala.concurrent.{ExecutionContext, Future}
import utils.Validation.Result.Failure
import org.scalactic.{Or, Good, Bad}

object jdbc {
  final case class RecordNotUnique(p: PSQLException)
    extends PSQLException(p.getMessage, new PSQLState(p.getSQLState))

  final case class FailsConstraintCheck(p: PSQLException)
    extends PSQLException(p.getMessage, new PSQLState(p.getSQLState))

  val uniqueConstraintError = """ERROR: duplicate key value violates unique constraint""".r

  def withUniqueConstraint[A](f: ⇒ Future[A])(implicit ec: ExecutionContext): Future[A] = {
    f.recoverWith {
      case e: PSQLException if uniqueConstraintError.findFirstIn(e.getMessage).nonEmpty ⇒
        Future.failed(RecordNotUnique(e))
      case t ⇒
        Future.failed(t)
    }
  }
}
