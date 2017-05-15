package payloads

import utils.aliases._

object ProductReviewPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateProductReviewPayload(attributes: Map[String, Json],
                                        sku: String,
                                        scope: Option[String] = None)

  case class UpdateProductReviewPayload(attributes: Map[String, Json])
}
