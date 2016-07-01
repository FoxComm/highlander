package utils.db

import failures.DatabaseFailure
import slick.dbio.DBIO
import utils.aliases._

object ExceptionWrapper {

  def wrapDbio[A](dbio: DBIO[A])(implicit ec: EC): DbResultT[A] = {
    import scala.util.{Failure, Success}

    dbio.asTry.toXor.flatMap {
      case Success(value) ⇒ DbResultT.good(value)
      case Failure(e)     ⇒ DbResultT.failure(DatabaseFailure(e.getMessage))
    }
  }

  def wrapDbResultT[A](dbresult: DbResultT[A])(implicit ec: EC): DbResultT[A] = {
    import scala.util.{Failure, Success}

    dbresult.value.asTry.toXor.flatMap {
      case Success(value) ⇒ DbResultT.fromXor(value)
      case Failure(e)     ⇒ DbResultT.failure(DatabaseFailure(e.getMessage))
    }
  }
}
