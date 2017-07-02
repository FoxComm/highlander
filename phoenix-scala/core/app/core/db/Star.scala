package core.db

import cats._
import cats.data._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import core.failures.{Failures, GeneralFailure}
import slick.dbio._
import slick.sql.SqlAction

import scala.concurrent.Future

object * extends LazyLogging {
  def <~[A](v: DBIO[Either[Failures, A]])(implicit M: Monad[DBIO]): DbResultT[A] =
    DbResultT.fromFEither(v)

  def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
    <~(v.map(Either.right))

  def <~[A](fa: DBIO[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromF(fa)

  def <~[A](fa: Either[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromEither(fa)

  def <~[A](gfa: Future[Either[Failures, A]])(implicit M1: Monad[DBIO], M2: Monad[Future]): DbResultT[A] =
    DbResultT.fromResult(Result.fromFEither(gfa))

  def <~[A](fa: Future[A])(implicit ec: EC): DbResultT[A] =
    <~(fa.map(Either.right).recover {
      case ex ⇒
        logger.error("A Future failed during conversion to DbResultT.", ex)
        Either.left(GeneralFailure(ex.getMessage).single)
    })

  def <~[A](fa: Result[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromResult(fa)

  // TODO: Is this more readable than inlining? @michalrus
  def <~[A](a: A)(implicit ec: EC): DbResultT[A] =
    a.pure[DbResultT]

  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromEither(v.toEither)

  def <~[M[_]: TraverseFilter, A](fas: M[DbResultT[A]])(implicit ec: EC): DbResultT[M[A]] =
    DbResultT.seqCollectFailures(fas)

  // FIXME: Remove this function after switching all Seqs to List/Vector. Cats don’t have instances for Seq and Seq is unsound. PM me or @kjanosz for details. @michalrus
  def <~[A](fas: Seq[DbResultT[A]])(implicit ec: EC): DbResultT[List[A]] =
    DbResultT.seqCollectFailures(fas.toList)

  // TODO: Is this more readable than inlining? @michalrus
  def <~[A](fa: DbResultT[A]): DbResultT[A] =
    fa

  // TODO: Is this more readable than inlining? @michalrus
  def <~[A](ofa: Option[DbResultT[A]])(implicit ec: EC): DbResultT[Option[A]] =
    ofa.sequence
}
