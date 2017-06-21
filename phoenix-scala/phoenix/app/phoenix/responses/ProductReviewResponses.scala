package phoenix.responses

import phoenix.models.review.ProductReview
import phoenix.utils.aliases.Json

object ProductReviewResponses {
  def build(review: ProductReview, skuCode: String): ProductReviewResponse =
    ProductReviewResponse(review.id, skuCode, review.userId, review.content)

  case class ProductReviewResponse(id: Int, sku: String, userId: Int, attributes: Json) extends ResponseItem

}
