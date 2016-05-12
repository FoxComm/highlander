package services.variant

import failures.GeneralFailure
import failures.ProductFailures._
import models.objects._
import models.product._
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object VariantManager {
  def findVariantsByProduct(product: Product)
    (implicit ec: EC): DbResultT[Seq[FullObject[Variant]]] = for {
    links    ← * <~ ObjectLinks.findByLeftAndType(product.shadowId, ObjectLink.ProductVariant).result
    variants ← * <~ DbResultT.sequence(links.map {
      link ⇒ mustFindVariantByContextAndShadow(product.contextId, link.rightId)
    })
  } yield variants

  def findVariantForValue(variantValue: VariantValue)
    (implicit ec: EC): DbResultT[FullObject[Variant]] = for {
    link    ← * <~ ObjectLinks.findByRightAndType(variantValue.shadowId, ObjectLink.VariantValue).
      one.mustFindOr(GeneralFailure("ST"))
    variant ← * <~ mustFindVariantByContextAndShadow(variantValue.contextId, variantValue.shadowId)
  } yield variant

  private def mustFindVariantByContextAndShadow(contextId: Int, shadowId: Int)
    (implicit ec: EC): DbResultT[FullObject[Variant]] = for {
    shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
    form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
    variant ← * <~ Variants.filterByContextAndFormId(contextId, form.id).one.
      mustFindOr(VariantNotFoundForContext(form.id, contextId))
  } yield FullObject(variant, form, shadow)

}
