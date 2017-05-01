package failures

import models.Assignment
import models.activity.Dimension
import models.cord.{Cart, Cord, Order}
import models.inventory.Sku
import models.payment.giftcard.GiftCard
import models.returns.Return
import models.sharedsearch.SharedSearch

object Util {

  def searchTerm[A](a: A): String = a match {
    case Cart | _: Cart                       ⇒ "referenceNumber"
    case Order | _: Order                     ⇒ "referenceNumber"
    case Cord | _: Cord                       ⇒ "referenceNumber"
    case Return | _: Return                   ⇒ "referenceNumber"
    case Assignment.Order | Assignment.Return ⇒ "referenceNumber"

    case GiftCard | _: GiftCard               ⇒ "code"
    case Sku | _: Sku                         ⇒ "code"
    case Assignment.GiftCard | Assignment.Sku ⇒ "code"
    case SharedSearch | _: SharedSearch       ⇒ "code"

    case Dimension | _: Dimension ⇒ "name"
    case _                        ⇒ "id"
  }

  /* Diff lists of model identifiers to produce a list of failures for absent models */
  def diffToFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[Failures] =
    Failures(requested.diff(available).map(NotFoundFailure404(modelType, _)): _*)

  /* Diff lists of model identifiers to produce a list of warnings for absent models */
  def diffToFlatFailures[A, B](requested: Seq[A],
                               available: Seq[A],
                               modelType: B): Option[List[String]] =
    diffToFailures(requested, available, modelType).map(_.flatten)
}
