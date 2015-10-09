package services

import scala.concurrent.ExecutionContext

import models.{CreditCards, Regions, Region, Customer, Customers, StoreAdmin}
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
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

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  Result[Seq[Root]] = {
    val query = fetchRegions(Customers)

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
    val query = fetchRegions(Customers.filter(_.id === id))
    db.run(query.result.headOption).flatMap {
      case Some((customer, shipRegion, billRegion)) ⇒
        Result.right(build(customer, shipRegion, billRegion))
      case _ ⇒
        Result.failure(NotFoundFailure(Customer, id))
    }
  }

  /* Returns Query with included shippingRegion and billingRegion for customer */
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

