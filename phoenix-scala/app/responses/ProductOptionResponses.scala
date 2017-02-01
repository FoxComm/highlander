package responses

import cats.implicits._
import models.objects._
import models.product._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductValueResponses.ProductValueResponse
import utils.aliases._

object ProductOptionResponses {

  type ProductValueVariantLinks = Map[Int, Seq[String]]

  sealed trait ProductOptionResponse extends ResponseItem {
    def id: Int
    def context: Option[ObjectContextResponse.Root]
    def attributes: Json
  }
  object ProductOptionResponse {
    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    values: Seq[ProductValueResponse.Root])
        extends ProductOptionResponse

    case class Partial(id: Int,
                       context: Option[ObjectContextResponse.Root],
                       attributes: Json,
                       value: ProductValueResponse.Root)
        extends ProductOptionResponse {
      def values: Seq[ProductValueResponse.Root] = List(value)
    }

    def build(productOption: IlluminatedProductOption,
              productValues: Seq[FullObject[ProductOptionValue]],
              productValueVariants: ProductValueVariantLinks): Root =
      Root(id = productOption.id,
           attributes = productOption.attributes,
           context = ObjectContextResponse.build(productOption.context).some,
           values = illuminateValues(productValues, productValueVariants))

    def buildLite(productOption: IlluminatedProductOption,
                  productValues: Seq[FullObject[ProductOptionValue]],
                  productValueVariants: ProductValueVariantLinks): Root =
      Root(id = productOption.id,
           attributes = productOption.attributes,
           context = None,
           values = illuminateValues(productValues, productValueVariants))

    def buildPartial(productOption: IlluminatedProductOption,
                     productValue: FullObject[ProductOptionValue]): Partial =
      Partial(id = productOption.id,
              attributes = productOption.attributes,
              context = ObjectContextResponse.build(productOption.context).some,
              value = ProductValueResponse.buildPartial(productValue))

    def illuminateValues(
        productValues: Seq[FullObject[ProductOptionValue]],
        productValueVariants: ProductValueVariantLinks): Seq[ProductValueResponse.Root] =
      productValues.map(vv ⇒
            ProductValueResponse.build(vv, productValueVariants.getOrElse(vv.model.id, Seq.empty)))
  }
}
