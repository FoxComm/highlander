package payloads

import payloads.ImagePayloads.AlbumPayload
import payloads.SkuPayloads._
import payloads.VariantPayloads.VariantPayload
import utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(scope: Option[String] = None,
                                  attributes: Map[String, Json],
                                  slug: Option[String] = None,
                                  skus: Seq[SkuPayload],
                                  variants: Option[Seq[VariantPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None,
                                  override val schema: Option[String] = None)
      extends ObjectSchemaValidation.SchemaValidation[CreateProductPayload] {

    override def defaultSchemaName: String = "product"
  }

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  slug: Option[String] = None,
                                  skus: Option[Seq[SkuPayload]],
                                  variants: Option[Seq[VariantPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None)
      extends ObjectSchemaValidation.SchemaValidation[UpdateProductPayload] {
    override def defaultSchemaName: String = "product"
  }

}
