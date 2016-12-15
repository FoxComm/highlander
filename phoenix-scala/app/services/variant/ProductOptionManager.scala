package services.variant

import failures.ArchiveFailures._
import failures.NotFoundFailure404
import failures.ProductFailures._
import models.inventory.ProductVariant
import models.account._
import models.objects._
import models.product._
import payloads.ProductOptionPayloads._
import responses.VariantResponses.IlluminatedVariantResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import services.inventory.SkuManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ProductOptionManager {
  type FullVariant = (FullObject[ProductOption], Seq[FullObject[ProductValue]])

  def createVariant(contextName: String, payload: ProductOptionPayload)(
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
          variant = IlluminatedProductOption.illuminate(context, variant),
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

      values ← * <~ ProductOptionValueLinks.queryRightByLeft(fullVariant.model)
      variantValueSkuCodes ← * <~ ProductOptionManager.getVariantValueSkuCodes(
                                values.map(_.model.id))
    } yield
      IlluminatedVariantResponse.build(
          variant = IlluminatedProductOption.illuminate(context, fullVariant),
          variantValues = values,
          variantValueSkus = variantValueSkuCodes
      )

  def updateVariant(contextName: String, variantId: Int, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedVariantResponse.Root] =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ updateVariantInner(context, variantId, payload)
      (variant, values) = fullVariant
      variantValueSkuCodes ← * <~ ProductOptionManager.getVariantValueSkuCodes(
                                values.map(_.model.id))
    } yield
      IlluminatedVariantResponse.build(
          variant = IlluminatedProductOption.illuminate(context, variant),
          variantValues = values,
          variantValueSkus = variantValueSkuCodes
      )

  def createVariantInner(
      context: ObjectContext,
      payload: ProductOptionPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[FullVariant] = {

    val form          = ObjectForm.fromPayload(ProductOption.kind, payload.attributes)
    val shadow        = ObjectShadow.fromPayload(payload.attributes)
    val variantValues = payload.values.getOrElse(Seq.empty)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      variant ← * <~ ProductOptions.create(
                   ProductOption(scope = scope,
                                 contextId = context.id,
                                 formId = ins.form.id,
                                 shadowId = ins.shadow.id,
                                 commitId = ins.commit.id))
      values ← * <~ variantValues.map(createVariantValueInner(context, variant, _))
    } yield (FullObject(variant, ins.form, ins.shadow), values)
  }

  def updateVariantInner(context: ObjectContext, variantId: Int, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullVariant] = {

    val newFormAttrs   = ObjectForm.fromPayload(ProductOption.kind, payload.attributes).attributes
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

      values ← * <~ ProductOptionValueLinks.queryRightByLeft(oldVariant.model)
    } yield (FullObject(updatedHead, updated.form, updated.shadow), values)
  }

  def updateOrCreateVariant(
      context: ObjectContext,
      payload: ProductOptionPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[FullVariant] = {

    payload.id match {
      case Some(id) ⇒ updateVariantInner(context, id, payload)
      case None     ⇒ createVariantInner(context, payload)
    }
  }

  private def updateHead(
      variant: ProductOption,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[ProductOption] =
    maybeCommit match {
      case Some(commit) ⇒
        ProductOptions.update(variant, variant.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(variant)
    }

  def createVariantValue(contextName: String, variantId: Int, payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedVariantValueResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ ProductOptions
                 .filterByContextAndFormId(context.id, variantId)
                 .mustFindOneOr(VariantNotFoundForContext(variantId, context.id))
      value ← * <~ createVariantValueInner(context, variant, payload)
    } yield IlluminatedVariantValueResponse.build(value, payload.skuCodes)

  private def createVariantValueInner(context: ObjectContext,
                                      variant: ProductOption,
                                      payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullObject[ProductValue]] = {

    val (form, shadow) = payload.formAndShadow.tupled

    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      skuCodes ← * <~ payload.skuCodes.map(SkuManager.mustFindSkuByContextAndCode(context.id, _))
      _ ← * <~ skuCodes.map(sku ⇒
               DbResultT.fromXor(sku.mustNotBeArchived(ProductOption, variant.formId)))
      ins ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      variantValue ← * <~ ProductValues.create(
                        ProductValue(scope = scope,
                                     contextId = context.id,
                                     formId = ins.form.id,
                                     shadowId = ins.shadow.id,
                                     commitId = ins.commit.id))
      _ ← * <~ ProductOptionValueLinks.create(
             ProductOptionValueLink(leftId = variant.id, rightId = variantValue.id))
      _ ← * <~ skuCodes.map(
             s ⇒
               ProductValueVariantLinks.create(
                   ProductValueVariantLink(leftId = variantValue.id, rightId = s.id)))
    } yield FullObject(variantValue, ins.form, ins.shadow)
  }

  private def updateVariantValueInner(valueId: Int, contextId: Int, payload: VariantValuePayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[ProductValue]] = {
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

      variantValueLinks ← * <~ ProductValueVariantLinks.filterLeft(value.id).result
      linkedSkuIds = variantValueLinks.map(_.rightId).toSet

      toDelete = variantValueLinks.filter(link ⇒ !newSkuIds.contains(link.rightId))
      toCreate = newSkuIds.diff(linkedSkuIds)

      _ ← * <~ ProductValueVariantLinks.createAllReturningIds(
             toCreate.map(id ⇒ ProductValueVariantLink(leftId = value.id, rightId = id)))
      _ ← * <~ toDelete.map(
             link ⇒
               ProductValueVariantLinks
                 .deleteById(link.id, DbResultT.unit, id ⇒ NotFoundFailure404(link, link.id)))
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  private def updateOrCreateVariantValue(
      variant: ProductOption,
      context: ObjectContext,
      payload: VariantValuePayload)(implicit ec: EC, db: DB, au: AU) = {

    payload.id match {
      case Some(id) ⇒ updateVariantValueInner(id, context.id, payload)
      case None     ⇒ createVariantValueInner(context, variant, payload)
    }
  }

  private def updateValueHead(
      value: ProductValue,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[ProductValue] =
    maybeCommit match {
      case Some(commit) ⇒
        ProductValues.update(value, value.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(value)
    }

  def findVariantsByProduct(product: Product)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[(FullObject[ProductOption], Seq[FullObject[ProductValue]])]] =
    for {
      variants ← * <~ ProductOptionLinks.queryRightByLeft(product)
      values   ← * <~ variants.map(findValuesForVariant)
    } yield variants.zip(values)

  def findValuesForVariant(variant: FullObject[ProductOption])(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[ProductValue]]] =
    ProductOptionValueLinks.queryRightByLeft(variant.model)

  def getVariantValueSkuCodes(
      variantValueHeadIds: Seq[Int])(implicit ec: EC, db: DB): DbResultT[Map[Int, Seq[String]]] =
    for {
      links ← * <~ ProductValueVariantLinks
               .findVariantsForProductValues(variantValueHeadIds)
               .result
      linksMapping = links.groupBy { case (valueId, _) ⇒ valueId }.mapValues(_.map {
        case (_, skuCode) ⇒ skuCode
      })
    } yield linksMapping

  private def mustFindVariantValueByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[ProductValue] =
    for {

      value ← * <~ ProductValues
               .filterByContextAndFormId(contextId, formId)
               .mustFindOneOr(VariantValueNotFoundForContext(formId, contextId))
    } yield value

  def mustFindVariantValueByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[ProductValue]] =
    for {

      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      value ← * <~ ProductValues
               .filterByContextAndFormId(contextId, form.id)
               .mustFindOneOr(VariantValueNotFoundForContext(form.id, contextId))
    } yield FullObject(value, form, shadow)

  private def mustFindVariantByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC): DbResultT[ProductOption] =
    for {
      variant ← * <~ ProductOptions
                 .filterByContextAndFormId(contextId, formId)
                 .mustFindOneOr(VariantNotFoundForContext(formId, contextId))
    } yield variant

  def mustFindVariantByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[ProductOption]] =
    for {
      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      variant ← * <~ ProductOptions
                 .filterByContextAndFormId(contextId, form.id)
                 .mustFindOneOr(VariantNotFoundForContext(form.id, contextId))
    } yield FullObject(variant, form, shadow)

  def mustFindFullVariantWithValuesById(
      id: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[FullVariant] =
    for {
      fullVariant ← * <~ ObjectManager.getFullObject(ProductOptions.mustFindById404(id))
      values      ← * <~ findValuesForVariant(fullVariant)
    } yield (fullVariant, values)

  def zipVariantWithValues(variant: FullObject[ProductOption])(implicit ec: EC,
                                                               db: DB,
                                                               oc: OC): DbResultT[FullVariant] =
    findValuesForVariant(variant).map(values ⇒ variant → values)
}
