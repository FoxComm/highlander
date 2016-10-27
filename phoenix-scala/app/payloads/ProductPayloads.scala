package payloads

import payloads.SkuPayloads._
import payloads.VariantPayloads.VariantPayload
import utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(attributes: Map[String, Json],
                                  skus: Seq[SkuPayload],
                                  variants: Option[Seq[VariantPayload]],
                                  override val schema: Option[String] = None)
      extends ObjectSchemaValidation.SchemaValidation[CreateProductPayload] {

    override def defaultSchemaName: String = "product"
  }

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  skus: Option[Seq[SkuPayload]],
                                  variants: Option[Seq[VariantPayload]])
      extends ObjectSchemaValidation.SchemaValidation[UpdateProductPayload] {
    override def defaultSchemaName: String = "product"
  }

}
