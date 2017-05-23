package phoenix.payloads

import phoenix.utils.aliases.Json

object ProductReviewPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateProductReviewPayload(attributes: Json,
                                        sku: String,
                                        scope: Option[String] = None)

  case class UpdateProductReviewPayload(attributes: Json)
}
