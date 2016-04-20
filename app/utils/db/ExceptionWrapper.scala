package utils.db

import failures.DatabaseFailure
import slick.dbio.DBIO
import utils.aliases._

object ExceptionWrapper {

  def wrapDbio[A](dbio: DBIO[A])(implicit ec: EC): DbResult[A] = {
    import scala.util.{Failure, Success}

    dbio.asTry.flatMap {
      case Success(value) ⇒ DbResult.good(value)
      case Failure(e) ⇒ DbResult.failure(DatabaseFailure(e.getMessage))
    }
  }

  def wrapDbResult[A](dbresult: DbResult[A])(implicit ec: EC): DbResult[A] = {
    import scala.util.{Failure, Success}

    dbresult.asTry.flatMap {
      case Success(value) ⇒ lift(value)
      case Failure(e) ⇒ DbResult.failure(DatabaseFailure(e.getMessage))
    }
  }
}
