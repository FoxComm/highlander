package responses

import models.objects._
import models.product._
import utils.{IlluminateAlgorithm, JsonFormatters}

object VariantValueResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object IlluminatedVariantValueResponse {
    case class Root(id: Int,
                    name: String,
                    swatch: Option[String] = None,
                    image: Option[String],
                    skuCodes: Seq[String])

    def build(value: FullObject[VariantValue], skuCodes: Seq[String]): Root = {
      val model       = value.model
      val formAttrs   = value.form.attributes
      val shadowAttrs = value.shadow.attributes

      val name   = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
      val swatch = IlluminateAlgorithm.get("swatch", formAttrs, shadowAttrs).extractOpt[String]
      val image  = IlluminateAlgorithm.get("image", formAttrs, shadowAttrs).extractOpt[String]

      Root(id = model.formId, name = name, swatch = swatch, image = image, skuCodes)
    }
  }
}
