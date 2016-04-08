package services.inventory

import services.Result

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

import models.StoreAdmin
import responses.ObjectResponses.ObjectContextResponse

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

  def createFullSku(admin: StoreAdmin, payload: CreateFullSku, contextName: String)
    (implicit ec: EC, db: DB, ac: AC): Result[FullSkuResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
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
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    sku ← * <~ Skus.filterByContextAndCode(context.id, code).one
      .mustFindOr(SkuNotFoundForContext(code, contextName))
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

  def updateSkuHead(sku: Sku, skuShadow: ObjectShadow, maybeCommit: Option[ObjectCommit]) 
    (implicit ec: EC, db: DB): DbResultT[Sku] = 
      maybeCommit match {
        case Some(commit) ⇒  for { 
          sku ← * <~ Skus.update(sku, sku.copy(shadowId = skuShadow.id, 
            commitId = commit.id))
        } yield sku
        case None ⇒ DbResultT.pure(sku)
      }
}
