package phoenix.responses

import objectframework.IlluminateAlgorithm
import objectframework.models.FullObject
import phoenix.models.image.Image
import phoenix.utils.JsonFormatters

object ImageResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object ImageResponse {
    case class Root(id: Int, src: String, baseUrl: Option[String], title: Option[String], alt: Option[String])
        extends ResponseItem

    def build(id: Int,
              src: String,
              baseUrl: Option[String] = None,
              title: Option[String] = None,
              alt: Option[String] = None): ImageResponse.Root =
      Root(id, src, baseUrl, title, alt)

    def build(value: FullObject[Image]): ImageResponse.Root = {
      val form   = value.form.attributes
      val shadow = value.shadow.attributes

      val src     = IlluminateAlgorithm.get("src", form, shadow).extract[String]
      val baseUrl = IlluminateAlgorithm.get("baseUrl", form, shadow).extractOpt[String]
      val title   = IlluminateAlgorithm.get("title", form, shadow).extractOpt[String]
      val alt     = IlluminateAlgorithm.get("alt", form, shadow).extractOpt[String]

      build(value.model.id, src, baseUrl, title, alt)
    }
  }
}
