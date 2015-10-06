package services

import scala.concurrent.ExecutionContext

import models.{CreditCards, Regions, Region, Customer, Customers, StoreAdmin}
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._

object CustomerManager {
  import models.Customers.QuerySeq

  def toggleDisabled(customerId: Int, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Customer] = {
    db.run(for {
      updated ← Customers.filter(_.id === customerId).map { t ⇒ (t.disabled, t.disabledBy) }.
        updateReturning(Customers.map(identity), (disabled, Some(admin.id))).headOption
    } yield updated).flatMap {
      /** We’d need to flatMap now */
      case Some(c) ⇒ Result.good(c)
      case None    ⇒ Result.failures(NotFoundFailure(Customer, customerId).single)
    }
  }

  def findAll(implicit db: Database, ec: ExecutionContext): Result[Seq[Root]] = {
    val query = fetchRegions(Customers)

    Result.fromFuture(db.run(query.result).map { results ⇒
      results.map {
        case (customer, shipRegion, billRegion) ⇒
          build(customer, shipRegion, billRegion)
      }
    })
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val query = fetchRegions(Customers.filter(_.id === id))
    db.run(query.result.headOption).flatMap {
      case Some((customer, shipRegion, billRegion)) ⇒
        Result.right(build(customer, shipRegion, billRegion))
      case _ ⇒
        Result.failure(NotFoundFailure(Customer, id))
    }
  }

  /* Returns Query with included shipRegion and billRegion for customer */
  protected def fetchRegions(query: QuerySeq) = {
    val customerWithShipRegion = for {
      ((c, a), r) ← query.joinLeft(models.Addresses).on {
        case (a, b) ⇒ a.id === b.customerId && b.isDefaultShipping === true
      }.joinLeft(Regions).on(_._2.map(_.regionId) === _.id)
    } yield (c, r)

    val CcWithRegions = CreditCards.join(Regions).on {
      case (c, r) ⇒ c.regionId === r.id && c.isDefault === true
    }

    for {
      ((c, shipRegion), billInfo) ←
        customerWithShipRegion.joinLeft(CcWithRegions).on(_._1.id === _._1.customerId)
    } yield (c, shipRegion, billInfo.map(_._2))
  }

}

