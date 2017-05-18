package phoenix.payloads

import payloads.ObjectSchemaValidation
import phoenix.payloads.ImagePayloads.AlbumPayload
import phoenix.payloads.SkuPayloads._
import phoenix.payloads.VariantPayloads.VariantPayload
import phoenix.utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(scope: Option[String] = None,
                                  attributes: Map[String, Json],
                                  slug: String = "",
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
