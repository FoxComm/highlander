package services

import scala.concurrent.ExecutionContext

import models.product._
import responses.ProductResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import payloads.{CreateProductForm, UpdateProductForm, CreateProductShadow, 
  UpdateProductShadow, CreateProductContext, UpdateProductContext}

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
    shadow       ← * <~ ProductShadows.mustFindById404(id)
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

  private def validateShadow(context: ProductContext, form: Product, shadow: ProductShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    ProductValidator.validate(context, form, shadow) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }

}
