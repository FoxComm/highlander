package utils

import scala.concurrent.ExecutionContext

import services.Failures
import cats.{Monad, Applicative, Functor}
import cats.data.{XorT, Xor}
import slick.driver.PostgresDriver._
import slick.driver.PostgresDriver.api._

object DbResultT {
  type IO[A] = DBIOAction[A, NoStream, api.Effect.All]
  type DbResultT[A] = XorT[IO, Failures, A]

  object implicits {
    implicit def dbioApplicative(implicit ec: ExecutionContext): Applicative[IO] = new Applicative[IO] {
      def ap[A, B](fa: IO[A])(f: IO[A => B]): IO[B] =
        fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

      def pure[A](a: A): IO[A] = DBIO.successful(a)
    }

    implicit def dbioMonad(implicit ec: ExecutionContext, app: Applicative[IO]) = new Functor[IO] with Monad[IO] {
      override def map[A, B](fa: IO[A])(f: A ⇒ B): IO[B] = fa.map(f)

      override def pure[A](a: A): IO[A] = DBIO.successful(a)

      override def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B] = fa.flatMap(f)
    }
  }

  import implicits._

  def apply[A](v: DBIO[Failures Xor A])(implicit ec: ExecutionContext): DbResultT[A] =
    XorT[IO, Failures, A](v)

  def pure[A](v: A)(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.pure[IO, Failures, A](v)

  def fromXor[A](v: Failures Xor A)(implicit ec: ExecutionContext): DbResultT[A] =
    v.fold(leftLift, rightLift)

  def right[A](v: IO[A])(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.right[IO, Failures, A](v)

  def rightLift[A](v: A)(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.right[IO, Failures, A](DBIO.successful(v))

  def left[A](v: IO[Failures])(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.left[IO, Failures, A](v)

  def leftLift[A](v: Failures)(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.left[IO, Failures, A](DBIO.successful(v))
}
