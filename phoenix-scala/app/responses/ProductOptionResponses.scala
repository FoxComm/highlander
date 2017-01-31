package responses

import cats.implicits._
import models.objects._
import models.product._
import org.json4s._
import org.json4s.JsonDSL._
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductValueResponses.ProductValueResponse
import utils.aliases._

object ProductOptionResponses {

  type ProductValueVariantLinks = Map[Int, Seq[String]]

  object ProductOptionResponse {
    private object RootExtractor {
      def unapply(v: JValue)(implicit formats: Formats): Option[Root] = v match {
        case obj: JObject ⇒
          val fields = obj.values
          if (fields.contains("values")) obj.extractOpt[Root.Standalone]
          else if (fields.contains("value")) obj.extractOpt[Root.Partial]
          else None
        case _ ⇒ None
      }
    }

    // There is a typeclass-based mechanism in json4s (Writer, Read and JsonFormat),
    // but I bet json4s developers don't know themselves why they added it
    // since no part of json4s serialization mechanism use it under the hood.
    // Instead we have an implicitly-scoped registry with custom serializers.
    // I guess the only reason was that it fits nicely with retarded way of class serialization based on runtime reflection.
    // And you know, you cannot specify only a serializer or only a deserializer for some class,
    // because why the hell would you like to have a static guarantee
    // that some custom type is only (de)serializable? You shouldn't even think about it.
    // tl;dr This library might has been good in early (pre 2.10) Scala days, but not anymore.
    val Serializer = new CustomSerializer[Root](implicit formats ⇒
          ({
        case RootExtractor(root) ⇒ root
      }, {
        case root: Root ⇒
          val jOptionValues = root match {
            case Root.Standalone(_, _, _, values) ⇒ "values" → Extraction.decompose(values)
            case Root.Partial(_, _, _, value)     ⇒ "value"  → Extraction.decompose(value)
          }
          ("id"         → root.id) ~ ("context" → Extraction.decompose(root.context)) ~
          ("attributes" → root.attributes) ~ jOptionValues
      }))

    sealed trait Root extends ResponseItem {
      def id: Int
      def context: Option[ObjectContextResponse.Root]
      def attributes: Json
      def values: Seq[ProductValueResponse.Root]
    }
    object Root {
      case class Standalone(id: Int,
                            context: Option[ObjectContextResponse.Root],
                            attributes: Json,
                            values: Seq[ProductValueResponse.Root])
          extends Root

      case class Partial(id: Int,
                         context: Option[ObjectContextResponse.Root],
                         attributes: Json,
                         value: ProductValueResponse.Root)
          extends Root {
        def values: Seq[ProductValueResponse.Root] = List(value)
      }
    }

    def build(productOption: IlluminatedProductOption,
              productValues: Seq[FullObject[ProductOptionValue]],
              productValueVariants: ProductValueVariantLinks): Root =
      Root.Standalone(id = productOption.id,
                      attributes = productOption.attributes,
                      context = ObjectContextResponse.build(productOption.context).some,
                      values = illuminateValues(productValues, productValueVariants))

    def buildLite(productOption: IlluminatedProductOption,
                  productValues: Seq[FullObject[ProductOptionValue]],
                  productValueVariants: ProductValueVariantLinks): Root =
      Root.Standalone(id = productOption.id,
                      attributes = productOption.attributes,
                      context = None,
                      values = illuminateValues(productValues, productValueVariants))

    def buildPartial(productOption: IlluminatedProductOption,
                     productValue: FullObject[ProductOptionValue]): Root =
      Root.Partial(id = productOption.id,
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
