package responses

import models.image.Image
import models.objects.FullObject
import utils.{IlluminateAlgorithm, JsonFormatters}

object ImageResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object IlluminatedImageResponse {
    case class Root(id: Int, src: String, title: Option[String], alt: Option[String])
        extends ResponseItem

    def build(id: Int,
              src: String,
              title: Option[String] = None,
              alt: Option[String] = None): IlluminatedImageResponse.Root =
      Root(id, src, title, alt)

    def build(value: FullObject[Image]): IlluminatedImageResponse.Root = {
      val form   = value.form.attributes
      val shadow = value.shadow.attributes

      val src   = IlluminateAlgorithm.get("src", form, shadow).extract[String]
      val title = IlluminateAlgorithm.get("title", form, shadow).extractOpt[String]
      val alt   = IlluminateAlgorithm.get("alt", form, shadow).extractOpt[String]

      build(value.model.id, src, title, alt)
    }
  }
}
