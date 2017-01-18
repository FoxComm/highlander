package services.variant

import failures.ArchiveFailures._
import failures.NotFoundFailure404
import failures.ProductFailures._
import models.inventory.Sku
import models.account._
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
      db: DB,
      au: AU): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ createVariantInner(context, payload)
      (variant, values) = fullVariant
      variantToSkuMapping = payload.values
        .getOrElse(Seq.empty)
        .zip(values)
        .collect {
          case (vvPayload, vvDb) if vvPayload.skuCodes.nonEmpty ⇒
            (vvDb.model.id, vvPayload.skuCodes)
        }
        .toMap
    } yield
      IlluminatedVariantResponse.build(
        variant = IlluminatedVariant.illuminate(context, variant),
        variantValues = values,
        variantValueSkus = variantToSkuMapping
      )

  def getVariant(contextName: String, variantId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ ObjectManager.getFullObject(
        mustFindVariantByContextAndForm(context.id, variantId))

      values               ← * <~ VariantValueLinks.queryRightByLeft(fullVariant.model)
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(values.map(_.model.id))
    } yield
      IlluminatedVariantResponse.build(
        variant = IlluminatedVariant.illuminate(context, fullVariant),
        variantValues = values,
        variantValueSkus = variantValueSkuCodes
      )

  def updateVariant(contextName: String, variantId: Int, payload: VariantPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ updateVariantInner(context, variantId, payload)
      (variant, values) = fullVariant
      variantValueSkuCodes ← * <~ VariantManager.getVariantValueSkuCodes(values.map(_.model.id))
    } yield
      IlluminatedVariantResponse.build(
        variant = IlluminatedVariant.illuminate(context, variant),
        variantValues = values,
        variantValueSkus = variantValueSkuCodes
      )

  def createVariantInner(
      context: ObjectContext,
      payload: VariantPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[FullVariant] = {

    val form          = ObjectForm.fromPayload(Variant.kind, payload.attributes)
    val shadow        = ObjectShadow.fromPayload(payload.attributes)
    val variantValues = payload.values.getOrElse(Seq.empty)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      variant ← * <~ Variants.create(
        Variant(scope = scope,
                contextId = context.id,
                formId = ins.form.id,
                shadowId = ins.shadow.id,
                commitId = ins.commit.id))
      values ← * <~ variantValues.map(createVariantValueInner(context, variant, _))
    } yield (FullObject(variant, ins.form, ins.shadow), values)
  }

  def updateVariantInner(context: ObjectContext, variantId: Int, payload: VariantPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullVariant] = {

    val newFormAttrs   = ObjectForm.fromPayload(Variant.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val valuePayloads  = payload.values.getOrElse(Seq.empty)

    for {
      oldVariant ← * <~ ObjectManager.getFullObject(
        mustFindVariantByContextAndForm(context.id, variantId))

      mergedAttrs = oldVariant.shadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils
        .update(oldVariant.form.id, oldVariant.shadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldVariant.model, updated.shadow, commit)

      _ ← * <~ valuePayloads.map(pay ⇒ updateOrCreateVariantValue(updatedHead, context, pay))

      values ← * <~ VariantValueLinks.queryRightByLeft(oldVariant.model)
    } yield (FullObject(updatedHead, updated.form, updated.shadow), values)
  }

  def updateOrCreateVariant(
      context: ObjectContext,
      payload: VariantPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[FullVariant] = {

    payload.id match {
      case Some(id) ⇒ updateVariantInner(context, id, payload)
      case None     ⇒ createVariantInner(context, payload)
    }
  }

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
      db: DB,
      au: AU): DbResultT[IlluminatedVariantValueResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ Variants
        .filterByContextAndFormId(context.id, variantId)
        .mustFindOneOr(VariantNotFoundForContext(variantId, context.id))
      value ← * <~ createVariantValueInner(context, variant, payload)
    } yield IlluminatedVariantValueResponse.build(value, payload.skuCodes)

  private def createVariantValueInner(context: ObjectContext,
                                      variant: Variant,
                                      payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullObject[VariantValue]] = {

    val (form, shadow) = payload.formAndShadow.tupled

    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      skuCodes ← * <~ payload.skuCodes.map(SkuManager.mustFindSkuByContextAndCode(context.id, _))
      _ ← * <~ skuCodes.map(sku ⇒
        DbResultT.fromXor(sku.mustNotBeArchived(Variant, variant.formId)))
      ins ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      variantValue ← * <~ VariantValues.create(
        VariantValue(scope = scope,
                     contextId = context.id,
                     formId = ins.form.id,
                     shadowId = ins.shadow.id,
                     commitId = ins.commit.id))
      _ ← * <~ VariantValueLinks.create(
        VariantValueLink(leftId = variant.id, rightId = variantValue.id))
      _ ← * <~ skuCodes.map(s ⇒
        VariantValueSkuLinks.create(VariantValueSkuLink(leftId = variantValue.id, rightId = s.id)))
    } yield FullObject(variantValue, ins.form, ins.shadow)
  }

  private def updateVariantValueInner(valueId: Int, contextId: Int, payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[VariantValue]] = {
    val (form, shadow) = payload.formAndShadow.tupled

    for {
      value     ← * <~ mustFindVariantValueByContextAndForm(contextId, valueId)
      oldForm   ← * <~ ObjectForms.mustFindById404(value.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(value.shadowId)

      mergedAttrs = oldShadow.attributes.merge(shadow.attributes)
      updated ← * <~ ObjectUtils
        .update(oldForm.id, oldShadow.id, form.attributes, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateValueHead(value, updated.shadow, commit)

      newSkus ← * <~ payload.skuCodes.map(SkuManager.mustFindSkuByContextAndCode(contextId, _))
      newSkuIds = newSkus.map(_.id).toSet

      variantValueLinks ← * <~ VariantValueSkuLinks.filterLeft(value.id).result
      linkedSkuIds = variantValueLinks.map(_.rightId).toSet

      toDelete = variantValueLinks.filter(link ⇒ !newSkuIds.contains(link.rightId))
      toCreate = newSkuIds.diff(linkedSkuIds)

      _ ← * <~ VariantValueSkuLinks.createAllReturningIds(
        toCreate.map(id ⇒ VariantValueSkuLink(leftId = value.id, rightId = id)))
      _ ← * <~ toDelete.map(
        link ⇒
          VariantValueSkuLinks
            .deleteById(link.id, DbResultT.unit, id ⇒ NotFoundFailure404(link, link.id)))
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  private def updateOrCreateVariantValue(
      variant: Variant,
      context: ObjectContext,
      payload: VariantValuePayload)(implicit ec: EC, db: DB, au: AU) = {

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
      implicit ec: EC,
      db: DB): DbResultT[Seq[(FullObject[Variant], Seq[FullObject[VariantValue]])]] =
    for {
      variants ← * <~ ProductVariantLinks.queryRightByLeft(product)
      values   ← * <~ variants.map(findValuesForVariant)
    } yield variants.zip(values)

  def findValuesForVariant(variant: FullObject[Variant])(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[VariantValue]]] =
    VariantValueLinks.queryRightByLeft(variant.model)

  def getVariantValueSkuCodes(
      variantValueHeadIds: Seq[Int])(implicit ec: EC, db: DB): DbResultT[Map[Int, Seq[String]]] =
    for {
      links ← * <~ VariantValueSkuLinks.findSkusForVariantValues(variantValueHeadIds).result
      linksMapping = links.groupBy { case (valueId, _) ⇒ valueId }.mapValues(_.map {
        case (_, skuCode) ⇒ skuCode
      })
    } yield linksMapping

  private def mustFindVariantValueByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[VariantValue] =
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

  def mustFindFullVariantWithValuesById(
      id: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[FullVariant] =
    for {
      fullVariant ← * <~ ObjectManager.getFullObject(Variants.mustFindById404(id))
      values      ← * <~ findValuesForVariant(fullVariant)
    } yield (fullVariant, values)

  def zipVariantWithValues(
      variant: FullObject[Variant])(implicit ec: EC, db: DB, oc: OC): DbResultT[FullVariant] =
    findValuesForVariant(variant).map(values ⇒ variant → values)
}
