package responses

import cats.implicits._
import models.objects._
import models.product._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductValueResponses.IlluminatedProductValueResponse
import utils.aliases._

object ProductOptionResponses {

  type ProductValueVariantLinks = Map[Int, Seq[String]]

  object IlluminatedProductOptionResponse {
    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    values: Seq[IlluminatedProductValueResponse.Root])
        extends ResponseItem

    def build(productOption: IlluminatedProductOption,
              productValues: Seq[FullObject[ProductValue]],
              productValueVariants: ProductValueVariantLinks): Root =
      Root(id = productOption.id,
           attributes = productOption.attributes,
           context = ObjectContextResponse.build(productOption.context).some,
           values = illuminateValues(productValues, productValueVariants))

    def buildLite(productOption: IlluminatedProductOption,
                  productValues: Seq[FullObject[ProductValue]],
                  productValueVariants: ProductValueVariantLinks): Root =
      Root(id = productOption.id,
           attributes = productOption.attributes,
           context = None,
           values = illuminateValues(productValues, productValueVariants))

    def illuminateValues(productValues: Seq[FullObject[ProductValue]],
                         productValueVariants: ProductValueVariantLinks)
      : Seq[IlluminatedProductValueResponse.Root] =
      productValues.map(
          vv ⇒
            IlluminatedProductValueResponse
              .build(vv, productValueVariants.getOrElse(vv.model.id, Seq.empty)))
  }
}
