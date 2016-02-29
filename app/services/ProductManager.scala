package services

import scala.concurrent.ExecutionContext

import models.product._
import responses.ProductResponses._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._


object ProductManager {

  // Detailed info for SKU of each type in given warehouse
  def getForm(id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[ProductFormResponse.Root] = (for {
    form       ← * <~ Products.mustFindById404(id)
  } yield ProductFormResponse.build(form)).run()

  def getShadow(id: Int, productContextName: String)
    (implicit ec: ExecutionContext, db: Database): Result[ProductShadowResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    shadow       ← * <~ ProductShadows.mustFindById404(id)
  } yield ProductShadowResponse.build(shadow, productContext)).run()
    
  def getIlluminatedProduct(id: Int, productContextName: String)
    (implicit ec: ExecutionContext, db: Database): Result[IlluminatedProductResponse.Root] = (for {
    productContext ← * <~ ProductContexts.filterByName(productContextName).one.
      mustFindOr(ProductContextNotFound(productContextName))
    form       ← * <~ Products.mustFindById404(id)
    shadow       ← * <~ ProductShadows.filter(_.productId === form.id).
      filter(_.productContextId === productContext.id).one.
        mustFindOr(ProductNotFoundForContext(form.id, productContext.id)) 
  } yield IlluminatedProductResponse.build(IlluminatedProduct.illuminate(productContext, form, shadow))).run()

}
