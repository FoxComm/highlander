package services

import scala.concurrent.ExecutionContext

import models.inventory._
import models.product._
import responses.ProductResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateProductForm, UpdateProductForm, CreateProductShadow, 
  UpdateProductShadow, CreateProductContext, UpdateProductContext,
  CreateFullProductForm, UpdateFullProductForm, CreateFullProductShadow, 
  UpdateFullProductShadow, CreateSkuForm, UpdateFullSkuForm, CreateSkuShadow, 
  UpdateFullSkuShadow}

import ProductFailure._
import utils.aliases._
import cats.data.NonEmptyList


object ProductManager {

  // Detailed info for SKU of each type in given warehouse
  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ Products.mustFindById404(id)
  } yield ProductFormResponse.build(form)).run()

  def createForm(payload: CreateProductForm)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ Products.create(
      Product(attributes = payload.attributes, variants = payload.variants, 
        isActive = false))
  } yield ProductFormResponse.build(form)).run()

  def updateForm(id: Int, payload: UpdateProductForm)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ Products.mustFindById404(id)
    form       ← * <~ Products.update(form, form.copy(attributes = payload.attributes,
      variants = payload.variants, isActive = payload.isActive))
  } yield ProductFormResponse.build(form)).run()

  def getShadow(id: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    shadow  ← * <~ ProductShadows.filter(_.productId === id).one.
      mustFindOr(ProductNotFoundForContext(id, productContext.id))
  } yield ProductShadowResponse.build(shadow, productContext)).run()

  def createShadow(payload: CreateProductShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(payload.productId)
    shadow       ← * <~ ProductShadows.create(ProductShadow(
      productContextId = productContext.id, productId = form.id,
      attributes = payload.attributes))
    _    ← * <~ validateShadow(productContext, form, shadow)
  } yield ProductShadowResponse.build(shadow, productContext)).run()
    
  def updateShadow(id: Int, payload: UpdateProductShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(payload.productId)
    shadow       ← * <~ ProductShadows.filter(_.productId === form.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(form.id, productContext.id)) 
    shadow       ← * <~ ProductShadows.update(shadow, shadow.copy(
      productId = payload.productId,
      attributes = payload.attributes))
    _    ← * <~ validateShadow(productContext, form, shadow)
  } yield ProductShadowResponse.build(shadow, productContext)).run()
    
  def getIlluminatedProduct(id: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedProductResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(id)
    shadow       ← * <~ ProductShadows.filter(_.productId === form.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(form.id, productContext.id)) 
  } yield IlluminatedProductResponse.build(IlluminatedProduct.illuminate(productContext, form, shadow))).run()

  def getContextByName(name: String) 
    (implicit ec: EC, db: DB): Result[ProductContextResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(name).one.
      mustFindOr(ProductContextNotFound(name))
  } yield ProductContextResponse.build(productContext)).run()

  def createContext(payload: CreateProductContext) 
    (implicit ec: EC, db: DB): Result[ProductContextResponse.Root] = (for {
    productContext ← * <~ ProductContexts.create(ProductContext(
      name = payload.name, attributes = payload.attributes))
  } yield ProductContextResponse.build(productContext)).run()

  def updateContextByName(name: String, payload: UpdateProductContext) 
    (implicit ec: EC, db: DB): Result[ProductContextResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(name).one.
      mustFindOr(ProductContextNotFound(name))
    productContext  ← * <~ ProductContexts.update(productContext, 
      productContext.copy(name = payload.name, attributes = payload.attributes))
  } yield ProductContextResponse.build(productContext)).run()

  def getFullForm(id: Int)
    (implicit ec: EC, db: DB): Result[FullProductFormResponse.Root] = (for {
    productForm  ← * <~ Products.mustFindById404(id)
    skuForms  ← * <~ Skus.filter(_.productId === productForm.id).result
  } yield FullProductFormResponse.build(productForm, skuForms)).run()

  def createFullForm(payload: CreateFullProductForm)
    (implicit ec: EC, db: DB): Result[FullProductFormResponse.Root] = (for {
    productForm ← * <~ Products.create(
      Product(attributes = payload.product.attributes, variants = payload.product.variants, 
        isActive = false))
    skuForms  ← * <~ DbResultT.sequence(payload.skus.map { s ⇒ createSkuForm(s)})
  } yield FullProductFormResponse.build(productForm, skuForms)).run()

  private def createSkuForm(payload: CreateSkuForm)(implicit ec: EC, db: DB) = for {
    skuForm ← * <~ Skus.create(
      Sku(code = payload.code, productId = payload.productId, 
        attributes = payload.attributes, isActive = payload.isActive, 
        isHazardous = payload.isHazardous))
  } yield skuForm

  def updateFullForm(id: Int, payload: UpdateFullProductForm)
    (implicit ec: EC, db: DB): Result[FullProductFormResponse.Root] = (for {
    productForm ← * <~ Products.mustFindById404(id)
    productForm ← * <~ Products.update(productForm, productForm.copy(
      attributes = payload.product.attributes, variants = payload.product.variants, 
      isActive = payload.product.isActive))
    skuForms  ← * <~ DbResultT.sequence(payload.skus.map { s ⇒  updateSkuForm(s) })

  } yield FullProductFormResponse.build(productForm, skuForms)).run()

  private def updateSkuForm(payload: UpdateFullSkuForm)(implicit ec: EC, db: DB)  = for {
    skuForm ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    skuForm ← * <~ Skus.update(skuForm, skuForm.copy(attributes = payload.attributes,
      isActive = payload.isActive, isHazardous = payload.isHazardous)) 
  } yield skuForm

  def getFullShadow(id: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[FullProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productShadow  ← * <~ ProductShadows.filter(_.productId === id).one.
      mustFindOr(ProductNotFoundForContext(id, productContext.id))
    skus ← * <~ Skus.filter(_.productId === id).result
    skuIds ← * <~ skus.map(_.id)
    skuShadows ← * <~ SkuShadows.filter(_.skuId.inSet(skuIds)).
      filter(_.productContextId === productContext.id).result
    skuIdsFromShadows ← * <~ skuShadows.map(_.skuId)
    skus ← * <~ Skus.filter(_.id.inSet(skuIdsFromShadows)).result
    skuShadowPair = skus zip skuShadows
  } yield FullProductShadowResponse.build(productContext, productShadow, skuShadowPair)).run()


  def createFullShadow(payload: CreateFullProductShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[FullProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm   ← * <~ Products.mustFindById404(payload.product.productId)
    productShadow ← * <~ ProductShadows.create(ProductShadow(
      productContextId = productContext.id, productId = productForm.id,
      attributes = payload.product.attributes))
    _    ← * <~ validateShadow(productContext, productForm, productShadow)
    skuShadowPair ← * <~ DbResultT.sequence(payload.skus.map { s ⇒  createSkuShadow(s, productContext) } )
  } yield FullProductShadowResponse.build(productContext, productShadow, skuShadowPair)).run()

  private def createSkuShadow(payload: CreateSkuShadow, productContext: ProductContext)
  (implicit ec: EC, db: DB) = for {
    skuForm    ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    skuShadow  ← * <~ SkuShadows.create(
      SkuShadow(skuId = skuForm.id, productContextId = productContext.id,
      attributes = payload.attributes))
    _    ← * <~ SkuManager.validateShadow(skuForm, skuShadow)
  } yield (skuForm, skuShadow)
    
  def updateFullShadow(id: Int, payload: UpdateFullProductShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[FullProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm    ← * <~ Products.mustFindById404(payload.product.productId)
    productShadow  ← * <~ ProductShadows.filter(_.productId === productForm.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(productForm.id, productContext.id)) 
    productShadow       ← * <~ ProductShadows.update(productShadow, productShadow.copy(
      productId = payload.product.productId,
      attributes = payload.product.attributes))
    _    ← * <~ validateShadow(productContext, productForm, productShadow)
    skuShadowPair ← * <~ DbResultT.sequence(payload.skus.map { s ⇒  updateSkuShadow(s, productContext) } )
  } yield FullProductShadowResponse.build(productContext, productShadow, skuShadowPair)).run()

  private def updateSkuShadow(payload: UpdateFullSkuShadow, productContext: ProductContext)
  (implicit ec: EC, db: DB) = for {
    skuForm    ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    skuShadow  ← * <~ SkuShadows.filterBySkuAndContext(skuForm.id, productContext.id).
      one.mustFindOr(SkuNotFoundForContext(payload.code, productContext.name))
    skuShadow  ← * <~ SkuShadows.update(skuShadow, skuShadow.copy(attributes = payload.attributes))
    _    ← * <~ SkuManager.validateShadow(skuForm, skuShadow)
  } yield (skuForm, skuShadow)

  def getIlluminatedFullProduct(id: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm   ← * <~ Products.mustFindById404(id)
    productShadow ← * <~ ProductShadows.filter(_.productId === productForm.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(productForm.id, productContext.id)) 
    skus ← * <~ Skus.filter(_.productId === id).result
    skuIds ← * <~ skus.map(_.id)
    skuShadows ← * <~ SkuShadows.filter(_.skuId.inSet(skuIds)).
      filter(_.productContextId === productContext.id).result
    skuIdsFromShadows ← * <~ skuShadows.map(_.skuId)
    skus ← * <~ Skus.filter(_.id.inSet(skuIdsFromShadows)).result
    skuShadowPair = skus zip skuShadows
  } yield IlluminatedFullProductResponse.build(
    IlluminatedProduct.illuminate(productContext, productForm, productShadow),
    skuShadowPair.map { 
      case (s, ss) ⇒ IlluminatedSku.illuminate(productContext, s, ss)
    })).run()

  private def validateShadow(context: ProductContext, form: Product, shadow: ProductShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    ProductValidator.validate(context, form, shadow) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }
}
