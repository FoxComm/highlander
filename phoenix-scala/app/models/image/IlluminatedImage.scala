package models.image

import java.time.Instant

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class IlluminatedImage(id: Int,
                            context: ObjectContext,
                            attributes: Json,
                            archivedAt: Option[Instant])

object IlluminatedImage {
  def illuminate(context: ObjectContext,
                 image: Image,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedImage =
    IlluminatedImage(id = form.id,
                     context = context,
                     attributes =
                       IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes),
                     archivedAt = image.archivedAt)

  def findByAlbumId(albumHeadId: Int)(implicit ec: EC, oc: OC): DbResultT[Seq[IlluminatedImage]] =
    for {
      images ← * <~ filterByAlbumId(albumHeadId).result
    } yield
      images.map {
        case (head, form, shadow) ⇒ illuminate(oc, head, form, shadow)
      }

  private def filterByAlbumId(albumHeadId: Int) =
    for {
      (heads, _) ← Images.join(AlbumImageLinks).on(_.id === _.rightId).filter {
                    case (_, link) ⇒ link.leftId === albumHeadId
                  }
      forms   ← ObjectForms if heads.formId === forms.id
      shadows ← ObjectShadows if heads.shadowId === shadows.id
    } yield (heads, forms, shadows)
}
