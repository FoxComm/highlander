package services

import scala.concurrent.ExecutionContext

import models._
import models.Customers.scope._
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.UpdateReturning._

object CustomerManager {

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
      case Some(s) ⇒ query.sortBy { case (customer, _, _) ⇒
        s.sortColumn match {
          case "id"                => if(s.asc) customer.id.asc                 else customer.id.desc
          case "isDisabled"        => if(s.asc) customer.isDisabled.asc         else customer.isDisabled.desc
          case "disabledBy"        => if(s.asc) customer.disabledBy.asc         else customer.disabledBy.desc
          case "isBlacklisted"     => if(s.asc) customer.isBlacklisted.asc      else customer.isBlacklisted.desc
          case "blacklistedBy"     => if(s.asc) customer.blacklistedBy.asc      else customer.blacklistedBy.desc
          case "blacklistedReason" => if(s.asc) customer.blacklistedReason.asc  else customer.blacklistedReason.desc
          case "email"             => if(s.asc) customer.email.asc              else customer.email.desc
          case "firstName"         => if(s.asc) customer.firstName.asc          else customer.firstName.desc
          case "lastName"          => if(s.asc) customer.lastName.asc           else customer.lastName.desc
          case "phoneNumber"       => if(s.asc) customer.phoneNumber.asc        else customer.phoneNumber.desc
          case "location"          => if(s.asc) customer.location.asc           else customer.location.desc
          case "modality"          => if(s.asc) customer.modality.asc           else customer.modality.desc
          case "isGuest"           => if(s.asc) customer.isGuest.asc            else customer.isGuest.desc
          case "createdAt"         => if(s.asc) customer.createdAt.asc          else customer.createdAt.desc
          case _                   => customer.id.asc
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

