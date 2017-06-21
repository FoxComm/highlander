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

  def <~[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromF(v)

  def <~[A](v: Either[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromEither(v)

  def <~[A](v: Future[Either[Failures, A]])(implicit M1: Monad[DBIO], M2: Monad[Future]): DbResultT[A] =
    DbResultT.fromResult(Result.fromFEither(v))

  def <~[A](v: Future[A])(implicit ec: EC): DbResultT[A] =
    <~(v.map(Either.right(_)).recover {
      case ex ⇒
        logger.error("A Future failed during conversion to DbResultT.", ex)
        Either.left(GeneralFailure(ex.getMessage).single)
    })

  def <~[A](fa: Result[A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromResult(fa)

  def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
    DbResultT.pure(v)

  def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
    DbResultT.fromEither(v.toEither)

  def <~[M[_]: TraverseFilter, A](v: M[DbResultT[A]])(implicit ec: EC): DbResultT[M[A]] =
    DbResultT.seqCollectFailures(v)

  // FIXME: Remove this function after switching all Seqs to List/Vector. Cats don’t have instances for Seq and Seq is unsound. PM me or @kjanosz for details. @michalrus
  def <~[A](v: Seq[DbResultT[A]])(implicit ec: EC): DbResultT[List[A]] =
    DbResultT.seqCollectFailures(v.toList)

  def <~[A](v: DbResultT[A]): DbResultT[A] =
    v

  def <~[A](v: Option[DbResultT[A]])(implicit ec: EC): DbResultT[Option[A]] = // TODO: sequence? @michalrus - yes, please! @aafa
    v.fold(DbResultT.none[A])(_.map(Some(_)))
}
