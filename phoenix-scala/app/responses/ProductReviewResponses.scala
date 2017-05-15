package responses

import models.objects.FullObject
import models.review.ProductReview
import utils.aliases.Json

object ProductReviewResponses {
  def build(review: FullObject[ProductReview], skuCode: String): ProductReviewResponse =
    ProductReviewResponse(review.model.formId,
                          skuCode,
                          review.model.userId,
                          review.projectAttributes())

  case class ProductReviewResponse(id: Int, sku: String, userId: Int, attributes: Json)
      extends ResponseItem

}
