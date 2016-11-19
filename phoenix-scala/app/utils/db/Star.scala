package utils.db

import scala.collection.generic.CanBuildFrom
import scala.concurrent.Future

import cats.data.{Validated, Xor}
import failures.Failures
import slick.dbio._
import slick.profile.SqlAction
import utils.aliases.EC

object * {
  def <~[A](v: DBIO[Failures Xor A]): DbResultT[A] =
    DbResultT(v)

  def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
    DbResultT(v.map(Xor.right))

  def <~[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromDbio(v)

  def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
    DbResultT.fromXor(v)

  def <~[A](v: Future[Failures Xor A]): DbResultT[A] =
    DbResultT(DBIO.from(v))

  def <~[A](v: Future[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromFuture(v)

  def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
    DbResultT.pure(v)

  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromXor(v.toXor)

  def <~[A, M[X] <: TraversableOnce[X]](v: M[DbResultT[A]])(
      implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]],
      ec: EC): DbResultT[M[A]] =
    DbResultT.sequence(v)

  def <~[A](v: DbResultT[A]): DbResultT[A] =
    v

  def <~[A](v: Option[DbResultT[A]])(implicit ec: EC): DbResultT[Option[A]] =
    v.fold(DbResultT.none[A]) { dbresult ⇒
      dbresult.map(Some(_))
    }
}
