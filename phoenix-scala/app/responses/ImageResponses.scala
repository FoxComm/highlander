package responses

import cats.implicits._
import models.image.Image
import models.objects.FullObject
import utils.IlluminateAlgorithm
import utils.json.yolo._

object ImageResponses {
  object ImageResponse {
    case class Root(id: Int,
                    src: String,
                    baseUrl: Option[String],
                    title: Option[String],
                    alt: Option[String])
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

      val src     = IlluminateAlgorithm.get("src", form, shadow).orEmpty.extract[String]
      val baseUrl = IlluminateAlgorithm.get("baseUrl", form, shadow).flatMap(_.asString)
      val title   = IlluminateAlgorithm.get("title", form, shadow).flatMap(_.asString)
      val alt     = IlluminateAlgorithm.get("alt", form, shadow).flatMap(_.asString)

      build(value.model.id, src, baseUrl, title, alt)
    }
  }
}
