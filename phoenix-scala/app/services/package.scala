import scala.concurrent.Future

import cats.data.{NonEmptyList, Xor, XorT}
import cats.implicits._
import failures.{Failure, Failures}
import utils.aliases._

package object services {

  type Result[A] = Future[Failures Xor A]

  object Result {
    def fromFuture[A](value: Future[A])(implicit ec: EC): services.Result[A] = value.flatMap(good)

    val unit = right(())

    def good[A](value: A): Result[A]                     = Future.successful(Xor.right(value))
    def right[A](value: A): Result[A]                    = good(value)
    def left[A](fs: Failures): Result[A]                 = failures(fs)
    def leftNel[A](fs: NonEmptyList[Failure]): Result[A] = Future.successful(Xor.left(fs))

    def failures[A](fs: Failures): Result[A] =
      Future.successful(Xor.left[Failures, A](fs))

    def failure[A](failure: Failure): Result[A] =
      failures[A](NonEmptyList.of(failure))
  }

  type ResultT[A] = XorT[Future, Failures, A]

  object ResultT {
    def apply[A](xor: Result[A]): ResultT[A] = XorT[Future, Failures, A](xor)

    def fromXor[A](xor: Failures Xor A)(implicit ec: EC): ResultT[A] =
      xor.fold(leftAsync, rightAsync)

    def rightAsync[A](value: A)(implicit ec: EC): ResultT[A] = XorT.right(Future.successful(value))

    def right[A](value: Future[A])(implicit ec: EC): ResultT[A] = XorT.right(value)

    def left[A](f: Future[Failures])(implicit ec: EC): ResultT[A] = XorT.left(f)

    def leftAsync[A](f: Failures)(implicit ec: EC): ResultT[A] = XorT.left(Future.successful(f))
  }
}
