package services.variant

import failures.NotFoundFailure404
import failures.ObjectFailures._
import failures.ProductFailures._
import models.objects._
import models.product._
import payloads.VariantPayloads._
import responses.VariantResponses.IlluminatedVariantResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import services.inventory.SkuManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object VariantManager {
  type FullVariant = (FullObject[Variant], Seq[FullObject[VariantValue]])

  def createVariant(contextName: String, payload: VariantPayload)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ createVariantInner(context, payload)
      (variant, values) = fullVariant
      variantToSkuMapping = payload.values
        .getOrElse(Seq.empty)
        .zip(values)
        .collect {
          case (vvPayload, vvDb) if vvPayload.skuCode.isDefined ⇒
            (vvDb.model.id, vvPayload.skuCode.get)
        }
        .toMap
    } yield
      IlluminatedVariantResponse.build(
          variant = IlluminatedVariant.illuminate(context, variant),
          variantValues = values,
          variantValueSkuCodeLinks = variantToSkuMapping
      )

  def getVariant(contextName: String, variantId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ ObjectManager.getFullObject(
                       mustFindVariantByContextAndForm(context.id, variantId))

      links ← * <~ ObjectLinks
               .findByLeftAndType(fullVariant.shadow.id, ObjectLink.VariantValue)
               .result
      values ← * <~ links.map(link ⇒
                    mustFindVariantValueByContextAndShadow(context.id, link.rightId))
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(values.map(_.model.id))
    } yield
      IlluminatedVariantResponse.build(
          variant = IlluminatedVariant.illuminate(context, fullVariant),
          variantValues = values,
          variantValueSkuCodeLinks = variantValueSkuCodes
      )

  def updateVariant(contextName: String, variantId: Int, payload: VariantPayload)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ updateVariantInner(context, variantId, payload)
      (variant, values) = fullVariant
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(values.map(_.model.id))
    } yield
      IlluminatedVariantResponse.build(
          variant = IlluminatedVariant.illuminate(context, variant),
          variantValues = values,
          variantValueSkuCodeLinks = variantValueSkuCodes
      )

  def createVariantInner(context: ObjectContext, payload: VariantPayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullVariant] = {

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
      implicit ec: EC,
      db: DB): DbResultT[FullVariant] = {

    val newFormAttrs   = ObjectForm.fromPayload(Variant.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val valuePayloads  = payload.values.getOrElse(Seq.empty)

    for {
      oldVariant ← * <~ ObjectManager.getFullObject(
                      mustFindVariantByContextAndForm(context.id, variantId))

      mergedAttrs = oldVariant.shadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(oldVariant.form.id,
                                        oldVariant.shadow.id,
                                        newFormAttrs,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldVariant.model, updated.shadow, commit)

      _ ← * <~ valuePayloads.map(pay ⇒ updateOrCreateVariantValue(updatedHead, context, pay))

      _ ← * <~ ObjectUtils.updateAssociatedLefts(Products,
                                                 context.id,
                                                 oldVariant.shadow.id,
                                                 updatedHead.shadowId,
                                                 ObjectLink.ProductVariant)

      valueLinks ← * <~ ObjectLinks
                    .findByLeftAndType(oldVariant.shadow.id, ObjectLink.VariantValue)
                    .result
      _ ← * <~ ObjectUtils.updateAssociatedRights(VariantValues, valueLinks, updatedHead.shadowId)

      links ← * <~ ObjectLinks
               .findByLeftAndType(updatedHead.shadowId, ObjectLink.VariantValue)
               .result
      values ← * <~ links.map(link ⇒
                    mustFindVariantValueByContextAndShadow(context.id, link.rightId))
    } yield (FullObject(updatedHead, updated.form, updated.shadow), values)
  }

  def updateOrCreateVariant(context: ObjectContext, payload: VariantPayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullVariant] = {

    payload.id match {
      case Some(id) ⇒ updateVariantInner(context, id, payload)
      case None     ⇒ createVariantInner(context, payload)
    }
  }

  // Update the variant's links to the right (such as variant values).
  // This helps Variant stay in sync when a Product/SKU is updated.
  // Once we rip out ObjectLinks with shadows, this will be unnecessary.
  def updateVariantTree(newLeftId: Int, existingLinks: Seq[ObjectLink])(implicit ec: EC, db: DB) = {}

  private def updateHead(variant: Variant,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Variant] =
    maybeCommit match {
      case Some(commit) ⇒
        Variants.update(variant, variant.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(variant)
    }

  def createVariantValue(contextName: String, variantId: Int, payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedVariantValueResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ Variants
                 .filterByContextAndFormId(context.id, variantId)
                 .mustFindOneOr(VariantNotFoundForContext(variantId, context.id))
      value ← * <~ createVariantValueInner(context, variant, payload)
    } yield IlluminatedVariantValueResponse.build(value, payload.skuCode)

  private def createVariantValueInner(context: ObjectContext,
                                      variant: Variant,
                                      payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[VariantValue]] = {

    val form   = payload.objectForm
    val shadow = payload.objectShadow

    for {
      skuOption ← * <~ payload.skuCode.map(SkuManager.mustFindSkuByContextAndCode(context.id, _))
      ins       ← * <~ ObjectUtils.insert(form, shadow)
      variantValue ← * <~ VariantValues.create(
                        VariantValue(contextId = context.id,
                                     formId = ins.form.id,
                                     shadowId = ins.shadow.id,
                                     commitId = ins.commit.id))
      _ ← * <~ ObjectLinks.create(
             ObjectLink(leftId = variant.shadowId,
                        rightId = variantValue.shadowId,
                        linkType = ObjectLink.VariantValue))
      _ ← * <~ skuOption.map(
             s ⇒
               VariantValueSkuLinks.create(
                   VariantValueSkuLink(leftId = variantValue.id, rightId = s.id)))
    } yield FullObject(variantValue, ins.form, ins.shadow)
  }

  private def updateVariantValueInner(valueId: Int, contextId: Int, payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[VariantValue]] = {

    val newFormAttrs   = payload.objectForm.attributes
    val newShadowAttrs = payload.objectShadow.attributes

    for {
      value     ← * <~ mustFindVariantValueByContextAndForm(contextId, valueId)
      oldForm   ← * <~ ObjectForms.mustFindById404(value.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(value.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateValueHead(value, updated.shadow, commit)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(Variants,
                                                 value.contextId,
                                                 oldShadow.id,
                                                 updatedHead.shadowId,
                                                 ObjectLink.VariantValue)
      newSku           ← * <~ payload.skuCode.map(SkuManager.mustFindSkuByContextAndCode(contextId, _))
      variantValueLink ← * <~ VariantValueSkuLinks.filterLeft(valueId).result.headOption
      _ ← * <~ ((variantValueLink, newSku) match {
               case (Some(link), None) ⇒
                 VariantValueSkuLinks
                   .deleteById(link.id, DbResult.unit, id ⇒ NotFoundFailure404(link, id))
               case (Some(link), Some(sku)) ⇒
                 VariantValueSkuLinks.update(link, link.copy(rightId = sku.id))
               case (None, Some(sku)) ⇒
                 VariantValueSkuLinks.create(
                     new VariantValueSkuLink(leftId = valueId, rightId = sku.id))
               case _ ⇒ DbResultT.unit
             })
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  private def updateOrCreateVariantValue(variant: Variant,
                                         context: ObjectContext,
                                         payload: VariantValuePayload)(implicit ec: EC, db: DB) = {

    payload.id match {
      case Some(id) ⇒ updateVariantValueInner(id, context.id, payload)
      case None     ⇒ createVariantValueInner(context, variant, payload)
    }
  }

  private def updateValueHead(
      value: VariantValue,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[VariantValue] =
    maybeCommit match {
      case Some(commit) ⇒
        VariantValues.update(value, value.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(value)
    }

  def findVariantsByProduct(product: Product)(
      implicit ec: EC): DbResultT[Seq[(FullObject[Variant], Seq[FullObject[VariantValue]])]] =
    for {
      links ← * <~ ObjectLinks
               .findByLeftAndType(product.shadowId, ObjectLink.ProductVariant)
               .result
      variants ← * <~ links.map { link ⇒
                  mustFindVariantByContextAndShadow(product.contextId, link.rightId)
                }
      values ← * <~ variants.map(v ⇒ findValuesForVariant(product.contextId, v.shadow.id))
    } yield variants.zip(values)

  def findVariantForValue(variantValue: VariantValue)(
      implicit ec: EC): DbResultT[FullObject[Variant]] =
    for {
      link ← * <~ ObjectLinks
              .findByRightAndType(variantValue.shadowId, ObjectLink.VariantValue)
              .mustFindOneOr(ObjectRightLinkCannotBeFound(variantValue.shadowId))
      variant ← * <~ mustFindVariantByContextAndShadow(variantValue.contextId,
                                                       variantValue.shadowId)
    } yield variant

  def findValuesForVariant(contextId: Int, variantShadowId: Int)(
      implicit ec: EC): DbResultT[Seq[FullObject[VariantValue]]] =
    for {
      links ← * <~ ObjectLinks.findByLeftAndType(variantShadowId, ObjectLink.VariantValue).result
      values ← * <~ links.map(link ⇒
                    mustFindVariantValueByContextAndShadow(contextId, link.rightId))
    } yield values

  def getVariantValueSkuCodes(variantValueHeadIds: Seq[Int])(
      implicit ec: EC): DbResultT[Map[Int, String]] =
    for {
      links ← * <~ VariantValueSkuLinks.findSkusForVariantValues(variantValueHeadIds).result
    } yield links.toMap

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

  def mustFindVariantByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[Variant]] =
    for {
      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      variant ← * <~ Variants
                 .filterByContextAndFormId(contextId, form.id)
                 .mustFindOneOr(VariantNotFoundForContext(form.id, contextId))
    } yield FullObject(variant, form, shadow)
}
