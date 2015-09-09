import collection.immutable
import scala.concurrent.{ExecutionContext, Future}

import cats.data.{XorT, Xor, NonEmptyList}, Xor.{ left, right }
import models.Note
import scala.concurrent.{Future, ExecutionContext}
import org.scalactic.{Bad, Good, Or}
import cats.implicits._

package object services {
  type Failures = immutable.Seq[Failure]
  private [services] def Failures(failures: Failure*): Failures = immutable.Seq[Failure](failures: _*)

  implicit class FailuresOps(val underlying: Failure) extends AnyVal {
    def single: Failures = Failures(underlying)
  }

  implicit class NonEmptyListFailuresOps(val underlying: NonEmptyList[Failure]) extends AnyVal {
    import cats.implicits._
    def failure: Failures = Failures(underlying.unwrap.toSeq: _*)
  }

  type Result[A] = Future[Failures Xor A]

  object Result {
    def fromFuture[A](value: Future[A])(implicit ec: ExecutionContext): services.Result[A] = value.flatMap(good)

    val unit = right(())

    def good[A](value: A):  Result[A] = Future.successful(Xor.right(value))
    def right[A](value: A): Result[A] = good(value)
    def left[A](fs: Failures): Result[A] = failures(fs: _*)
    def leftNel[A](fs: NonEmptyList[Failure]): Result[A] = Future.successful(Xor.left(fs.unwrap.toSeq))

    def failures(failures: Failure*): Result[Nothing] =
      Future.successful(Xor.left(Failures(failures: _*)))

    def failures(fs: Failures): Result[Nothing] =
      failures(fs: _*)

    def failure(failure: Failure): Result[Nothing] =
      failures(failure)
  }

  type ResultT[A] = XorT[Future, Failures, A]

  object ResultT {
    def apply[A](xor: Result[A])
      (implicit ec: ExecutionContext): ResultT[A] = XorT[Future, Failures, A](xor)

    def fromXor[A](xor: Failures Xor A)
      (implicit ec: ExecutionContext): ResultT[A] = xor.fold(leftAsync, rightAsync)

    def rightAsync[A](value: A)(implicit ec: ExecutionContext):     ResultT[A] = XorT.right(Future.successful(value))
    def right[A](value: Future[A])(implicit ec: ExecutionContext):  ResultT[A] = XorT.right(value)

    def left[A](f: Future[Failures])(implicit ec: ExecutionContext):  ResultT[A] = XorT.left(f)
    def leftAsync[A](f: Failures)(implicit ec: ExecutionContext):     ResultT[A] = XorT.left(Future.successful(f))
  }
}
