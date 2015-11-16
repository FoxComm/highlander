package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{Customer, Customers, SaveForLater, SaveForLaters, Sku, Skus}
import responses.ResponseWithFailuresAndMetadata.SavedForLater
import responses.{ResponseWithFailuresAndMetadata, SaveForLaterResponse}
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._

object SaveForLaterManager {

  def notFound(id: Int): NotFoundFailure404 = NotFoundFailure404(SaveForLater, id)

  def findAll(customerId: Int)(implicit db: Database, ec: ExecutionContext): Result[SavedForLater] = {
    Customers.findById(customerId).extract.selectOne { customer ⇒
      DbResult.fromDbio(findAllDbio(customer))
    }
  }

  def saveForLater(customerId: Int, skuId: Int)
    (implicit db: Database, ec: ExecutionContext): Result[SavedForLater] = {

    val customerQ = Customers.findOneById(customerId)
    val skuQ = Skus.findOneById(skuId)

    customerQ.zip(skuQ).flatMap {
      case (Some(customer), Some(sku)) ⇒
        SaveForLaters.filter(_.customerId === customerId).filter(_.skuId === skuId).one.flatMap {
          case Some(_) ⇒
            DbResult.failure(AlreadySavedForLater(customerId, skuId))
          case None ⇒
            val insert = SaveForLaters.saveNew(SaveForLater(customerId = customerId, skuId = skuId))
            DbResult.fromDbio(insert >> findAllDbio(customer))
        }
      case (None, _) ⇒
        DbResult.failure(NotFoundFailure404(Customer, customerId))
      case (_, None) ⇒
        DbResult.failure(NotFoundFailure404(Sku, skuId))

    }.transactionally.run()
  }

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
