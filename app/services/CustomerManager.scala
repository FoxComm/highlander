package services

import scala.concurrent.ExecutionContext

import models.{Customer, Customers, StoreAdmin}
import models.Customers.scope._
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._

object CustomerManager {
  import models.Customers.QuerySeq

  def toggleDisabled(customerId: Int, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Customer] = {
    db.run(for {
      updated ← Customers.filter(_.id === customerId).map { t ⇒ (t.isDisabled, t.disabledBy) }.
        updateReturning(Customers.map(identity), (disabled, Some(admin.id))).headOption
    } yield updated).flatMap {
      /** We’d need to flatMap now */
      case Some(c) ⇒ Result.good(c)
      case None    ⇒ Result.failures(NotFoundFailure(Customer, customerId).single)
    }
  }

  def findAll(implicit db: Database, ec: ExecutionContext): Result[Seq[Root]] = {
    Result.fromFuture(db.run(Customers.withDefaultRegions.result).map { results ⇒
      results.map {
        case (customer, shipRegion, billRegion) ⇒
          build(customer, shipRegion, billRegion)
      }
    })
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val query = Customers.filter(_.id === id).withDefaultRegions
    db.run(query.result.headOption).flatMap {
      case Some((customer, shipRegion, billRegion)) ⇒
        Result.right(build(customer, shipRegion, billRegion))
      case _ ⇒
        Result.failure(NotFoundFailure(Customer, id))
    }
  }

}

