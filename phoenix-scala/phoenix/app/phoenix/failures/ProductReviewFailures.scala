package phoenix.failures

import core.failures.Failure
import phoenix.models.review.ProductReview

object ProductReviewFailures {

  case class ProductReviewIsArchived(id: ProductReview#Id) extends Failure {
    override def description: String = s"Cannot update deleted review: $id"
  }

  case class UpdateProductReviewUserMismatch(id: ProductReview#Id) extends Failure {
    override def description: String =
      s"Cannot update review $id: Only the user who created the review can modify it."
  }

  case class FetchProductReviewUserMismatch(id: ProductReview#Id) extends Failure {
    override def description: String =
      s"Cannot fetch review $id: You can only fetch your own reviews."
  }
}
