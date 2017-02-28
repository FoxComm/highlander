package utils.db

import scala.collection.generic.CanBuildFrom
import scala.concurrent.Future

import cats.Functor
import cats.data.{Validated, Xor}
import failures.Failures
import slick.dbio.{DBIO, Effect, NoStream}
import slick.lifted.Query
import slick.profile.SqlAction
import utils.aliases.{EC, SF, SL}

trait BaseStar {

  // Potentially failing
  def <~[A](v: DBIO[Failures Xor A])(implicit ec: EC, f: Functor[DBIO]): DbResultT[A]
  def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A]
  def <~[A](v: Future[Failures Xor A]): DbResultT[A]
  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A]
  def <~[A](v: DbResultT[A])(implicit f: Functor[DBIO]): DbResultT[A]

  // Always good? Exceptions?..
  def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
    DbResultT(v.map(Xor.right))

  def <~[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromDbio(v)

  def <~[A](v: Future[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromFuture(v)

  def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
    DbResultT.pure(v)

  def <~[A, M[X] <: TraversableOnce[X]](v: M[DbResultT[A]])(
      implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]],
      ec: EC): DbResultT[M[A]] =
    DbResultT.sequence(v)

  def <~[A](v: Option[DbResultT[A]])(implicit ec: EC): DbResultT[Option[A]] =
    v.fold(DbResultT.none[A]) { dbresult ⇒
      dbresult.map(Some(_))
    }

  def <~[A, C[_]](query: Query[_, A, C])(implicit ec: EC, sl: SL, sf: SF): DbResultT[A] =
    DbResultT.nonEmptyQuery(query)
}
