package responses

import cats.implicits._
import models.objects._
import models.product._
import responses.ObjectResponses.ObjectContextResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import utils.aliases._

object VariantResponses {
  object IlluminatedVariantResponse {
    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    values: Seq[IlluminatedVariantValueResponse.Root])
        extends ResponseItem

    def build(variant: IlluminatedVariant,
              variantValues: Seq[FullObject[VariantValue]],
              variantValueSkuCodeLinks: Map[Int, Seq[String]]): Root =
      Root(id = variant.id,
           attributes = variant.attributes,
           context = ObjectContextResponse.build(variant.context).some,
           values = variantValues.map(
               vv ⇒
                 IlluminatedVariantValueResponse
                   .build(vv, variantValueSkuCodeLinks.getOrElse(vv.model.id, Seq.empty))))

    def buildLite(variant: IlluminatedVariant,
                  variantValues: Seq[FullObject[VariantValue]],
                  variantValueSkuCodeLinks: Map[Int, Seq[String]]): Root =
      Root(id = variant.id,
           attributes = variant.attributes,
           context = None,
           values = variantValues.map(
               vv ⇒
                 IlluminatedVariantValueResponse
                   .build(vv, variantValueSkuCodeLinks.getOrElse(vv.model.id, Seq.empty))))
  }
}
