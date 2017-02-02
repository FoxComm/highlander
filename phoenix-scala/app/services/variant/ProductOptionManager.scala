package services.variant

import failures.NotFoundFailure404
import failures.ProductFailures._
import models.account._
import models.objects._
import models.product._
import payloads.ProductOptionPayloads._
import services.inventory.ProductVariantManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ProductOptionManager {
  type FullProductOption = (FullObject[ProductOption], Seq[FullObject[ProductOptionValue]])

  private def create(payload: ProductOptionPayload)(implicit ec: EC,
                                                    db: DB,
                                                    au: AU,
                                                    ctx: OC): DbResultT[FullProductOption] = {
    val form          = ObjectForm.fromPayload(ProductOption.kind, payload.attributes)
    val shadow        = ObjectShadow.fromPayload(payload.attributes)
    val productValues = payload.values.getOrElse(Seq.empty)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      productOption ← * <~ ProductOptions.create(
                         ProductOption(scope = scope,
                                       contextId = ctx.id,
                                       formId = ins.form.id,
                                       shadowId = ins.shadow.id,
                                       commitId = ins.commit.id))
      values ← * <~ productValues.map(createProductOptionValue(productOption, _))
    } yield (FullObject(productOption, ins.form, ins.shadow), values)
  }

  private def update(variantId: Int, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU,
      ctx: OC): DbResultT[FullProductOption] = {
    val newFormAttrs   = ObjectForm.fromPayload(ProductOption.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val valuePayloads  = payload.values.getOrElse(Seq.empty)

    for {
      oldVariant ← * <~ ObjectManager.getFullObject(mustFindByContextAndForm(variantId))

      mergedAttrs = oldVariant.shadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(oldVariant.form.id,
                                        oldVariant.shadow.id,
                                        newFormAttrs,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldVariant.model, updated.shadow, commit)

      _ ← * <~ valuePayloads.map(pay ⇒ updateOrCreateProductOptionValue(updatedHead, pay))

      values ← * <~ ProductOptionValueLinks.queryRightByLeft(oldVariant.model)
    } yield (FullObject(updatedHead, updated.form, updated.shadow), values)
  }

  def updateOrCreate(payload: ProductOptionPayload)(implicit ec: EC,
                                                    db: DB,
                                                    au: AU,
                                                    ctx: OC): DbResultT[FullProductOption] =
    payload.id match {
      case Some(id) ⇒ update(id, payload)
      case None     ⇒ create(payload)
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

  private def createProductOptionValue(variant: ProductOption, payload: ProductOptionValuePayload)(
      implicit ec: EC,
      db: DB,
      au: AU,
      ctx: OC): DbResultT[FullObject[ProductOptionValue]] = {

    val (form, shadow) = payload.formAndShadow.tupled

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      skuCodes ← * <~ payload.skuCodes.map(
                    ProductVariantManager.mustFindByContextAndCode(ctx.id, _))
      _ ← * <~ skuCodes.map(sku ⇒
               DbResultT.fromXor(sku.mustNotBeArchived(ProductOption, variant.formId)))
      ins ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      variantValue ← * <~ ProductOptionValues.create(
                        ProductOptionValue(scope = scope,
                                           contextId = ctx.id,
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

  private def updateProductOptionValue(valueId: Int, payload: ProductOptionValuePayload)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[FullObject[ProductOptionValue]] = {
    val (form, shadow) = payload.formAndShadow.tupled

    for {
      value     ← * <~ mustFindValueByContextAndForm(valueId)
      oldForm   ← * <~ ObjectForms.mustFindById404(value.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(value.shadowId)

      mergedAttrs = oldShadow.attributes.merge(shadow.attributes)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, form.attributes, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateValueHead(value, updated.shadow, commit)

      newProductVariants ← * <~ payload.skuCodes.map(
                              ProductVariantManager.mustFindByContextAndCode(ctx.id, _))
      newProductVariantIds = newProductVariants.map(_.id).toSet

      variantValueLinks ← * <~ ProductValueVariantLinks.filterLeft(value.id).result
      linkedSkuIds = variantValueLinks.map(_.rightId).toSet

      toDelete = variantValueLinks.filter(link ⇒ !newProductVariantIds.contains(link.rightId))
      toCreate = newProductVariantIds.diff(linkedSkuIds)

      _ ← * <~ ProductValueVariantLinks.createAllReturningIds(
             toCreate.map(id ⇒ ProductValueVariantLink(leftId = value.id, rightId = id)))
      _ ← * <~ toDelete.map(
             link ⇒
               ProductValueVariantLinks
                 .deleteById(link.id, DbResultT.unit, id ⇒ NotFoundFailure404(link, link.id)))
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  private def updateOrCreateProductOptionValue(
      variant: ProductOption,
      payload: ProductOptionValuePayload)(implicit ec: EC, db: DB, au: AU, ctx: OC) = {

    payload.id match {
      case Some(id) ⇒ updateProductOptionValue(id, payload)
      case None     ⇒ createProductOptionValue(variant, payload)
    }
  }

  private def updateValueHead(
      value: ProductOptionValue,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[ProductOptionValue] =
    maybeCommit match {
      case Some(commit) ⇒
        ProductOptionValues.update(value, value.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(value)
    }

  def findValuesForOption(productOption: FullObject[ProductOption])(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[ProductOptionValue]]] =
    ProductOptionValueLinks.queryRightByLeft(productOption.model)

  def getProductValueSkuCodes(
      variantValueHeadIds: Seq[Int])(implicit ec: EC, db: DB): DbResultT[Map[Int, Seq[String]]] =
    for {
      links ← * <~ ProductValueVariantLinks
               .findProductVariantsForProductValues(variantValueHeadIds)
               .result
      linksMapping = links.groupBy { case (valueId, _) ⇒ valueId }.mapValues(_.map {
        case (_, skuCode) ⇒ skuCode
      })
    } yield linksMapping

  private def mustFindValueByContextAndForm(
      formId: Int)(implicit ec: EC, db: DB, ctx: OC): DbResultT[ProductOptionValue] =
    for {
      value ← * <~ ProductOptionValues
               .filterByContextAndFormId(ctx.id, formId)
               .mustFindOneOr(ProductValueNotFoundForContext(formId, ctx.id))
    } yield value

  private def mustFindByContextAndForm(formId: Int)(implicit ec: EC,
                                                    ctx: OC): DbResultT[ProductOption] =
    for {
      productOption ← * <~ ProductOptions
                       .filterByContextAndFormId(ctx.id, formId)
                       .mustFindOneOr(ProductOptionNotFoundForContext(formId))
    } yield productOption

  def zipVariantWithValues(variant: FullObject[ProductOption])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[FullProductOption] =
    findValuesForOption(variant).map(values ⇒ variant → values)
}
