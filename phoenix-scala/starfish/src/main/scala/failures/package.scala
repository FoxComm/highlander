import cats.data._
import cats.implicits._
import utils.friendlyClassName

package object failures {

  trait Failure {
    def description: String
  }

  type Failures = NonEmptyList[Failure]

  def Failures(failures: Failure*): Option[Failures] = failures.toList match {
    case Nil          ⇒ None
    case head :: tail ⇒ Some(NonEmptyList(head, tail))
  }

  implicit class FailureOps(val underlying: Failure) extends AnyVal {
    def single: Failures = NonEmptyList.of(underlying)
  }

  implicit class FailuresOps(val underlying: Failures) extends AnyVal {
    def flatten: List[String] = underlying.toList.map(_.description)
  }

  case class GeneralFailure(a: String) extends Failure {
    override def description = a
  }

  case class DatabaseFailure(message: String) extends Failure {
    override def description = message
  }

  case class NotFoundFailure404(message: String) extends Failure {
    override def description = message
  }

  object NotFoundFailure404 {
    def apply[A](a: A, searchKey: Any): NotFoundFailure404 = {
      NotFoundFailure404(s"${friendlyClassName(a)} with key=$searchKey not found")
    }

    // TODO: get rid of this usage @michalrus
    def apply[A](a: A, searchTerm: String, searchKey: Any): NotFoundFailure404 =
      NotFoundFailure404(s"${friendlyClassName(a)} with key=$searchKey not found")

    // TODO: get rid of this usage @michalrus
    def apply(className: String, searchTerm: String, searchKey: Any): NotFoundFailure404 = {
      NotFoundFailure404(s"$className with $searchTerm=$searchKey not found")
    }
  }

  case class NotFoundFailure400(message: String) extends Failure {
    override def description = message
  }

  object NotFoundFailure400 {
    def apply[A](a: A, searchKey: Any): NotFoundFailure400 = {
      NotFoundFailure400(s"${friendlyClassName(a)} with key=$searchKey not found")
    }
  }

}
