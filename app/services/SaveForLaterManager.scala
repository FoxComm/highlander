package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{Customer, Customers, SaveForLater, SaveForLaters, Skus}
import responses.ResponseWithFailuresAndMetadata.SavedForLater
import responses.{ResponseWithFailuresAndMetadata, SaveForLaterResponse}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._

object SaveForLaterManager {

  def notFound(id: Int): NotFoundFailure404 = NotFoundFailure404(SaveForLater, id)

  def findAll(customerId: Int)(implicit db: Database, ec: ExecutionContext): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById(customerId)
    response ← * <~ findAllDbio(customer).toXor
  } yield response).run()

  def saveForLater(customerId: Int, skuId: Int)
    (implicit db: Database, ec: ExecutionContext): Result[SavedForLater] = (for {
    customer ← * <~ Customers.mustFindById(customerId)
    sku ← * <~ Skus.mustFindById(skuId)
    _ ← * <~ SaveForLaters.find(customerId = customer.id, skuId = sku.id).one.flatMap(_.fold {
      SaveForLaters.create(SaveForLater(customerId = customer.id, skuId = sku.id))
    } { _ ⇒ DbResult.failure(AlreadySavedForLater(customerId = customer.id, skuId = sku.id)) })
    response ← * <~ findAllDbio(customer).toXor
  } yield response).runTxn()

  def deleteSaveForLater(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Unit] =
    SaveForLaters.deleteById(id, DbResult.unit, notFound).transactionally.run()

  private def findAllDbio(customer: Customer)(implicit ec: ExecutionContext, db: Database): DBIO[SavedForLater] = {
    SaveForLaters.filter(_.customerId === customer.id).result.flatMap { all ⇒
      DBIO.sequence(all.map(_.skuId).map(SaveForLaterResponse.forSkuId)).map { xors ⇒
        val failures = xors.collect { case Xor.Left(f) ⇒ f }.flatMap(_.toList)
        val roots = xors.collect { case Xor.Right(r) ⇒ r }
        ResponseWithFailuresAndMetadata.fromFailureList(roots, failures)
      }
    }
  }
}
