package services.variant

import failures.GeneralFailure
import failures.ObjectFailures._
import failures.ProductFailures._
import models.objects._
import models.product._
import payloads.VariantPayloads.CreateVariantPayload
import responses.VariantResponses.IlluminatedVariantResponse
import services.Result
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object VariantManager {
  def createVariant(contextName: String, payload: CreateVariantPayload)(
      implicit ec: EC, db: DB): Result[IlluminatedVariantResponse.Root] =
    (for {

      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ createVariantInner(context, payload)
    } yield
      IlluminatedVariantResponse.build(
          v = IlluminatedVariant.illuminate(context, variant),
          vs = Seq.empty
      )).runTxn()

  def createVariantInner(context: ObjectContext, payload: CreateVariantPayload)(
      implicit ec: EC, db: DB): DbResultT[FullObject[Variant]] = {

    val form   = ObjectForm.fromPayload(Variant.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      variant ← * <~ Variants.create(
                   Variant(contextId = context.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))
    } yield FullObject(variant, ins.form, ins.shadow)
  }

  def findVariantsByProduct(
      product: Product)(implicit ec: EC): DbResultT[Seq[FullObject[Variant]]] =
    for {
      links ← * <~ ObjectLinks
               .findByLeftAndType(product.shadowId, ObjectLink.ProductVariant)
               .result
      variants ← * <~ links.map { link ⇒
                  mustFindVariantByContextAndShadow(product.contextId, link.rightId)
                }
    } yield variants

  def findVariantForValue(
      variantValue: VariantValue)(implicit ec: EC): DbResultT[FullObject[Variant]] =
    for {
      link ← * <~ ObjectLinks
              .findByRightAndType(variantValue.shadowId, ObjectLink.VariantValue)
              .mustFindOneOr(ObjectRightLinkCannotBeFound(variantValue.shadowId))
      variant ← * <~ mustFindVariantByContextAndShadow(
                   variantValue.contextId, variantValue.shadowId)
    } yield variant

  def mustFindVariantValueByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[VariantValue]] =
    for {

      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      value ← * <~ VariantValues
               .filterByContextAndFormId(contextId, form.id)
               .mustFindOneOr(VariantValueNotFoundForContext(form.id, contextId))
    } yield FullObject(value, form, shadow)

  private def mustFindVariantByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[Variant]] =
    for {
      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      variant ← * <~ Variants
                 .filterByContextAndFormId(contextId, form.id)
                 .mustFindOneOr(VariantNotFoundForContext(form.id, contextId))
    } yield FullObject(variant, form, shadow)
}
