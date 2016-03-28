import cats.data._
import cats.implicits._
import models.activity.Dimension
import models.inventory.Sku
import models.order.Order
import models.payment.giftcard.GiftCard
import models.rma.Rma
import responses.BatchMetadata

package object failures {

  type Failures = NonEmptyList[Failure]

  def Failures(failures: Failure*): Option[Failures] = failures.toList match {
    case Nil          ⇒ None
    case head :: tail ⇒ Some(NonEmptyList(head, tail))
  }

  implicit class FailureOps(val underlying: Failure) extends AnyVal {
    def single: Failures = NonEmptyList(underlying)
  }

  implicit class FailuresOps(val underlying: Failures) extends AnyVal {
    def toList: List[Failure] = underlying.unwrap

    def flatten: List[String] = toList.map(_.description)
  }

  object Util {

    def searchTerm[A](a: A): String = a match {
      case Order | _: Order | Rma | _: Rma ⇒ "referenceNumber"
      case GiftCard | _: GiftCard | Sku | _: Sku ⇒ "code"
      case Dimension | _: Dimension ⇒ "name"
      case _ ⇒ "id"
    }

    /* Diff lists of model identifiers to produce a list of failures for absent models */
    def diffToFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[Failures] =
      Failures(requested.diff(available).map(NotFoundFailure404(modelType, _)): _*)

    /* Diff lists of model identifiers to produce a list of warnings for absent models */
    def diffToFlatFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[List[String]] =
      diffToFailures(requested, available, modelType).map(_.flatten)
  }
}
