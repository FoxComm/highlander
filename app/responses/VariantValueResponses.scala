package responses

import models.objects._
import models.product._
import utils.{IlluminateAlgorithm, JsonFormatters}

object VariantValueResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object IlluminatedVariantValueResponse {
    case class Root(id: Int, name: String, swatch: Option[String] = None)

    def build(value: FullObject[VariantValue]): Root = {
      val model = value.model
      val formAttrs = value.form.attributes
      val shadowAttrs = value.shadow.attributes

      val name = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
      val swatch = IlluminateAlgorithm.get("swatch", formAttrs, shadowAttrs).extractOpt[String]

      Root(id = model.id, name = name, swatch = swatch)
    }
  }
}
