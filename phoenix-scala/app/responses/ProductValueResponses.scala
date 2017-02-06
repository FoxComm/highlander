package responses

import models.objects._
import models.product._
import utils.{IlluminateAlgorithm, JsonFormatters}

object ProductValueResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  // Kangaroos TODO: replace skuCodes with variantIds -- @anna
  object ProductValueResponse {
    case class Root(id: Int, name: String, swatch: Option[String] = None, skuCodes: Seq[String])

    def build(value: FullObject[ProductOptionValue], skuCodes: Seq[String]): Root = {
      val model       = value.model
      val formAttrs   = value.form.attributes
      val shadowAttrs = value.shadow.attributes

      val name   = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
      val swatch = IlluminateAlgorithm.get("swatch", formAttrs, shadowAttrs).extractOpt[String]

      Root(id = model.formId, name = name, swatch = swatch, skuCodes)
    }
  }
}
