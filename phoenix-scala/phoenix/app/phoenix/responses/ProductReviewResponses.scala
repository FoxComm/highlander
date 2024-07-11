package phoenix.responses

import phoenix.models.review.ProductReview
import phoenix.utils.aliases.Json

case class ProductReviewResponse(id: Int, sku: String, userId: Int, attributes: Json) extends ResponseItem

object ProductReviewResponse {
  def build(review: ProductReview, skuCode: String): ProductReviewResponse =
    ProductReviewResponse(review.id, skuCode, review.userId, review.content)
}

case class ProductReviewsResponse(reviews: Seq[ProductReviewResponse]) extends ResponseItem

object ProductReviewsResponse {
  def build(reviews: Seq[ProductReviewResponse]): ProductReviewsResponse =
    ProductReviewsResponse(reviews)
}
