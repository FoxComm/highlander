package services

import cats.data.Xor
import models.customer.{Customers, Customer}
import models.inventory.{Skus, SkuShadows}
import models.product.{Products, ProductShadows}
import models.{SaveForLater, SaveForLaters}
import responses.{SaveForLaterResponse, TheResponse}

import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._

import cats.data.Xor
import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._

object SaveForLaterManager {

  type SavedForLater = TheResponse[Seq[SaveForLaterResponse.Root]]

  def findAll(customerId: Int, productContextId: Int )(implicit db: Database, ec: ExecutionContext): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ findAllDbio(customer, productContextId).toXor
  } yield response).run()

  def saveForLater(customerId: Int, skuCode: String, productContextId: Int)
    (implicit db: Database, ec: ExecutionContext): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    sku ← * <~ Skus.mustFindByCode(skuCode)
    product ← * <~ Products.mustFindById404(sku.productId)
    skuShadow ← * <~ SkuShadows.filter(_.skuId === sku.id).filter(_.productContextId === productContextId).one
                .mustFindOr(SkuNotFoundForContext(sku.id, productContextId))
    productShadow ← * <~ ProductShadows.filter(_.productId === product.id).filter(_.productContextId === productContextId).one
                .mustFindOr(ProductNotFoundForContext(product.id, productContextId))
    _   ← * <~ SaveForLaters.find(customerId = customer.id, skuId = sku.id).one
                 .mustNotFindOr(AlreadySavedForLater(customerId = customer.id, skuId = sku.id))
    _   ← * <~ SaveForLaters.create(SaveForLater(customerId = customer.id, 
                skuId = sku.id, skuShadowId = skuShadow.id, productId = product.id, 
                productShadowId = productShadow.id))
    response ← * <~ findAllDbio(customer, productContextId).toXor
  } yield response).runTxn()

  def deleteSaveForLater(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Unit] =
    SaveForLaters.deleteById(id, DbResult.unit, i ⇒ NotFoundFailure404(SaveForLater, i)).run()

  private def findAllDbio(customer: Customer, productContextId: Int)(implicit ec: ExecutionContext, db: Database): DBIO[SavedForLater] = for {
    sfls ← SaveForLaters.filter(_.customerId === customer.id).result
    xors ← DBIO.sequence(sfls.map(_.skuId).map(skuId ⇒ SaveForLaterResponse.forSkuId(skuId, productContextId).value))

    fails = xors.collect { case Xor.Left(f) ⇒ f }.flatMap(_.toList)
    roots = xors.collect { case Xor.Right(r) ⇒ r }
  } yield TheResponse.build(roots, warnings = Failures(fails: _*))
}
