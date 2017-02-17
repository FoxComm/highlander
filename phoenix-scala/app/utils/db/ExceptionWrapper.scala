package utils.db

import cats.data.{StateT, Xor, XorT}
import failures.{DatabaseFailure, Failures}
import slick.dbio.Effect.All
import slick.dbio.{DBIO, DBIOAction, NoStream}
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
    dbresult.transformF { fsa ⇒
      val v: DBIO[Failures Xor (List[UIInfo], A)] =
        fsa.value.asTry.map { // Try has no fold? Also why do we have to hint the type here?
          case Success(x) ⇒ x
          case Failure(e) ⇒ Xor.left(DatabaseFailure(e.getMessage).single)
        }
      XorT(v)
    }
  }

}
