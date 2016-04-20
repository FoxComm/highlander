package services

import models.customer.{Customers, Customer}
import models.inventory.Skus
import models.objects.ObjectContext
import models.{SaveForLater, SaveForLaters}
import responses.{SaveForLaterResponse, TheResponse}
import utils.db._
import utils.db.DbResultT._
import utils.aliases._
import cats.data.Xor

import failures.{AlreadySavedForLater, Failures, NotFoundFailure404}
import failures.ProductFailures.SkuNotFoundForContext
import slick.driver.PostgresDriver.api._

object SaveForLaterManager {

  type SavedForLater = TheResponse[Seq[SaveForLaterResponse.Root]]

  def findAll(customerId: Int, contextId: Int )(implicit db: DB, ec: EC): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    response ← * <~ findAllDbio(customer, contextId).toXor
  } yield response).run()

  def saveForLater(customerId: Int, skuCode: String, context: ObjectContext)
    (implicit db: DB, ec: EC): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById404(customerId)
    sku ← * <~ Skus.filterByContextAndCode(context.id, skuCode).one
      .mustFindOr(SkuNotFoundForContext(skuCode, context.name))
    _   ← * <~ SaveForLaters.find(customerId = customer.id, skuId = sku.id).one
                 .mustNotFindOr(AlreadySavedForLater(customerId = customer.id, skuId = sku.id))
    _   ← * <~ SaveForLaters.create(SaveForLater(customerId = customer.id, 
                skuId = sku.id))
    response ← * <~ findAllDbio(customer, context.id).toXor
  } yield response).runTxn()

  def deleteSaveForLater(id: Int)(implicit ec: EC, db: DB): Result[Unit] =
    SaveForLaters.deleteById(id, DbResult.unit, i ⇒ NotFoundFailure404(SaveForLater, i)).run()

  private def findAllDbio(customer: Customer, contextId: Int)(implicit ec: EC, db: DB): DBIO[SavedForLater] = for {
    sfls ← SaveForLaters.filter(_.customerId === customer.id).result
    xors ← DBIO.sequence(sfls.map(sfl ⇒ SaveForLaterResponse.forSkuId(sfl.skuId, contextId).value))

    fails = xors.collect { case Xor.Left(f) ⇒ f }.flatMap(_.toList)
    roots = xors.collect { case Xor.Right(r) ⇒ r }
  } yield TheResponse.build(roots, warnings = Failures(fails: _*))
}
