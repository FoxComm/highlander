package models.inventory

import java.time.Instant

import models.image._
import models.objects._
import slick.driver.PostgresDriver.api._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._

/**
  * An IlluminatedSku is what you get when you combine the sku shadow and
  * the sku.
  */
case class IlluminatedSku(id: Int,
                          code: String,
                          archivedAt: Option[Instant],
                          context: IlluminatedContext,
                          attributes: Json,
                          albums: Seq[IlluminatedAlbum] = Seq.empty)

object IlluminatedSku {

  def illuminate(context: ObjectContext,
                 sku: FullObject[Sku],
                 albums: Seq[IlluminatedAlbum] = Seq.empty): IlluminatedSku = {
    val model       = sku.model
    val formAttrs   = sku.form.attributes
    val shadowAttrs = sku.shadow.attributes

    IlluminatedSku(id = sku.form.id,
                   code = model.code,
                   archivedAt = model.archivedAt,
                   context = IlluminatedContext(context.name, context.attributes),
                   attributes = IlluminateAlgorithm.projectAttributes(formAttrs, shadowAttrs))
  }

  def findAllByProductId(productHeadId: Int)(implicit ec: EC,
                                             oc: OC): DbResultT[Seq[IlluminatedSku]] =
    for {
      skus        ← * <~ filterByProductId(productHeadId).result
      illuminated ← * <~ illuminateWithDependents(skus)
    } yield illuminated

  private def illuminateWithDependents(skus: Seq[(Sku, ObjectForm, ObjectShadow)])(implicit ec: EC,
                                                                                   oc: OC) =
    skus.map {
      case (sku, form, shadow) ⇒
        val fullSku = FullObject(sku, form, shadow)
        IlluminatedAlbum.findBySkuId(sku.id).map(albums ⇒ illuminate(oc, fullSku, albums))
    }

  private def filterByProductId(productHeadId: Int) =
    for {
      (heads, _) ← Skus.join(ProductSkuLinks).on(_.id === _.rightId).filter {
                    case (_, link) ⇒ link.leftId === productHeadId
                  }
      forms   ← ObjectForms if heads.formId === forms.id
      shadows ← ObjectShadows if heads.shadowId === shadows.id
    } yield (heads, forms, shadows)
}
