package models.image

import cats.data.Xor
import cats.data.Xor.{left, right}
import failures.{Failures, GeneralFailure}
import models.objects._
import utils.{IlluminateAlgorithm, JsonFormatters}

case class Image(src: String, title: Option[String] = None, alt: Option[String] = None)

object Image {
  implicit val formats = JsonFormatters.phoenixFormats

  def buildFromAlbum(album: FullObject[Album]): Failures Xor Seq[Image] = {
    val images = IlluminateAlgorithm.get("images", album.form.attributes, album.shadow.attributes)
    images.extractOpt[Seq[Image]] match {
      case Some(seq) ⇒  right(seq)
      case None      ⇒  left(GeneralFailure("Image sequence could not be retrieved from album").single)
    }
  }
}

