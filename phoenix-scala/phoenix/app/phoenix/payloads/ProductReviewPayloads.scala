package phoenix.payloads

import phoenix.utils.aliases.Json

object ProductReviewPayloads {
  type AttributesMap = Map[String, Json]

  trait CreateProductReviewPayload {
    def attributes: Json
    def sku: String
    def scope: Option[String]
  }

  case class CreateProductReviewByCustomerPayload(attributes: Json, sku: String, scope: Option[String] = None)
      extends CreateProductReviewPayload

  case class CreateProductReviewByAdminPayload(userId: Option[Int],
                                               attributes: Json,
                                               sku: String,
                                               scope: Option[String] = None)
      extends CreateProductReviewPayload

  case class UpdateProductReviewPayload(attributes: Json)
}
