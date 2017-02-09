package models.product

import models.objects._
import slick.driver.PostgresDriver.api._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._

case class IlluminatedVariantValue(id: Int, context: IlluminatedContext, attributes: Json)

object IlluminatedVariantValue {
  def illuminate(c: ObjectContext,
                 head: VariantValue,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedVariantValue =
    illuminate(c, FullObject(head, form, shadow))

  def illuminate(c: ObjectContext, v: FullObject[VariantValue]): IlluminatedVariantValue =
    IlluminatedVariantValue(
        id = v.form.id,
        context = IlluminatedContext(c.name, c.attributes),
        attributes = IlluminateAlgorithm.projectAttributes(v.form.attributes, v.shadow.attributes))

  def findByVariantId(variantHeadId: Int)(implicit ec: EC,
                                          oc: OC): DbResultT[Seq[IlluminatedVariantValue]] =
    for {
      values ← * <~ filterByVariantId(variantHeadId).result
    } yield
      values.map {
        case (head, form, shadow) ⇒ illuminate(oc, head, form, shadow)
      }

  private def filterByVariantId(variantHeadId: Int) =
    for {
      (heads, _) ← VariantValues.join(VariantValueLinks).on(_.id === _.rightId).filter {
                    case (_, link) ⇒ link.leftId === variantHeadId
                  }
      forms   ← ObjectForms if heads.formId === forms.id
      shadows ← ObjectShadows if heads.shadowId === shadows.id
    } yield (heads, forms, shadows)
}
