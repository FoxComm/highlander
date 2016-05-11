package services.variant

import failures.ProductFailures._
import models.objects._
import models.product._
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object VariantManager {
  def mustFindVariantsByFullProduct(product: FullObject[Product])
    (implicit ec: EC): DbResultT[Seq[FullObject[Variant]]] = for {
    links    ← * <~ ObjectLinks.findByLeftAndType(product.shadow.id, ObjectLink.ProductVariant).result
    variants ← * <~ DbResultT.sequence(links.map(link ⇒ 
      for {
        shadow ← * <~ ObjectManager.mustFindShadowById404(link.rightId)
        form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
        variant ← * <~ Variants.filterByContextAndFormId(product.model.contextId, form.id).one.
          mustFindOr(VariantNotFoundForContext(form.id, product.model.contextId))
      } yield FullObject(variant, form, shadow)))
  } yield variants
}
