package failures

import models.review.ProductReview

object ProductReviewFailures {

  case class ProductReviewIsArchived(id: ProductReview#Id) extends Failure {
    override def description: String = s"Cannot update deleted review: $id"
  }

  case class ProductReviewUserMismatch(id: ProductReview#Id) extends Failure {
    override def description: String =
      s"Cannot update review $id: Only user which created the review can modify it."
  }

}
