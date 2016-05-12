package services.inventory

import cats.data.NonEmptyList
import cats.implicits._
import cats.data.Xor.right
import failures.ObjectFailures._
import failures.ProductFailures._
import models.StoreAdmin
import models.inventory._
import models.objects._
import models.product._
import payloads.{CreateFullSku, UpdateFullSku}
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses._
import services.{LogActivity, Result}
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object SkuManager {

  def getFullSkuByContextName(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[FullSkuResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    form    ← * <~ getFormInner(code)
    shadow  ← * <~ getShadowInner(code, contextName)
  } yield FullSkuResponse.build(form, shadow, context)).run()

  // Detailed info for SKU of each type in given warehouse
  def getForm(code: String)
    (implicit ec: EC, db: DB): Result[SkuFormResponse.Root] = getFormInner(code).run()

  def getFormInner(code: String)
    (implicit ec: EC, db: DB): DbResultT[SkuFormResponse.Root] = for {
    sku  ← * <~ Skus.filterByCode(code).one.mustFindOr(SkuNotFound(code))
    form ← * <~ ObjectForms.mustFindById404(sku.formId)
  } yield SkuFormResponse.build(sku, form)

  def getShadow(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[SkuShadowResponse.Root] =
    getShadowInner(code, contextName).run()

  def getShadowInner(code: String, contextName: String)
    (implicit ec: EC, db: DB): DbResultT[SkuShadowResponse.Root] = for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    sku     ← * <~ mustFindSkuByContextAndCode(context.id, code)
    shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
  } yield SkuShadowResponse.build(sku, shadow)

  def getIlluminatedSku(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    sku     ← * <~ mustFindSkuByContextAndCode(context.id, code)
    form    ← * <~ ObjectForms.mustFindById404(sku.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
  } yield IlluminatedSkuResponse.build(IlluminatedSku.illuminate(
    context, sku, form, shadow))).run()

  def validateShadow(form: ObjectForm, shadow: ObjectShadow)(implicit ec: EC) : DbResultT[Unit] =
    SkuValidator.validate(form, shadow) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }

  def createFullSku(admin: StoreAdmin, payload: CreateFullSku, contextName: String)
    (implicit ec: EC, db: DB, ac: AC): Result[FullSkuResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    skuForm ← * <~ ObjectForms.create(ObjectForm(kind = Sku.kind, attributes = payload.form.attributes))
    skuShadow ← * <~ ObjectShadows.create(ObjectShadow(formId = skuForm.id, attributes = payload.shadow.attributes))
    skuCommit ← * <~ ObjectCommits.create(ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
    _ ← * <~ validateShadow(form = skuForm, shadow = skuShadow)
    sku ← * <~ Skus.create(Sku(code = payload.form.code, contextId = context.id, formId = skuForm.id,
      shadowId = skuShadow.id, commitId = skuCommit.id))
    skuFormResponse = SkuFormResponse.build(sku, skuForm)
    skuShadowResponse = SkuShadowResponse.build(sku, skuShadow)
    skuResponse = FullSkuResponse.build(form = skuFormResponse, shadow = skuShadowResponse, context = context)
    contextResponse = ObjectContextResponse.build(context)
    _ ← * <~ LogActivity.fullSkuCreated(Some(admin), skuResponse, contextResponse)
  } yield skuResponse).runTxn()

  def updateFullSku(admin: StoreAdmin, code: String, payload: UpdateFullSku, contextName: String)
    (implicit ec: EC, db: DB, ac: AC): Result[FullSkuResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    sku     ← * <~ mustFindSkuByContextAndCode(context.id, code)
    updated ← * <~ ObjectUtils.update(sku.formId,
      sku.shadowId, payload.form.attributes, payload.shadow.attributes)
    commit ← * <~ ObjectUtils.commit(updated.form, updated.shadow, updated.updated)
    sku               ← * <~ updateSkuHead(sku, updated.shadow, commit)
    skuFormResponse   = SkuFormResponse.build(sku, updated.form)
    skuShadowResponse = SkuShadowResponse.build(sku, updated.shadow)
    skuResponse = FullSkuResponse.build(skuFormResponse, skuShadowResponse, context)
    contextResponse = ObjectContextResponse.build(context)
    _ ← * <~ LogActivity.fullSkuUpdated(Some(admin), skuResponse, contextResponse)
  } yield skuResponse).runTxn()

  def updateSkuHead(sku: Sku, skuShadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Sku] =
      maybeCommit match {
        case Some(commit) ⇒  for {
          sku ← * <~ Skus.update(sku, sku.copy(shadowId = skuShadow.id,
            commitId = commit.id))
        } yield sku
        case None ⇒ DbResultT.pure(sku)
      }

  private def varValueIfInProduct(valueShadowId: Int, contextId: Int, allowedVariantIds: Seq[Int])
    (implicit ec: EC): DbResultT[Option[VariantValueMapping]] = for {
    value ← * <~ ObjectLinks.findByRightAndType(valueShadowId, ObjectLink.VariantValue).
      filter(_.leftId.inSet(allowedVariantIds)).one.flatMap {
      case Some(variantLink) ⇒
        (for {
          shadow ← * <~ ObjectManager.mustFindShadowById404(valueShadowId)
          form   ← * <~ ObjectManager.mustFindFormById404(shadow.formId)
          value  ← * <~ VariantValues.filterByContextAndFormId(contextId, form.id).one.
            mustFindOr(VariantValueNotFoundForContext(form.id, contextId))
        } yield VariantValueMapping(variantLink.leftId, FullObject(value, form, shadow)).some).value
      case None ⇒
        DbResultT.pure(None).value
    }
  } yield value

  def findVariantValuesForSkuInProduct(sku: Sku, variants: Seq[FullObject[Variant]])
    (implicit ec: EC): DbResultT[Seq[VariantValueMapping]] = for {
    // Since not all of a variant's values must be used in a product,
    // and SKU + Variant may exist in multiple products: get all of a
    // SKUs VariantValues if the Variant is in the Product.
    links  ← * <~ ObjectLinks.findByLeftAndType(sku.shadowId, ObjectLink.SkuVariantValue).result
    varIds ← * <~ variants.map(_.shadow.id)
    toOpt  ← * <~ DbResultT.sequence(links.map(l ⇒ varValueIfInProduct(l.rightId, sku.contextId, varIds)))
    avail  ← * <~ toOpt.foldLeft(Seq.empty[VariantValueMapping]) { (acc, potentialValue) ⇒
      potentialValue match {
        case Some(value) ⇒ acc :+ value
        case None ⇒ acc
      }
    }
  } yield avail

  def mustFindSkuByContextAndCode(contextId: Int, code: String)
    (implicit ec: EC, db: DB): DbResultT[Sku] = for {
    sku ← * <~ Skus.filterByContextAndCode(contextId, code).one
      .mustFindOr(SkuNotFoundForContext(code, contextId))
  } yield sku
}
