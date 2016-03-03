package services

import scala.concurrent.ExecutionContext

import models.inventory._
import models.product.ProductContexts
import responses.SkuResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateSkuForm, UpdateSkuForm, CreateSkuShadow, UpdateSkuShadow}

import ProductFailure._
import utils.aliases._
import cats.data.NonEmptyList


object SkuManager {

  // Detailed info for SKU of each type in given warehouse
  def getForm(code: String)
    (implicit ec: EC, db: DB): Result[SkuFormResponse.Root] = (for {
    form       ← * <~ Skus.findOneByCode(code).mustFindOr(SkuNotFound(code))

  } yield SkuFormResponse.build(form)).run()

  def createForm(payload: CreateSkuForm)
    (implicit ec: EC, db: DB): Result[SkuFormResponse.Root] = (for {
    form       ← * <~ Skus.create(
      Sku(code = payload.code, productId = payload.productId, 
        attributes = payload.attributes, isActive = payload.isActive, 
        isHazardous = payload.isHazardous))
  } yield SkuFormResponse.build(form)).run()

  def updateForm(code: String, payload: UpdateSkuForm)
    (implicit ec: EC, db: DB): Result[SkuFormResponse.Root] = (for {
    form       ← * <~ Skus.findOneByCode(code).mustFindOr(SkuNotFound(code))
    form       ← * <~ Skus.update(form, form.copy(attributes = payload.attributes,
      isActive = payload.isActive, isHazardous = payload.isHazardous))
  } yield SkuFormResponse.build(form)).run()

  def getShadow(code: String, productContextName: String)
    (implicit ec: EC, db: DB): Result[SkuShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Skus.findOneByCode(code).mustFindOr(SkuNotFound(code))
    shadow     ← * <~ SkuShadows.filterBySkuAndContext(form.id, productContext.id).
      one.mustFindOr(SkuNotFoundForContext(code, productContext.name))
  } yield SkuShadowResponse.build(form, shadow, productContext)).run()

  def createShadow(payload: CreateSkuShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[SkuShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    shadow       ← * <~ SkuShadows.create(
      SkuShadow(skuId = form.id, productContextId = productContext.id,
      attributes = payload.attributes))
    _    ← * <~ validateShadow(form, shadow)
  } yield SkuShadowResponse.build(form, shadow, productContext)).run()
    
  def updateShadow(code: String, payload: UpdateSkuShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[SkuShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Skus.findOneByCode(code).mustFindOr(SkuNotFound(code))
    shadow     ← * <~ SkuShadows.filterBySkuAndContext(form.id, productContext.id).
      one.mustFindOr(SkuNotFoundForContext(code, productContext.name))
    shadow     ← * <~ SkuShadows.update(shadow, shadow.copy(attributes = payload.attributes))
    _    ← * <~ validateShadow(form, shadow)
  } yield SkuShadowResponse.build(form, shadow, productContext)).run()
    
  def getIlluminatedSku(code: String, productContextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedSkuResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Skus.findOneByCode(code).mustFindOr(SkuNotFound(code))
    shadow     ← * <~ SkuShadows.filterBySkuAndContext(form.id, productContext.id).
      one.mustFindOr(SkuNotFoundForContext(code, productContext.name))
  } yield IlluminatedSkuResponse.build(IlluminatedSku.illuminate(productContext, form, shadow))).run()

  private def validateShadow(form: Sku, shadow: SkuShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    SkuValidator.validate(form, shadow) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }

}
