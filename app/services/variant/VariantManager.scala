package services.variant

import failures.ObjectFailures._
import failures.ProductFailures._
import models.objects._
import models.product._
import payloads.VariantPayloads._
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

  def createVariant(contextName: String, payload: VariantPayload)(
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

  def getVariant(contextName: String, variantId: Int)(
      implicit ec: EC, db: DB): Result[IlluminatedVariantResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ mustFindVariantByContextAndForm(context.id, variantId)
      form    ← * <~ ObjectForms.mustFindById404(variant.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
      fullVariant = FullObject(variant, form, shadow)

      links ← * <~ ObjectLinks.findByLeftAndType(shadow.id, ObjectLink.VariantValue).result
      values ← * <~ links.map(link ⇒
                    mustFindVariantValueByContextAndShadow(context.id, link.rightId))
    } yield
      IlluminatedVariantResponse.build(
          v = IlluminatedVariant.illuminate(context, fullVariant),
          vs = values
      )).run()

  def updateVariant(contextName: String, variantId: Int, payload: VariantPayload)(
      implicit ec: EC, db: DB): Result[IlluminatedVariantResponse.Root] =
    (for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ updateVariantInner(context, variantId, payload)
      (variant, values) = fullVariant
    } yield
      IlluminatedVariantResponse.build(
          v = IlluminatedVariant.illuminate(context, variant),
          vs = values
      )).runTxn()

  def createVariantInner(context: ObjectContext, payload: VariantPayload)(
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

  def updateVariantInner(context: ObjectContext, variantId: Int, payload: VariantPayload)(
      implicit ec: EC, db: DB): DbResultT[FullVariant] = {

    val newFormAttrs   = ObjectForm.fromPayload(Variant.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val valuePayloads  = payload.values.getOrElse(Seq.empty)

    for {
      variant   ← * <~ mustFindVariantByContextAndForm(context.id, variantId)
      oldForm   ← * <~ ObjectForms.mustFindById404(variant.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(variant.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(
                   oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(variant, updated.shadow, commit)

      _ ← * <~ valuePayloads.map(pay ⇒ updateOrCreateVariantValue(updatedHead, context, pay))

      _ ← * <~ ObjectUtils.updateAssociatedLefts(
             Products, context.id, oldShadow.id, updatedHead.shadowId, ObjectLink.ProductVariant)
      _ ← * <~ ObjectUtils.updateAssociatedRights(VariantValues,
                                                  context.id,
                                                  oldShadow.id,
                                                  updatedHead.shadowId,
                                                  ObjectLink.VariantValue)

      links ← * <~ ObjectLinks
               .findByLeftAndType(updatedHead.shadowId, ObjectLink.VariantValue)
               .result
      values ← * <~ links.map(link ⇒
                    mustFindVariantValueByContextAndShadow(context.id, link.rightId))
    } yield (FullObject(updatedHead, updated.form, updated.shadow), values)
  }

  def updateOrCreateVariant(context: ObjectContext, payload: VariantPayload)(
      implicit ec: EC, db: DB): DbResultT[FullVariant] = {

    payload.id match {
      case Some(id) ⇒ updateVariantInner(context, id, payload)
      case None     ⇒ createVariantInner(context, payload)
    }
  }

  private def updateHead(
      variant: Variant, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[Variant] = maybeCommit match {
    case Some(commit) ⇒
      Variants.update(variant, variant.copy(shadowId = shadow.id, commitId = commit.id))
    case None ⇒
      DbResult.good(variant)
  }

  def createVariantValue(contextName: String, variantId: Int, payload: VariantValuePayload)(
      implicit ec: EC, db: DB): Result[IlluminatedVariantValueResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ Variants
                 .filterByContextAndFormId(context.id, variantId)
                 .mustFindOneOr(VariantNotFoundForContext(variantId, context.id))
      value ← * <~ createVariantValueInner(context, variant, payload)
    } yield IlluminatedVariantValueResponse.build(value)).runTxn()

  private def createVariantValueInner(
      context: ObjectContext, variant: Variant, payload: VariantValuePayload)(
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

  private def updateVariantValueInner(valueId: Int, contextId: Int, payload: VariantValuePayload)(
      implicit ec: EC, db: DB): DbResultT[FullObject[VariantValue]] = {

    val newFormAttrs   = payload.objectForm.attributes
    val newShadowAttrs = payload.objectShadow.attributes

    for {
      value     ← * <~ mustFindVariantValueByContextAndForm(contextId, valueId)
      oldForm   ← * <~ ObjectForms.mustFindById404(value.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(value.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(
                   oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateValueHead(value, updated.shadow, commit)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(Variants,
                                                 value.contextId,
                                                 oldShadow.id,
                                                 updatedHead.shadowId,
                                                 ObjectLink.VariantValue)
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  private def updateOrCreateVariantValue(
      variant: Variant, context: ObjectContext, payload: VariantValuePayload)(
      implicit ec: EC, db: DB) = {

    payload.id match {
      case Some(id) ⇒ updateVariantValueInner(id, context.id, payload)
      case None     ⇒ createVariantValueInner(context, variant, payload)
    }
  }

  private def updateValueHead(
      value: VariantValue, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[VariantValue] = maybeCommit match {
    case Some(commit) ⇒
      VariantValues.update(value, value.copy(shadowId = shadow.id, commitId = commit.id))
    case None ⇒
      DbResult.good(value)
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

  private def mustFindVariantValueByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC): DbResultT[VariantValue] =
    for {

      value ← * <~ VariantValues
               .filterByContextAndFormId(contextId, formId)
               .mustFindOneOr(VariantValueNotFoundForContext(formId, contextId))
    } yield value

  def mustFindVariantValueByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[VariantValue]] =
    for {

      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      value ← * <~ VariantValues
               .filterByContextAndFormId(contextId, form.id)
               .mustFindOneOr(VariantValueNotFoundForContext(form.id, contextId))
    } yield FullObject(value, form, shadow)

  private def mustFindVariantByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC): DbResultT[Variant] =
    for {
      variant ← * <~ Variants
                 .filterByContextAndFormId(contextId, formId)
                 .mustFindOneOr(VariantNotFoundForContext(formId, contextId))
    } yield variant

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
