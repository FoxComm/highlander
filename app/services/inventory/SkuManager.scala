package services.inventory

import services.Result

import models.objects._
import models.inventory._
import responses.SkuResponses._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateSkuForm, CreateSkuShadow, UpdateSkuForm, UpdateSkuShadow}
import utils.aliases._
import cats.data.NonEmptyList
import failures.NotFoundFailure404
import failures.ProductFailures._
import failures.ObjectFailures._

object SkuManager {

  def getIlluminatedFullSkuByContextName(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedFullSkuResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    form    ← * <~ getFormInner(code)
    shadow  ← * <~ getShadowInner(code, contextName)
  } yield IlluminatedFullSkuResponse.build(form, shadow, context)).run()

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

}
