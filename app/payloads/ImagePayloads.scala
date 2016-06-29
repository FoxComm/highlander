package payloads

import cats.data.{Validated, ValidatedNel}
import failures.Failure
import models.image._
import models.objects._
import org.json4s.JsonAST.{JValue, JField, JObject}
import org.json4s.JsonDSL._
import utils.Validation
import utils.Validation._
import utils.aliases._

object ImagePayloads {

  type Images = Option[Seq[ImagePayload]]

  def generateIds(images: Seq[ImagePayload]): Seq[ImagePayload] = {
    val (withId, withoutId)      = images.span(_.id.isDefined)
    val usedIds                  = withId.map(_.id.get).toSet
    val idGenerator: Stream[Int] = Stream.from(1).filter(!usedIds.contains(_))
    withId ++ withoutId.zip(idGenerator).map { case (image, id) ⇒ image.copy(id = Some(id)) }
  }

  val imageShadow    = ("type" → "images") ~ ("ref" → "images")
  val nameShadow     = ("type" → "string") ~ ("ref" → "name")
  val positionShadow = ("type" → "int") ~ ("ref"    → "position")

  case class ImagePayload(id: Option[Int] = None,
                          src: String,
                          title: Option[String] = None,
                          alt: Option[String] = None) {
    def toJson: Json = ("id" → id) ~ ("src" → src) ~ ("title" → title) ~ ("alt" → alt)
  }

  case class CreateAlbumPayload(name: String, images: Images = None, position: Option[Int] = None)
      extends Validation[CreateAlbumPayload] {
    def objectForm: ObjectForm = {
      val imageJson   = images.map(_.map(_.toJson))
      val postionForm = position.fold(JObject())(pos ⇒ "position" → pos)
      val json        = ("name" → name) ~ ("images" → imageJson) ~ postionForm
      ObjectForm(kind = Album.kind, attributes = json)
    }

    def objectShadow: ObjectShadow = {
      val imageJson    = "images"                                → images.map(_ ⇒ imageShadow)
      val nameJson     = "name"                                  → nameShadow
      val positionJson = position.fold(JObject())(_ ⇒ "position" → positionShadow)
      ObjectShadow(attributes = nameJson ~ imageJson ~ positionJson)
    }

    def fillImageIds(): CreateAlbumPayload = {
      copy(images = images.map(generateIds))
    }

    override def validate: ValidatedNel[Failure, CreateAlbumPayload] = {
      validateIdsUnique(images).map(_ ⇒ this)
    }
  }

  case class UpdateAlbumPayload(name: Option[String] = None,
                                images: Images = None,
                                position: Option[Int] = None)
      extends Validation[UpdateAlbumPayload] {
    val objectForm: ObjectForm = {
      def imageJson(): JField    = "images"   → images.get.map(_.toJson)
      def nameJson(): JField     = "name"     → name.get
      def positionJson(): JField = "position" → position.get

      val tuples: List[(Option[Any], () ⇒ (String, JValue))] = List(name → nameJson,
                                                                    images → imageJson,
                                                                    position →
                                                                      positionJson)

      val json = JObject(tuples.collect { case (value, jfield) if value.isDefined ⇒ jfield() }: _*)
      // TODO: validate payload to prohibit both `None`s
      ObjectForm(kind = Album.kind, attributes = json)
    }

    def objectShadow: ObjectShadow = {
      val imageJson    = images.fold(JObject())(img ⇒ "images"   → img.map(_.toJson))
      val nameJson     = name.fold(JObject())(n ⇒ "name"         → n)
      val positionJson = position.fold(JObject())(p ⇒ "position" → p)
      ObjectShadow(attributes = nameJson ~ imageJson ~ positionJson)
    }

    def fillImageIds(): UpdateAlbumPayload = {
      copy(images = images.map(generateIds))
    }

    override def validate: ValidatedNel[Failure, UpdateAlbumPayload] = {
      validateIdsUnique(images).map(_ ⇒ this)
    }
  }

  def validateIdsUnique(images: Images): ValidatedNel[Failure, Images] = images match {
    case Some(imgList) ⇒
      val (withId, withoutId) = imgList.span(_.id.isDefined)
      val duplicated =
        withId.groupBy(_.id.get).mapValues(_.size).filter { case (id, count) ⇒ count > 1 }
      validExpr(duplicated.isEmpty, s"Image id is duplicated ${duplicated.head._1}").map(_ ⇒
            images)
    case None ⇒
      Validated.valid(images)
  }
}
