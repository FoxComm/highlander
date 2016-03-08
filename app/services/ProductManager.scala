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
  UpdateFullProductShadow, CreateFullSkuForm, UpdateFullSkuForm, CreateSkuShadow, 
  UpdateFullSkuShadow, CreateFullProduct, UpdateFullProduct}

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
    form       ← * <~ Products.create(Product(attributes = payload.attributes, 
      variants = payload.variants))
  } yield ProductFormResponse.build(form)).runTxn()

  def updateForm(productId: Int, payload: UpdateProductForm)
    (implicit ec: EC, db: DB): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ Products.mustFindById404(productId)
    form       ← * <~ Products.update(form, form.copy(
      attributes = payload.attributes, variants = payload.variants))
  } yield ProductFormResponse.build(form)).runTxn()

  def getShadow(productId: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    shadow  ← * <~ ProductShadows.filter(_.productId === productId).one.
      mustFindOr(ProductNotFoundForContext(productId, productContext.id))
  } yield ProductShadowResponse.build(shadow)).run()

  def createShadow(productId: Int, payload: CreateProductShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(productId)
    shadow       ← * <~ ProductShadows.create(ProductShadow(
      productContextId = productContext.id, productId = form.id,
      attributes = payload.attributes, variants = payload.variants, 
      activeFrom = payload.activeFrom, activeTo = payload.activeTo))
    _    ← * <~ validateShadow(productContext, form, shadow)
  } yield ProductShadowResponse.build(shadow)).runTxn()
    
  def updateShadow(productId: Int, payload: UpdateProductShadow, productContextName: String)
    (implicit ec: EC, db: DB): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(productId)
    shadow       ← * <~ ProductShadows.filter(_.productId === form.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(form.id, productContext.id)) 
    shadow       ← * <~ ProductShadows.update(shadow, shadow.copy(
      productId = productId, attributes = payload.attributes,
      variants = payload.variants, activeFrom = payload.activeFrom, 
      activeTo = payload.activeTo))
    _    ← * <~ validateShadow(productContext, form, shadow)
  } yield ProductShadowResponse.build(shadow)).runTxn()
    
  def getIlluminatedProduct(productId: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedProductResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(productId)
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
  } yield ProductContextResponse.build(productContext)).runTxn()

  def updateContextByName(name: String, payload: UpdateProductContext) 
    (implicit ec: EC, db: DB): Result[ProductContextResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(name).one.
      mustFindOr(ProductContextNotFound(name))
    productContext  ← * <~ ProductContexts.update(productContext, 
      productContext.copy(name = payload.name, attributes = payload.attributes))
  } yield ProductContextResponse.build(productContext)).runTxn()

  def getFullProduct(productId: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm ← * <~ Products.mustFindById404(productId)
    productShadow  ← * <~ ProductShadows.filter(_.productId === productId).one.
      mustFindOr(ProductNotFoundForContext(productId, productContext.id))
    skuTuple ← * <~ getSkus(productId)
    skuShadows ← * <~ SkuShadows.filter(_.skuId.inSet(skuTuple._1)).
      filter(_.productContextId === productContext.id).result
    skuIdsFromShadows ← * <~ skuShadows.map(_.skuId)
    skus ← * <~ Skus.filter(_.id.inSet(skuIdsFromShadows)).result
    skuShadowPair = skus zip skuShadows

  } yield FullProductResponse.build(productForm, productShadow, skuShadowPair)).run()

  def createFullProduct(payload: CreateFullProduct, productContextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm ← * <~ Products.create(Product(
      attributes = payload.form.product.attributes, 
      variants = payload.form.product.variants))
    skuForms  ← * <~ DbResultT.sequence(payload.form.skus.map { s ⇒ createSkuForm(productForm.id, s)})
    productShadow ← * <~ ProductShadows.create(ProductShadow(
      productContextId = productContext.id, productId = productForm.id,
      attributes = payload.shadow.product.attributes, variants = payload.shadow.product.variants, 
      activeFrom = payload.shadow.product.activeFrom, activeTo = payload.shadow.product.activeTo))
    _    ← * <~ validateShadow(productContext, productForm, productShadow)
    skuShadowPair ← * <~ DbResultT.sequence(payload.shadow.skus.map { s ⇒  createSkuShadow(s, productContext) } )

  } yield FullProductResponse.build(productForm, productShadow, skuShadowPair)).runTxn()

  def updateFullProduct(productId: Int, payload: UpdateFullProduct, productContextName: String)
    (implicit ec: EC, db: DB): Result[FullProductResponse.Root] = (for {

    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm ← * <~ Products.mustFindById404(productId)
    productForm ← * <~ Products.update(productForm, productForm.copy(
      attributes = payload.form.product.attributes, variants = payload.form.product.variants))
    productShadow  ← * <~ ProductShadows.filter(_.productId === productForm.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(productForm.id, productContext.id)) 
    productShadow       ← * <~ ProductShadows.update(productShadow, 
      productShadow.copy( productId = productId, 
        variants = payload.shadow.product.variants,
        attributes = payload.shadow.product.attributes, 
        activeFrom = payload.shadow.product.activeFrom, 
        activeTo = payload.shadow.product.activeTo))
    _    ← * <~ validateShadow(productContext, productForm, productShadow)
    skuForms  ← * <~ DbResultT.sequence(payload.form.skus.map { s ⇒  updateSkuForm(s) })
    skuShadowPair ← * <~ DbResultT.sequence(payload.shadow.skus.map { s ⇒  updateSkuShadow(s, productContext) } )

  } yield FullProductResponse.build(productForm, productShadow, skuShadowPair)).runTxn()

  def getIlluminatedFullProduct(productId: Int, productContextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedFullProductResponse.Root] = (for {
      
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    productForm   ← * <~ Products.mustFindById404(productId)
    productShadow ← * <~ ProductShadows.filter(_.productId === productForm.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(productForm.id, productContext.id)) 
    skuTuple ← * <~ getSkus(productId)
    skuShadows ← * <~ SkuShadows.filter(_.skuId.inSet(skuTuple._1)).
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

  private def createSkuForm(productId: Int, payload: CreateFullSkuForm)
  (implicit ec: EC, db: DB) = for {
    skuForm ← * <~ Skus.create(Sku(code = payload.code, 
      attributes = payload.attributes))
    link ← * <~ SkuProductLinks.create(SkuProductLink(
      skuId = skuForm.id, productId = productId))
  } yield skuForm

  private def updateSkuForm(payload: UpdateFullSkuForm)(implicit ec: EC, db: DB)  = for {
    skuForm ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    skuForm ← * <~ Skus.update(skuForm, skuForm.copy(attributes = payload.attributes)) 
  } yield skuForm

  private def createSkuShadow(payload: CreateSkuShadow, productContext: ProductContext)
  (implicit ec: EC, db: DB) = for {
    skuForm    ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    skuShadow  ← * <~ SkuShadows.create(
      SkuShadow(skuId = skuForm.id, productContextId = productContext.id,
      attributes = payload.attributes, activeFrom = payload.activeFrom, 
      activeTo = payload.activeTo))
    _    ← * <~ SkuManager.validateShadow(skuForm, skuShadow)
  } yield (skuForm, skuShadow)
    
  private def updateSkuShadow(payload: UpdateFullSkuShadow, productContext: ProductContext)
  (implicit ec: EC, db: DB) = for {
    skuForm    ← * <~ Skus.findOneByCode(payload.code).mustFindOr(SkuNotFound(payload.code))
    skuShadow  ← * <~ SkuShadows.filterBySkuAndContext(skuForm.id, productContext.id).
      one.mustFindOr(SkuNotFoundForContext(payload.code, productContext.name))
    skuShadow  ← * <~ SkuShadows.update(skuShadow, skuShadow.copy(attributes = payload.attributes,
      activeFrom = payload.activeFrom, activeTo = payload.activeTo))
    _    ← * <~ SkuManager.validateShadow(skuForm, skuShadow)
  } yield (skuForm, skuShadow)

  private def getSkus(productId: Int)(implicit ec: EC, db: DB) : DbResultT[(Seq[Int], Seq[Sku])]= for {
    skuLinks ← * <~ SkuProductLinks.filter(_.productId === productId).result
    skuIds ← * <~ skuLinks.map(_.skuId)
    skus ← * <~ Skus.filter(_.id.inSet(skuIds)).result
  } yield (skuIds, skus)

}
