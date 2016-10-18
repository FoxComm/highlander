package payloads

import cats.data.{Validated, ValidatedNel}
import failures.Failure
import models.image._
import models.objects.ObjectUtils._
import models.objects._
import payloads.ObjectPayloads.{AttributesBuilder, StringField}
import utils.Validation
import utils.Validation._

object ImagePayloads {

  type Images = Option[Seq[ImagePayload]]

  case class ImagePayload(id: Option[Int] = None,
                          src: String,
                          title: Option[String] = None,
                          alt: Option[String] = None,
                          scope: Option[String] = None) {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = ObjectPayloads.optionalAttributes(
          Some(StringField("src", src)),
          title.map(StringField("title", _)),
          alt.map(StringField("alt", _)))

      (ObjectForm(kind = Image.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }
  }

  case class CreateAlbumPayload(name: String,
                                images: Images = None,
                                position: Option[Int] = None,
                                scope: Option[String] = None)
      extends Validation[CreateAlbumPayload] {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder = AttributesBuilder(StringField("name", name))

      (ObjectForm(kind = Album.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }

    override def validate: ValidatedNel[Failure, CreateAlbumPayload] = images match {
      case Some(imgList) ⇒
        val withId = imgList.filter(_.id.isDefined)
        validExpr(withId.isEmpty, s"Image id should be empty").map(_ ⇒ this)
      case None ⇒
        Validated.valid(this)
    }
  }

  case class UpdateAlbumPayload(name: Option[String] = None,
                                images: Images = None,
                                position: Option[Int] = None)
      extends Validation[UpdateAlbumPayload] {

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder =
        ObjectPayloads.optionalAttributes(name.map(StringField("name", _)))

      (ObjectForm(kind = Album.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }

    override def validate: ValidatedNel[Failure, UpdateAlbumPayload] =
      validateIdsUnique(images).map(_ ⇒ this)
  }

  def validateIdsUnique(images: Images): ValidatedNel[Failure, Images] = images match {
    case Some(imgList) ⇒
      val duplicated = imgList.groupBy(_.id.getOrElse(0)).collect {
        case (id, items) if id != 0 && items.size > 1 ⇒ id
      }
      validExpr(duplicated.isEmpty, s"Image ID is duplicated ${duplicated.head}").map(_ ⇒ images)
    case None ⇒
      Validated.valid(images)

  }

  case class UpdateAlbumPositionPayload(albumId: Int, position: Int)
}
