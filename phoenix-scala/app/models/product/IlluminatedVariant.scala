package models.product

import java.time.Instant

import models.objects._
import slick.driver.PostgresDriver.api._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._

case class IlluminatedVariant(id: Int,
                              context: IlluminatedContext,
                              attributes: Json,
                              archivedAt: Option[Instant],
                              values: Seq[IlluminatedVariantValue] = Seq.empty)

object IlluminatedVariant {
  def illuminate(c: ObjectContext,
                 v: FullObject[Variant],
                 values: Seq[IlluminatedVariantValue] = Seq.empty): IlluminatedVariant =
    IlluminatedVariant(
        id = v.form.id,
        context = IlluminatedContext(c.name, c.attributes),
        archivedAt = v.model.archivedAt,
        attributes = IlluminateAlgorithm.projectAttributes(v.form.attributes, v.shadow.attributes),
        values = values)

  def findByProductId(productHeadId: Int)(implicit ec: EC,
                                          oc: OC): DbResultT[Seq[IlluminatedVariant]] =
    for {
      variants    ← * <~ filterByProductId(productHeadId).result
      illuminated ← * <~ illuminateWithDependencies(variants)
    } yield illuminated

  private def illuminateWithDependencies(
      variants: Seq[(Variant, ObjectForm, ObjectShadow)])(implicit ec: EC, oc: OC) =
    variants.map {
      case (variant, form, shadow) ⇒
        val fullVariant = FullObject(variant, form, shadow)
        IlluminatedVariantValue.findByVariantId(variant.id).map { values ⇒
          illuminate(oc, fullVariant, values)
        }
    }

  private def filterByProductId(productHeadId: Int) =
    for {
      (heads, _) ← Variants.join(ProductVariantLinks).on(_.id === _.rightId).filter {
                    case (_, link) ⇒ link.leftId === productHeadId
                  }
      forms   ← ObjectForms if heads.formId === forms.id
      shadows ← ObjectShadows if heads.shadowId === shadows.id
    } yield (heads, forms, shadows)
}
