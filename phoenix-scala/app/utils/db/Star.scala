package utils.db

import scala.collection.generic.CanBuildFrom
import scala.concurrent.Future
import cats._
import cats.data._
import cats.implicits._
import failures.Failures
import utils.db._
import slick.dbio._
import slick.profile.SqlAction
import utils.aliases.EC

object * {
  def <~[A](v: DBIO[Failures Xor A])(implicit M: Monad[DBIO]): DbResultT[A] =
    DbResultT.fromFXor(v)

  def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
    <~(v.map(Xor.right))

  def <~[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromF(v)

  def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
    DbResultT.fromXor(v)

  def <~[A](v: Future[Failures Xor A])(implicit M1: Monad[DBIO], M2: Monad[Future]): DbResultT[A] =
    DbResultT.fromResultT(ResultT.fromFXor(v))

  def <~[A](v: Future[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromF(DBIO.from(v))

  def <~[A](fa: ResultT[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromResultT(fa)

  def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
    DbResultT.pure(v)

  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromXor(v.toXor)

  def <~[M[_]: TraverseFilter, A](v: M[DbResultT[A]])(implicit ec: EC): DbResultT[M[A]] =
    DbResultT.seqCollectFailures(v)

  // FIXME: Remove this function after switching all Seqs to List/Vector. Cats don’t have instances for Seq and Seq is unsound. PM me or @kjanosz for details. @michalrus
  def <~[A](v: Seq[DbResultT[A]])(implicit ec: EC): DbResultT[List[A]] =
    DbResultT.seqCollectFailures(v.toList)

  def <~[A](v: DbResultT[A]): DbResultT[A] =
    v

  def <~[A](v: Option[DbResultT[A]])(implicit ec: EC): DbResultT[Option[A]] = // TODO: sequence? @michalrus
    v.fold(DbResultT.none[A])(_.map(Some(_)))
}
