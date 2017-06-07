package phoenix.responses

import cats.implicits._
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.product._
import phoenix.responses.VariantValueResponses.IlluminatedVariantValueResponse
import phoenix.utils.aliases._

object VariantResponses {

  type VariantValueSkuLinks = Map[Int, Seq[String]]

  object IlluminatedVariantResponse {
    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    values: Seq[IlluminatedVariantValueResponse.Root])
        extends ResponseItem

    def build(variant: IlluminatedVariant,
              variantValues: Seq[FullObject[VariantValue]],
              variantValueSkus: VariantValueSkuLinks): Root =
      Root(
        id = variant.id,
        attributes = variant.attributes,
        context = ObjectContextResponse.build(variant.context).some,
        values = illuminateValues(variantValues, variantValueSkus)
      )

    def buildLite(variant: IlluminatedVariant,
                  variantValues: Seq[FullObject[VariantValue]],
                  variantValueSkus: VariantValueSkuLinks): Root =
      Root(id = variant.id,
           attributes = variant.attributes,
           context = None,
           values = illuminateValues(variantValues, variantValueSkus))

    def illuminateValues(variantValues: Seq[FullObject[VariantValue]],
                         variantValueSkus: VariantValueSkuLinks): Seq[IlluminatedVariantValueResponse.Root] =
      variantValues.map(
        vv ⇒
          IlluminatedVariantValueResponse
            .build(vv, variantValueSkus.getOrElse(vv.model.id, Seq.empty)))
  }
}
