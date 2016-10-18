package payloads

import payloads.SkuPayloads._
import payloads.VariantPayloads.VariantPayload
import utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(attributes: Map[String, Json],
                                  skus: Seq[SkuPayload],
                                  variants: Option[Seq[VariantPayload]],
                                  scope: Option[String] = None)

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  skus: Option[Seq[SkuPayload]],
                                  variants: Option[Seq[VariantPayload]])
}
