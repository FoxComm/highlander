package services

import scala.concurrent.ExecutionContext

import models.{Customer, Customers, StoreAdmin}
import models.Customers.scope._
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
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

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  Result[Seq[Root]] = {
    val query = Customers.withDefaultRegions

    val sortedQuery = sortAndPage.sort match {
      case Some(s) ⇒ query.sortBy { case (t, _, _) ⇒
        s.sortColumn match {
          case "id"        => if(s.asc) t.id.asc        else t.id.desc
          case "email"     => if(s.asc) t.email.asc     else t.email.desc
          case "firstName" => if(s.asc) t.firstName.asc else t.firstName.desc
          case "lastName"  => if(s.asc) t.lastName.asc  else t.lastName.desc
          case "location"  => if(s.asc) t.location.asc  else t.location.desc
          case _           => t.id.asc
        }
      }
      case None    ⇒ query
    }

    Result.fromFuture(db.run(sortedQuery.paged.result).map { results ⇒
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

