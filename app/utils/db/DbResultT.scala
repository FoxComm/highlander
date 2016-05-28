package utils.db

import scala.collection.generic.CanBuildFrom

import cats.data.{Validated, Xor, XorT}
import cats.{Applicative, Functor, Monad}
import failures.Failures
import services.Result
import scala.concurrent.Future

import slick.driver.PostgresDriver.api._
import slick.profile.SqlAction
import utils.aliases._
import utils.db._

object DbResultT {

  implicit def dbioApplicative(implicit ec: EC): Applicative[DBIO] = new Applicative[DBIO] {
    def ap[A, B](fa: DBIO[A])(f: DBIO[A => B]): DBIO[B] =
      fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

    def pure[A](a: A): DBIO[A] = DBIO.successful(a)
  }

  implicit def dbioMonad(implicit ec: EC) = new Functor[DBIO] with Monad[DBIO] {
    override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

    override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

    override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] = fa.flatMap(f)
  }

  implicit class EnrichedDbResultT[A](dbResultT: DbResultT[A]) {
    def runTxn()(implicit db: DB): Result[A] =
      dbResultT.value.transactionally.run()

    def run()(implicit db: DB): Result[A] =
      dbResultT.value.run()
  }

  final implicit class EnrichedOption[A](val option: Option[A]) extends AnyVal {
    def toXor[F](or: F): F Xor A =
      option.fold { Xor.left[F, A](or) }(Xor.right[F, A])
  }

  def apply[A](v: DBIO[Failures Xor A]): DbResultT[A] =
    XorT[DBIO, Failures, A](v)

  def pure[A](v: A)(implicit ec: EC): DbResultT[A] =
    XorT.pure[DBIO, Failures, A](v)

  def fromXor[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
    v.fold(leftLift, rightLift)

  def right[A](v: DBIO[A])(implicit ec: EC): DbResultT[A] =
    XorT.right[DBIO, Failures, A](v)

  def rightLift[A](v: A)(implicit ec: EC): DbResultT[A] =
    XorT.right[DBIO, Failures, A](DBIO.successful(v))

  def left[A](v: DBIO[Failures])(implicit ec: EC): DbResultT[A] =
    XorT.left[DBIO, Failures, A](v)

  def leftLift[A](v: Failures)(implicit ec: EC): DbResultT[A] =
    left(DBIO.successful(v))

  def sequence[A, M[X] <: TraversableOnce[X]](values: M[DbResultT[A]])(
      implicit buildFrom: CanBuildFrom[M[DbResultT[A]], A, M[A]], ec: EC): DbResultT[M[A]] =
    values
      .foldLeft(rightLift(buildFrom(values))) { (liftedBuilder, liftedValue) ⇒
        for (builder ← liftedBuilder; value ← liftedValue) yield builder += value
      }
      .map(_.result)

  def unit: DbResultT[Unit] = apply(DbResult.unit)

  object * {
    def <~[A](v: DBIO[Failures Xor A]): DbResultT[A] =
      DbResultT(v)

    def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
      DbResultT(v.map(Xor.right))

    def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
      DbResultT.fromXor(v)

    def <~[A](v: Future[Failures Xor A]): DbResultT[A] =
      DbResultT(DBIO.from(v))

    def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
      DbResultT.pure(v)

    def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
      DbResultT.fromXor(v.toXor)

    def <~[A](v: DbResultT[A]): DbResultT[A] =
      v
  }
}
