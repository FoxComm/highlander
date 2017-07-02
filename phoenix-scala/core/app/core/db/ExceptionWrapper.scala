package core.db

import cats.data.EitherT
import cats.implicits._
import core.failures.{DatabaseFailure, Failures}
import slick.dbio.DBIO

object ExceptionWrapper {

  def wrapDbio[A](dbio: DBIO[A])(implicit ec: EC): DbResultT[A] = { // TODO: can we get rid of that? @michalrus
    import scala.util.{Failure, Success}

    dbio.asTry.dbresult.flatMap {
      case Success(value) ⇒ value.pure[DbResultT]
      case Failure(e)     ⇒ DbResultT.failure(DatabaseFailure(e.getMessage))
    }
  }

  def wrapDbResultT[A](dbresult: DbResultT[A])(implicit ec: EC): DbResultT[A] = { // TODO: can we get rid of that? @michalrus
    import scala.util.{Failure, Success}
    dbresult.transformF { fsa ⇒
      val v: DBIO[Either[Failures, (List[MetaResponse], A)]] =
        fsa.value.asTry.map { // Try has no fold? Also why do we have to hint the type here?
          case Success(x) ⇒ x
          case Failure(e) ⇒ Either.left(DatabaseFailure(e.getMessage).single)
        }
      EitherT(v)
    }
  }

}
