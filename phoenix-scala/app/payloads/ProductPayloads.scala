package payloads

import payloads.ImagePayloads.AlbumPayload
import payloads.ProductVariantPayloads._
import payloads.ProductOptionPayloads.ProductOptionPayload
import utils.aliases._

object ProductPayloads {
  case class CreateProductPayload(scope: Option[String] = None,
                                  attributes: Map[String, Json],
                                  variants: Seq[ProductVariantPayload],
                                  options: Option[Seq[ProductOptionPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None,
                                  override val schema: Option[String] = None)
      extends ObjectSchemaValidation.SchemaValidation[CreateProductPayload] {

    override def defaultSchemaName: String = "product"
  }

  case class UpdateProductPayload(attributes: Map[String, Json],
                                  variants: Option[Seq[ProductVariantPayload]],
                                  options: Option[Seq[ProductOptionPayload]],
                                  albums: Option[Seq[AlbumPayload]] = None)
      extends ObjectSchemaValidation.SchemaValidation[UpdateProductPayload] {
    override def defaultSchemaName: String = "product"
  }

}
