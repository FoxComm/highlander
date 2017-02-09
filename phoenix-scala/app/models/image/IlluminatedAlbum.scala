package models.image

import java.time.Instant

import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class IlluminatedAlbum(id: Int,
                            context: ObjectContext,
                            attributes: Json,
                            archivedAt: Option[Instant],
                            images: Seq[IlluminatedImage] = Seq.empty)

object IlluminatedAlbum {
  def illuminate(context: ObjectContext,
                 album: FullObject[Album],
                 images: Seq[IlluminatedImage] = Seq.empty): IlluminatedAlbum =
    IlluminatedAlbum(
        id = album.form.id,
        context = context,
        attributes =
          IlluminateAlgorithm.projectAttributes(album.form.attributes, album.shadow.attributes),
        archivedAt = album.model.archivedAt,
        images = images)

  def findByProductId(productHeadId: Int)(implicit ec: EC,
                                          oc: OC): DbResultT[Seq[IlluminatedAlbum]] =
    for {
      albums      ← * <~ filterByProductId(productHeadId).result
      illuminated ← * <~ illuminateWithDependencies(albums)
    } yield illuminated

  def findBySkuId(skuHeadId: Int)(implicit ec: EC, oc: OC): DbResultT[Seq[IlluminatedAlbum]] =
    for {
      albums      ← * <~ filterBySkuId(skuHeadId).result
      illuminated ← * <~ illuminateWithDependencies(albums)
    } yield illuminated

  private def illuminateWithDependencies(
      albums: Seq[(Album, ObjectForm, ObjectShadow)])(implicit ec: EC, oc: OC) =
    albums.map {
      case (album, form, shadow) ⇒
        val fullAlbum = FullObject(album, form, shadow)
        IlluminatedImage.findByAlbumId(album.id).map(images ⇒ illuminate(oc, fullAlbum, images))
    }

  private def filterByProductId(productHeadId: Int) =
    for {
      (heads, _) ← Albums.join(ProductAlbumLinks).on(_.id === _.rightId).filter {
                    case (_, link) ⇒ link.leftId === productHeadId
                  }
      forms   ← ObjectForms if heads.formId === forms.id
      shadows ← ObjectShadows if heads.shadowId === shadows.id
    } yield (heads, forms, shadows)

  private def filterBySkuId(skuHeadId: Int) =
    for {
      (heads, _) ← Albums.join(SkuAlbumLinks).on(_.id === _.rightId).filter {
                    case (_, link) ⇒ link.leftId === skuHeadId
                  }
      forms   ← ObjectForms if heads.formId === forms.id
      shadows ← ObjectShadows if heads.shadowId === shadows.id
    } yield (heads, forms, shadows)
}
