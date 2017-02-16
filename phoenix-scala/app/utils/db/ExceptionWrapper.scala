package utils.db

import cats.data.{StateT, XorT}
import failures.DatabaseFailure
import slick.dbio.DBIO
import utils.aliases._

object ExceptionWrapper {

  def wrapDbio[A](dbio: DBIO[A])(implicit ec: EC): DbResultT[A] = { // TODO: can we get rid of that? @michalrus
    import scala.util.{Failure, Success}

    dbio.asTry.dbresult.flatMap {
      case Success(value) ⇒ DbResultT.good(value)
      case Failure(e)     ⇒ DbResultT.failure(DatabaseFailure(e.getMessage))
    }
  }

  def wrapDbResultT[A](dbresult: DbResultT[A])(implicit ec: EC): DbResultT[A] = { // TODO: can we get rid of that? @michalrus
    import scala.util.{Failure, Success}

    val x = dbresult.transformF(old ⇒ 5)

    StateT(s ⇒
          dbresult.run(s).value.asTry.flatMap {
        case Success(value) ⇒ DbResultT.fromXor(value)
        case Failure(e)     ⇒ DbResultT.failure(DatabaseFailure(e.getMessage))
    })

    dbresult.value.asTry.dbresult.flatMap {
      ???
    }
  }
}
