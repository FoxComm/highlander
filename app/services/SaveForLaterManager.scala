package services

import cats.data.Xor
import failures.{AlreadySavedForLater, Failures, NotFoundFailure404}
import models.customer.{Customer, Customers}
import models.objects.ObjectContext
import models.{SaveForLater, SaveForLaters}
import responses.{SaveForLaterResponse, TheResponse}
import services.inventory.SkuManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object SaveForLaterManager {

  type SavedForLater = TheResponse[Seq[SaveForLaterResponse.Root]]

  def findAll(customerId: Int, contextId: Int)(implicit db: DB, ec: EC): DbResultT[SavedForLater] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      response ← * <~ findAllDbio(customer, contextId).toXor
    } yield response

  def saveForLater(customerId: Int, skuCode: String, context: ObjectContext)(
      implicit db: DB,
      ec: EC): DbResultT[SavedForLater] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      sku      ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, skuCode)
      _ ← * <~ SaveForLaters
           .find(customerId = customer.id, skuId = sku.id)
           .mustNotFindOneOr(AlreadySavedForLater(customerId = customer.id, skuId = sku.id))
      _        ← * <~ SaveForLaters.create(SaveForLater(customerId = customer.id, skuId = sku.id))
      response ← * <~ findAllDbio(customer, context.id).toXor
    } yield response

  // TODO @anna: #longlivedbresultt
  def deleteSaveForLater(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    DbResultT(SaveForLaters.deleteById(id, DbResult.unit, i ⇒ NotFoundFailure404(SaveForLater, i)))

  private def findAllDbio(customer: Customer, contextId: Int)(implicit ec: EC,
                                                              db: DB): DBIO[SavedForLater] =
    for {
      sfls ← SaveForLaters.filter(_.customerId === customer.id).result
      xors ← DBIO.sequence(
                sfls.map(sfl ⇒ SaveForLaterResponse.forSkuId(sfl.skuId, contextId).value))

      fails = xors.collect { case Xor.Left(f)  ⇒ f }.flatMap(_.toList)
      roots = xors.collect { case Xor.Right(r) ⇒ r }
    } yield TheResponse.build(roots, warnings = Failures(fails: _*))
}
