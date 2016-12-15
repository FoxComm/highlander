package responses

import cats.implicits._
import models.objects._
import models.product._
import responses.ObjectResponses.ObjectContextResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import utils.aliases._

object VariantResponses {

  type VariantValueSkuLinks = Map[Int, Seq[String]]

  object IlluminatedVariantResponse {
    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    values: Seq[IlluminatedVariantValueResponse.Root])
        extends ResponseItem

    def build(variant: IlluminatedProductOption,
              variantValues: Seq[FullObject[ProductValue]],
              variantValueSkus: VariantValueSkuLinks): Root =
      Root(id = variant.id,
           attributes = variant.attributes,
           context = ObjectContextResponse.build(variant.context).some,
           values = illuminateValues(variantValues, variantValueSkus))

    def buildLite(variant: IlluminatedProductOption,
                  variantValues: Seq[FullObject[ProductValue]],
                  variantValueSkus: VariantValueSkuLinks): Root =
      Root(id = variant.id,
           attributes = variant.attributes,
           context = None,
           values = illuminateValues(variantValues, variantValueSkus))

    def illuminateValues(
        variantValues: Seq[FullObject[ProductValue]],
        variantValueSkus: VariantValueSkuLinks): Seq[IlluminatedVariantValueResponse.Root] =
      variantValues.map(
          vv ⇒
            IlluminatedVariantValueResponse
              .build(vv, variantValueSkus.getOrElse(vv.model.id, Seq.empty)))
  }
}
