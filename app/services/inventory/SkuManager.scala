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
import payloads.SkuPayloads._
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses._
import services.{LogActivity, Result}
import services.objects.ObjectManager
import services.variant.VariantManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object SkuManager {

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  // NEW SIMPLE ENDPOINTS

  def createSku(contextName: String, payload: CreateSkuPayload)(
      implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] = {
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ createSkuInner(context, payload)
    } yield IlluminatedSkuResponse.build(IlluminatedSku.illuminate(context, sku))).runTxn()
  }

  def getSku(contextName: String, code: String)(
      implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      form    ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield
      IlluminatedSkuResponse.build(
          IlluminatedSku.illuminate(context, FullObject(sku, form, shadow)))).run()

  def updateSku(contextName: String, code: String, payload: UpdateSkuPayload)(
      implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] =
    (for {
      context    ← * <~ ObjectManager.mustFindByName404(contextName)
      sku        ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      updatedSku ← * <~ updateSkuInner(sku, payload.attributes)
    } yield IlluminatedSkuResponse.build(IlluminatedSku.illuminate(context, updatedSku))).runTxn()

  def createSkuInner(context: ObjectContext, payload: CreateSkuPayload)(
      implicit ec: EC, db: DB): DbResultT[FullObject[Sku]] = {

    val form   = ObjectForm.fromPayload(Sku.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      sku ← * <~ Skus.create(
               Sku(contextId = context.id,
                   code = payload.code,
                   formId = ins.form.id,
                   shadowId = ins.shadow.id,
                   commitId = ins.commit.id))
    } yield FullObject(sku, ins.form, ins.shadow)
  }

  def updateSkuInner(sku: Sku, attributes: Map[String, Json])(
      implicit ec: EC, db: DB): DbResultT[FullObject[Sku]] = {

    val newFormAttrs   = ObjectForm.fromPayload(Sku.kind, attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(attributes).attributes

    for {
      oldForm   ← * <~ ObjectForms.mustFindById404(sku.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils.update(
                   oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(sku, updated.shadow, commit)

      _ ← * <~ ObjectUtils.updateAssociatedLefts(
             Products, sku.contextId, oldShadow.id, updatedHead.shadowId, ObjectLink.ProductSku)

      albumLinks ← * <~ ObjectLinks.findByLeftAndType(oldShadow.id, ObjectLink.SkuAlbum).result
      _          ← * <~ ObjectUtils.updateAssociatedRights(Skus, albumLinks, updatedHead.shadowId)
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  private def updateHead(sku: Sku, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[Sku] = maybeCommit match {
    case Some(commit) ⇒
      Skus.update(sku, sku.copy(shadowId = shadow.id, commitId = commit.id))
    case None ⇒
      DbResult.good(sku)
  }

  //
  /////////////////////////////////////////////////////////////////////////////////////////////////////

  def getFullSkuByContextName(code: String, contextName: String)(
      implicit ec: EC, db: DB): Result[FullSkuResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      form    ← * <~ getFormInner(code)
      shadow  ← * <~ getShadowInner(code, contextName)
    } yield FullSkuResponse.build(form, shadow, context)).run()

  // Detailed info for SKU of each type in given warehouse
  def getForm(code: String)(implicit ec: EC, db: DB): Result[SkuFormResponse.Root] =
    getFormInner(code).run()

  def getFormInner(code: String)(implicit ec: EC, db: DB): DbResultT[SkuFormResponse.Root] =
    for {
      sku  ← * <~ Skus.filterByCode(code).mustFindOneOr(SkuNotFound(code))
      form ← * <~ ObjectForms.mustFindById404(sku.formId)
    } yield SkuFormResponse.build(sku, form)

  def getShadow(code: String, contextName: String)(
      implicit ec: EC, db: DB): Result[SkuShadowResponse.Root] =
    getShadowInner(code, contextName).run()

  def getShadowInner(code: String, contextName: String)(
      implicit ec: EC, db: DB): DbResultT[SkuShadowResponse.Root] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ mustFindSkuByContextAndCode(context.id, code)
      shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield SkuShadowResponse.build(sku, shadow)

  def getIlluminatedSku(code: String, contextName: String)(
      implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ mustFindSkuByContextAndCode(context.id, code)
      form    ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield
      IlluminatedSkuResponse.build(
          IlluminatedSku.illuminate(context, FullObject(sku, form, shadow)))).run()

  def validateShadow(form: ObjectForm, shadow: ObjectShadow)(implicit ec: EC): DbResultT[Unit] =
    SkuValidator.validate(form, shadow) match {
      case Nil          ⇒ DbResultT.pure(Unit)
      case head :: tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }

  def createFullSku(admin: StoreAdmin, payload: CreateFullSku, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[FullSkuResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      skuForm ← * <~ ObjectForms.create(
                   ObjectForm(kind = Sku.kind, attributes = payload.form.attributes))
      skuShadow ← * <~ ObjectShadows.create(
                     ObjectShadow(formId = skuForm.id, attributes = payload.shadow.attributes))
      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
      _ ← * <~ validateShadow(form = skuForm, shadow = skuShadow)
      sku ← * <~ Skus.create(
               Sku(code = payload.form.code,
                   contextId = context.id,
                   formId = skuForm.id,
                   shadowId = skuShadow.id,
                   commitId = skuCommit.id))
      skuFormResponse   = SkuFormResponse.build(sku, skuForm)
      skuShadowResponse = SkuShadowResponse.build(sku, skuShadow)
      skuResponse = FullSkuResponse.build(
          form = skuFormResponse, shadow = skuShadowResponse, context = context)
      contextResponse = ObjectContextResponse.build(context)
      _ ← * <~ LogActivity.fullSkuCreated(Some(admin), skuResponse, contextResponse)
    } yield skuResponse).runTxn()

  def updateFullSku(admin: StoreAdmin, code: String, payload: UpdateFullSku, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[FullSkuResponse.Root] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ mustFindSkuByContextAndCode(context.id, code)
      updated ← * <~ ObjectUtils.update(
                   sku.formId, sku.shadowId, payload.form.attributes, payload.shadow.attributes)
      commit ← * <~ ObjectUtils.commit(updated.form, updated.shadow, updated.updated)
      sku    ← * <~ updateSkuHead(sku, updated.shadow, commit)
      skuFormResponse   = SkuFormResponse.build(sku, updated.form)
      skuShadowResponse = SkuShadowResponse.build(sku, updated.shadow)
      skuResponse       = FullSkuResponse.build(skuFormResponse, skuShadowResponse, context)
      contextResponse   = ObjectContextResponse.build(context)
      _ ← * <~ LogActivity.fullSkuUpdated(Some(admin), skuResponse, contextResponse)
    } yield skuResponse).runTxn()

  def updateSkuHead(sku: Sku, skuShadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResultT[Sku] =
    maybeCommit match {
      case Some(commit) ⇒
        for {
          sku ← * <~ Skus.update(sku, sku.copy(shadowId = skuShadow.id, commitId = commit.id))
        } yield sku
      case None ⇒ DbResultT.pure(sku)
    }

  private def varValueIfInProduct(valueShadowId: Int, contextId: Int, allowedVariantIds: Seq[Int])(
      implicit ec: EC): DbResultT[Option[VariantValueMapping]] =
    for {
      value ← * <~ ObjectLinks
               .findByRightAndType(valueShadowId, ObjectLink.VariantValue)
               .filter(_.leftId.inSet(allowedVariantIds))
               .one
               .flatMap {
                 case Some(variantLink) ⇒
                   val fullValue = VariantManager.mustFindVariantValueByContextAndShadow(
                       contextId, valueShadowId)
                   fullValue.map(value ⇒ VariantValueMapping(variantLink.leftId, value).some).value
                 case None ⇒
                   DbResultT.pure(None).value
               }
    } yield value

  def findVariantValuesForSkuInProduct(sku: Sku, variants: Seq[FullObject[Variant]])(
      implicit ec: EC): DbResultT[Seq[VariantValueMapping]] =
    for {
      // Since not all of a variant's values must be used in a product,
      // and SKU + Variant may exist in multiple products: get all of a
      // SKUs VariantValues if the Variant is in the Product.
      links ← * <~ ObjectLinks.findByLeftAndType(sku.shadowId, ObjectLink.SkuVariantValue).result
      varIds = variants.map(_.shadow.id)
      toOpt ← * <~ links.map(l ⇒ varValueIfInProduct(l.rightId, sku.contextId, varIds))
      avail ← * <~ toOpt.foldLeft(Seq.empty[VariantValueMapping]) { (acc, potentialValue) ⇒
               potentialValue.foldLeft(acc)(_ :+ _)
             }
    } yield avail

  def mustFindSkuByContextAndCode(
      contextId: Int, code: String)(implicit ec: EC, db: DB): DbResultT[Sku] =
    for {
      sku ← * <~ Skus
             .filterByContextAndCode(contextId, code)
             .mustFindOneOr(SkuNotFoundForContext(code, contextId))
    } yield sku
}
