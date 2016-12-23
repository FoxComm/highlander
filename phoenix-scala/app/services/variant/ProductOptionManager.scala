package services.variant

import failures.ArchiveFailures._
import failures.NotFoundFailure404
import failures.ProductFailures._
import models.inventory.ProductVariant
import models.account._
import models.objects._
import models.product._
import payloads.ProductOptionPayloads._
import responses.ProductOptionResponses.IlluminatedProductOptionResponse
import responses.ProductValueResponses.IlluminatedProductValueResponse
import services.inventory.ProductVariantManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ProductOptionManager {
  type FullProductOption = (FullObject[ProductOption], Seq[FullObject[ProductValue]])

  def create(contextName: String, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedProductOptionResponse.Root] =
    for {
      context           ← * <~ ObjectManager.mustFindByName404(contextName)
      fullProductOption ← * <~ createInner(context, payload)
      (productOption, values) = fullProductOption
      productValueToSkuCodesMapping = payload.values
        .getOrElse(Seq.empty)
        .zip(values)
        .collect {
          case (vvPayload, vvDb) if vvPayload.skuCodes.nonEmpty ⇒
            (vvDb.model.id, vvPayload.skuCodes)
        }
        .toMap
    } yield
      IlluminatedProductOptionResponse.build(
          productOption = IlluminatedProductOption.illuminate(context, productOption),
          productValues = values,
          productValueVariants = productValueToSkuCodesMapping
      )

  def get(contextName: String, variantId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedProductOptionResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ ObjectManager.getFullObject(
                       mustFindByContextAndForm(context.id, variantId))

      values ← * <~ ProductOptionValueLinks.queryRightByLeft(fullVariant.model)
      variantValueSkuCodes ← * <~ ProductOptionManager.getProductValueSkuCodes(
                                values.map(_.model.id))
    } yield
      IlluminatedProductOptionResponse.build(
          productOption = IlluminatedProductOption.illuminate(context, fullVariant),
          productValues = values,
          productValueVariants = variantValueSkuCodes
      )

  def update(contextName: String, variantId: Int, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedProductOptionResponse.Root] =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      fullVariant ← * <~ updateInner(context, variantId, payload)
      (variant, values) = fullVariant
      variantValueSkuCodes ← * <~ ProductOptionManager.getProductValueSkuCodes(
                                values.map(_.model.id))
    } yield
      IlluminatedProductOptionResponse.build(
          productOption = IlluminatedProductOption.illuminate(context, variant),
          productValues = values,
          productValueVariants = variantValueSkuCodes
      )

  def createInner(context: ObjectContext, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullProductOption] = {

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
      values ← * <~ variantValues.map(createProductValueInner(context, variant, _))
    } yield (FullObject(variant, ins.form, ins.shadow), values)
  }

  def updateInner(context: ObjectContext, variantId: Int, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullProductOption] = {

    val newFormAttrs   = ObjectForm.fromPayload(ProductOption.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val valuePayloads  = payload.values.getOrElse(Seq.empty)

    for {
      oldVariant ← * <~ ObjectManager.getFullObject(
                      mustFindByContextAndForm(context.id, variantId))

      mergedAttrs = oldVariant.shadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(oldVariant.form.id,
                                        oldVariant.shadow.id,
                                        newFormAttrs,
                                        mergedAttrs,
                                        force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(oldVariant.model, updated.shadow, commit)

      _ ← * <~ valuePayloads.map(pay ⇒ updateOrCreateProductValue(updatedHead, context, pay))

      values ← * <~ ProductOptionValueLinks.queryRightByLeft(oldVariant.model)
    } yield (FullObject(updatedHead, updated.form, updated.shadow), values)
  }

  def updateOrCreate(context: ObjectContext, payload: ProductOptionPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullProductOption] = {

    payload.id match {
      case Some(id) ⇒ updateInner(context, id, payload)
      case None     ⇒ createInner(context, payload)
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

  def createProductValue(contextName: String, variantId: Int, payload: ProductValuePayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedProductValueResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      variant ← * <~ ProductOptions
                 .filterByContextAndFormId(context.id, variantId)
                 .mustFindOneOr(ProductOptionNotFoundForContext(variantId, context.id))
      value ← * <~ createProductValueInner(context, variant, payload)
    } yield IlluminatedProductValueResponse.build(value, payload.skuCodes)

  private def createProductValueInner(context: ObjectContext,
                                      variant: ProductOption,
                                      payload: ProductValuePayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullObject[ProductValue]] = {

    val (form, shadow) = payload.formAndShadow.tupled

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      skuCodes ← * <~ payload.skuCodes.map(
                    ProductVariantManager.mustFindByContextAndCode(context.id, _))
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

  private def updateProductValueInner(valueId: Int, contextId: Int, payload: ProductValuePayload)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[ProductValue]] = {
    val (form, shadow) = payload.formAndShadow.tupled

    for {
      value     ← * <~ mustFindProductValueByContextAndForm(contextId, valueId)
      oldForm   ← * <~ ObjectForms.mustFindById404(value.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(value.shadowId)

      mergedAttrs = oldShadow.attributes.merge(shadow.attributes)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, form.attributes, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateValueHead(value, updated.shadow, commit)

      newSkus ← * <~ payload.skuCodes.map(
                   ProductVariantManager.mustFindByContextAndCode(contextId, _))
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

  private def updateOrCreateProductValue(
      variant: ProductOption,
      context: ObjectContext,
      payload: ProductValuePayload)(implicit ec: EC, db: DB, au: AU) = {

    payload.id match {
      case Some(id) ⇒ updateProductValueInner(id, context.id, payload)
      case None     ⇒ createProductValueInner(context, variant, payload)
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

  def findByProduct(product: Product)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[(FullObject[ProductOption], Seq[FullObject[ProductValue]])]] =
    for {
      variants ← * <~ ProductOptionLinks.queryRightByLeft(product)
      values   ← * <~ variants.map(findValuesForOption)
    } yield variants.zip(values)

  def findValuesForOption(productOption: FullObject[ProductOption])(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[ProductValue]]] =
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

  private def mustFindProductValueByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[ProductValue] =
    for {

      value ← * <~ ProductValues
               .filterByContextAndFormId(contextId, formId)
               .mustFindOneOr(ProductValueNotFoundForContext(formId, contextId))
    } yield value

  def mustFindProductValueByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[ProductValue]] =
    for {

      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      value ← * <~ ProductValues
               .filterByContextAndFormId(contextId, form.id)
               .mustFindOneOr(ProductValueNotFoundForContext(form.id, contextId))
    } yield FullObject(value, form, shadow)

  private def mustFindByContextAndForm(contextId: Int, formId: Int)(
      implicit ec: EC): DbResultT[ProductOption] =
    for {
      productOption ← * <~ ProductOptions
                       .filterByContextAndFormId(contextId, formId)
                       .mustFindOneOr(ProductOptionNotFoundForContext(formId, contextId))
    } yield productOption

  def mustFindByContextAndShadow(contextId: Int, shadowId: Int)(
      implicit ec: EC): DbResultT[FullObject[ProductOption]] =
    for {
      shadow ← * <~ ObjectManager.mustFindShadowById404(shadowId)
      form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
      productOption ← * <~ ProductOptions
                       .filterByContextAndFormId(contextId, form.id)
                       .mustFindOneOr(ProductOptionNotFoundForContext(form.id, contextId))
    } yield FullObject(productOption, form, shadow)

  def mustFindFullWithValuesById(
      id: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[FullProductOption] =
    for {
      fullProductOption ← * <~ ObjectManager.getFullObject(ProductOptions.mustFindById404(id))
      values            ← * <~ findValuesForOption(fullProductOption)
    } yield (fullProductOption, values)

  def zipVariantWithValues(variant: FullObject[ProductOption])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[FullProductOption] =
    findValuesForOption(variant).map(values ⇒ variant → values)
}
