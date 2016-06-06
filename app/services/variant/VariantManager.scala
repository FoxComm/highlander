package services.variant

import failures.ObjectFailures._
import failures.ProductFailures._
import models.objects._
import models.product._
import payloads.VariantPayloads.{CreateVariantPayload, CreateVariantValuePayload}
import responses.VariantResponses.IlluminatedVariantResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import services.Result
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object VariantManager {
  type FullVariant = (FullObject[Variant], Seq[FullObject[VariantValue]])

  def createVariant(contextName: String, payload: CreateVariantPayload)(
      implicit ec: EC, db: DB): Result[IlluminatedVariantResponse.Root] =
    (for {

      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ createVariantInner(context, payload)
      (variant, values) = fullVariant
    } yield
      IlluminatedVariantResponse.build(
          v = IlluminatedVariant.illuminate(context, variant),
          vs = values
      )).runTxn()

  def createVariantInner(context: ObjectContext, payload: CreateVariantPayload)(
      implicit ec: EC, db: DB): DbResultT[FullVariant] = {

    val form          = ObjectForm.fromPayload(Variant.kind, payload.attributes)
    val shadow        = ObjectShadow.fromPayload(payload.attributes)
    val variantValues = payload.values.getOrElse(Seq.empty)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      variant ← * <~ Variants.create(
                   Variant(contextId = context.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))
      values ← * <~ variantValues.map(createVariantValueInner(context, variant, _))
    } yield (FullObject(variant, ins.form, ins.shadow), values)
  }

  def createVariantValue(contextName: String, variantId: Int, payload: CreateVariantValuePayload)(
      implicit ec: EC, db: DB): Result[IlluminatedVariantValueResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ Variants
                 .filterByContextAndFormId(context.id, variantId)
                 .mustFindOneOr(VariantNotFoundForContext(variantId, context.id))
      value ← * <~ createVariantValueInner(context, variant, payload)
    } yield IlluminatedVariantValueResponse.build(value)).runTxn()

  def createVariantValueInner(
      context: ObjectContext, variant: Variant, payload: CreateVariantValuePayload)(
      implicit ec: EC, db: DB): DbResultT[FullObject[VariantValue]] = {

    val form   = payload.objectForm
    val shadow = payload.objectShadow

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      variantValue ← * <~ VariantValues.create(
                        VariantValue(contextId = context.id,
                                     formId = ins.form.id,
                                     shadowId = ins.shadow.id,
                                     commitId = ins.commit.id))
      _ ← * <~ ObjectLinks.create(ObjectLink(leftId = variant.shadowId,
                                             rightId = variantValue.shadowId,
                                             linkType = ObjectLink.VariantValue))
    } yield FullObject(variantValue, ins.form, ins.shadow)
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
