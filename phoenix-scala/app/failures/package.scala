import cats.data._
import cats.implicits._
import models.Assignment
import models.activity.Dimension
import models.cord.{Cart, Cord, Order}
import models.inventory.ProductVariant
import models.payment.giftcard.GiftCard
import models.returns.Return
import models.sharedsearch.SharedSearch

package object failures {

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

  object Util {

    def searchTerm[A](a: A): String = a match {
      case Cart | _: Cart                       ⇒ "referenceNumber"
      case Order | _: Order                     ⇒ "referenceNumber"
      case Cord | _: Cord                       ⇒ "referenceNumber"
      case Return | _: Return                   ⇒ "referenceNumber"
      case Assignment.Order | Assignment.Return ⇒ "referenceNumber"

      case GiftCard | _: GiftCard               ⇒ "code"
      case ProductVariant | _: ProductVariant   ⇒ "code"
      case Assignment.GiftCard | Assignment.Sku ⇒ "code"
      case SharedSearch | _: SharedSearch       ⇒ "code"

      case Dimension | _: Dimension ⇒ "name"
      case _                        ⇒ "id"
    }

    /* Diff lists of model identifiers to produce a list of failures for absent models */
    def diffToFailures[A, B](requested: Seq[A],
                             available: Seq[A],
                             modelType: B): Option[Failures] =
      Failures(requested.diff(available).map(NotFoundFailure404(modelType, _)): _*)

    /* Diff lists of model identifiers to produce a list of warnings for absent models */
    def diffToFlatFailures[A, B](requested: Seq[A],
                                 available: Seq[A],
                                 modelType: B): Option[List[String]] =
      diffToFailures(requested, available, modelType).map(_.flatten)
  }
}
