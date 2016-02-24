package utils

import scala.collection.generic.CanBuildFrom

import cats.data.{Validated, Xor, XorT}
import cats.{Applicative, Functor, Monad}
import services.{Failures, Result}
import slick.driver.PostgresDriver.api._
import slick.profile.SqlAction
import utils.Slick.implicits._
import utils.aliases._

object DbResultT {
  type DbResultT[A] = XorT[DBIO, Failures, A]

  object implicits {
    implicit def dbioApplicative(implicit ec: EC): Applicative[DBIO] = new Applicative[DBIO] {
      def ap[A, B](fa: DBIO[A])(f: DBIO[A => B]): DBIO[B] =
        fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

      def pure[A](a: A): DBIO[A] = DBIO.successful(a)
    }

    implicit def dbioMonad(implicit ec: EC, app: Applicative[DBIO]) = new Functor[DBIO] with Monad[DBIO] {
      override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

      override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

      override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] = fa.flatMap(f)
    }

    implicit class EnrichedDbResultT[A](dbResultT: DbResultT[A]) {
      def runTxn()(implicit ec: EC, db: DB): Result[A] =
        dbResultT.value.transactionally.run()

      def run()(implicit ec: EC, db: DB): Result[A] =
        dbResultT.value.run()
    }

    final implicit class EnrichedOption[A](val option: Option[A]) {
      def toXor[F](or: F): F Xor A =
        option.fold { Xor.left[F, A](or) } (Xor.right[F,A])
    }
  }

  import implicits._

  def apply[A](v: DBIO[Failures Xor A])(implicit ec: EC): DbResultT[A] =
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

  def sequence[A, M[X] <: TraversableOnce[X]](in: M[DbResultT[A]])
    (implicit cbf: CanBuildFrom[M[DbResultT[A]], A, M[A]], ec: EC): DbResultT[M[A]] =
    in.foldLeft(rightLift(cbf(in))) {
      (fr, fa) ⇒ for (r ← fr; a ← fa) yield r += a
    }.map(_.result())

  object * {
    def <~[A](v: DBIO[Failures Xor A])(implicit ec: EC): DbResultT[A] =
      DbResultT(v)

    def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: EC): DbResultT[A] =
      DbResultT(v.map(Xor.right))

    def <~[A](v: Failures Xor A)(implicit ec: EC): DbResultT[A] =
      DbResultT.fromXor(v)

    def <~[A](v: A)(implicit ec: EC): DbResultT[A] =
      DbResultT.pure(v)

    def <~[A](v: Validated[Failures, A])(implicit ec: EC): DbResultT[A] =
      DbResultT.fromXor(v.toXor)

    def <~[A](v: DbResultT[A])(implicit ec: EC): DbResultT[A] =
      v
  }
}
