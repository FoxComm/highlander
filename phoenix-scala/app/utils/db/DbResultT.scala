package utils.db

import scala.collection.generic.CanBuildFrom
import scala.concurrent.Future

import cats.data.{Xor, XorT}
import failures.{Failure, Failures, SomethingWentWrong}
import slick.dbio.DBIO
import slick.lifted.Query
import utils.aliases._

object DbResultT {

  def apply[A](v: DBIO[Failures Xor A]): DbResultT[A] =
    XorT[DBIO, Failures, A](v)

  def pure[A](v: A)(implicit ec: EC): DbResultT[A] =
    XorT.pure[DBIO, Failures, A](v)

  def fromXor[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
    v.fold(failures, good)

  def fromDbio[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
    XorT.right[DBIO, Failures, A](v)

  def fromFuture[A](v: Future[A])(implicit ec: EC): DbResultT[A] =
    fromDbio(DBIO.from(v))

  def good[A](v: A)(implicit ec: EC): DbResultT[A] =
    XorT.right[DBIO, Failures, A](DBIO.successful(v))

  def failures[A](v: Failures)(implicit ec: EC): DbResultT[A] =
    XorT.left[DBIO, Failures, A](DBIO.successful(v))

  def failure[A](f: Failure)(implicit ec: EC): DbResultT[A] =
    failures[A](f.single)

  def sequence[A, M[X] <: TraversableOnce[X]](values: M[DbResultT[A]])(
      implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]],
      ec: EC): DbResultT[M[A]] =
    values
      .foldLeft(good(buildFrom(values))) { (liftedBuilder, liftedValue) ⇒
        for (builder ← liftedBuilder; value ← liftedValue) yield builder += value
      }
      .map(_.result)

  def unit(implicit ec: EC): DbResultT[Unit] =
    pure({})

  def none[A](implicit ec: EC): DbResultT[Option[A]] =
    pure(Option.empty[A])

  def nonEmptyQuery[A, C[_]](query: Query[_, A, C],
                             failure: Failure)(implicit ec: EC, sl: SL, sf: SF): DbResultT[A] =
    query.mustFindOneOr(failure)

  def nonEmptyQuery[A, C[_]](
      query: Query[_, A, C])(implicit ec: EC, sl: SL, sf: SF): DbResultT[A] =
    nonEmptyQuery(query, SomethingWentWrong(query))
}
