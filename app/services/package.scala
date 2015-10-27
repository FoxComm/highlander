import cats.data.{XorT, Xor, NonEmptyList}, Xor.{ left, right }
import scala.concurrent.{Future, ExecutionContext}
import cats.implicits._
import slick.driver.PostgresDriver.api._
import slick.lifted.Query
import slick.profile.RelationalTableComponent
import utils.CustomDirectives.SortAndPage
import utils.GenericTable.TableWithId

package object services {
  type Failures = NonEmptyList[Failure]

  def Failures(failures: Failure*): Failures = failures.toList match {
    case Nil          ⇒ throw new IllegalArgumentException("Can't instantiate NonEmptyList from an empty collection")
    case head :: tail ⇒ NonEmptyList(head, tail)
  }

  implicit class FailureOps(val underlying: Failure) extends AnyVal {
    def single: Failures = Failures(underlying)
  }

  implicit class FailuresOps(val underlying: Failures) extends AnyVal {
    def toList: List[Failure] = underlying.head :: underlying.tail
  }

  type Result[A] = Future[Failures Xor A]

  object Result {
    def fromFuture[A](value: Future[A])(implicit ec: ExecutionContext): services.Result[A] = value.flatMap(good)

    val unit = right(())

    def good[A](value: A):  Result[A] = Future.successful(Xor.right(value))
    def right[A](value: A): Result[A] = good(value)
    def left[A](fs: Failures): Result[A] = failures(fs)
    def leftNel[A](fs: NonEmptyList[Failure]): Result[A] = Future.successful(Xor.left(fs))

    def failures(fs: Failures): Result[Nothing] =
      Future.successful(Xor.left(fs))

    def failures(fs: Failure*): Result[Nothing] =
      failures(Failures(fs: _*))

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
  }}
