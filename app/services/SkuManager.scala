package services

import models.objects._
import models.inventory._
import responses.SkuResponses._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateFullSku, UpdateFullSku}
import utils.aliases._
import cats.data.NonEmptyList
import failures.NotFoundFailure404
import failures.ProductFailures._
import failures.ObjectFailures._

import cats.implicits._
import org.json4s.JsonAST.JValue
import java.time.Instant

object SkuManager {

  def getFullSkuByContextName(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[FullSkuResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
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
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    sku     ← * <~ Skus.filterByContextAndCode(context.id, code).one.
      mustFindOr(SkuNotFound(code))
    shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
  } yield SkuShadowResponse.build(sku, shadow)

  def getIlluminatedSku(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    sku     ← * <~ Skus.filterByContextAndCode(context.id, code).one.
      mustFindOr(SkuNotFound(code))
    form    ← * <~ ObjectForms.mustFindById404(sku.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
  } yield IlluminatedSkuResponse.build(IlluminatedSku.illuminate(
    context, sku, form, shadow))).run()

  def validateShadow(form: ObjectForm, shadow: ObjectShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    SkuValidator.validate(form, shadow) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }

  def createFullSku(payload: CreateFullSku, contextName: String)
    (implicit ec: EC, db: DB): Result[FullSkuResponse.Root] = (for {
    context   ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    skuForm   ← * <~ ObjectForms.create(ObjectForm(kind = Sku.kind, attributes = payload.form.attributes))
    skuShadow ← * <~ ObjectShadows.create(ObjectShadow(formId = skuForm.id, attributes = payload.shadow.attributes))
    skuCommit ← * <~ ObjectCommits.create(ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
    _         ← * <~ validateShadow(form = skuForm, shadow = skuShadow)
    sku       ← * <~ Skus.create(Sku(code = payload.form.code, contextId = context.id, formId = skuForm.id,
      shadowId = skuShadow.id, commitId = skuCommit.id))
    skuFormResponse   = SkuFormResponse.build(sku, skuForm)
    skuShadowResponse = SkuShadowResponse.build(sku, skuShadow)
  } yield FullSkuResponse.build(
    form = skuFormResponse,
    shadow = skuShadowResponse,
    context = context)).runTxn()

  def updateFullSku(code: String, payload: UpdateFullSku, contextName: String)
    (implicit ec: EC, db: DB): Result[FullSkuResponse.Root] = (for {
    context           ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    sku               ← * <~ Skus.filterByContextAndCode(context.id, code).one
      .mustFindOr(SkuNotFoundForContext(code, contextName))
    updatedFormShadow ← * <~ ObjectManager.updateObjectFormAndShadow(sku.formId,
      sku.shadowId, payload.form.attributes, payload.shadow.attributes)
    (skuForm, skuShadow, skuChanged) = updatedFormShadow
    sku               ← * <~ commitSku(sku, skuForm, skuShadow, skuChanged)
    skuFormResponse   = SkuFormResponse.build(sku, skuForm)
    skuShadowResponse = SkuShadowResponse.build(sku, skuShadow)
  } yield FullSkuResponse.build(
    form = skuFormResponse,
    shadow = skuShadowResponse,
    context = context)).runTxn()

  def commitSku(sku: Sku, skuForm: ObjectForm,
    skuShadow: ObjectShadow, shouldCommit: Boolean)
    (implicit ec: EC, db: DB): DbResultT[Sku] =
    if(shouldCommit) for {
      newCommit ← * <~ ObjectManager.createCommit(sku.commitId, skuForm.id, skuShadow.id)
      product   ← * <~ Skus.update(sku, sku.copy(
        shadowId = skuShadow.id, commitId = newCommit.id))
    } yield product
    else DbResultT.pure(sku)
}
