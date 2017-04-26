package responses

import cats.implicits._
import models.objects._
import models.product._
import utils.IlluminateAlgorithm
import utils.json.yolo._

object VariantValueResponses {
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

      val name   = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).orEmpty.extract[String]
      val swatch = IlluminateAlgorithm.get("swatch", formAttrs, shadowAttrs).flatMap(_.asString)
      val image  = IlluminateAlgorithm.get("image", formAttrs, shadowAttrs).flatMap(_.asString)

      Root(id = model.formId, name = name, swatch = swatch, image = image, skuCodes)
    }
  }
}
