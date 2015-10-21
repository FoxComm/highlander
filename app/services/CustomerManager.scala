package services

import scala.concurrent.ExecutionContext

import models._
import models.{Customers, StoreAdmin, Customer}
import models.Customers.scope._
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.Slick.UpdateReturning._
import payloads.{CreateCustomerPayload, UpdateCustomerPayload}

object CustomerManager {

  def toggleDisabled(customerId: Int, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Customer] = {
    (for {
      updated ← Customers.filter(_.id === customerId).map { t ⇒ (t.isDisabled, t.disabledBy) }.
        updateReturning(Customers.map(identity), (disabled, Some(admin.id))).headOption
    } yield updated).run().flatMap {
      /** We’d need to flatMap now */
      case Some(c) ⇒ Result.good(c)
      case None    ⇒ Result.failures(NotFoundFailure404(Customer, customerId).single)
    }
  }

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  Result[Seq[Root]] = {
    val query = Customers.withDefaultRegions

    val sortedQuery = sortAndPage.sort match {
      case Some(s) ⇒ query.sortBy { case (customer, _, _) ⇒
        s.sortColumn match {
          case "id"                ⇒ if(s.asc) customer.id.asc                 else customer.id.desc
          case "isDisabled"        ⇒ if(s.asc) customer.isDisabled.asc         else customer.isDisabled.desc
          case "disabledBy"        ⇒ if(s.asc) customer.disabledBy.asc         else customer.disabledBy.desc
          case "isBlacklisted"     ⇒ if(s.asc) customer.isBlacklisted.asc      else customer.isBlacklisted.desc
          case "blacklistedBy"     ⇒ if(s.asc) customer.blacklistedBy.asc      else customer.blacklistedBy.desc
          case "blacklistedReason" ⇒ if(s.asc) customer.blacklistedReason.asc  else customer.blacklistedReason.desc
          case "email"             ⇒ if(s.asc) customer.email.asc              else customer.email.desc
          case "name"              ⇒ if(s.asc) customer.name.asc               else customer.name.desc
          case "phoneNumber"       ⇒ if(s.asc) customer.phoneNumber.asc        else customer.phoneNumber.desc
          case "location"          ⇒ if(s.asc) customer.location.asc           else customer.location.desc
          case "modality"          ⇒ if(s.asc) customer.modality.asc           else customer.modality.desc
          case "isGuest"           ⇒ if(s.asc) customer.isGuest.asc            else customer.isGuest.desc
          case "createdAt"         ⇒ if(s.asc) customer.createdAt.asc          else customer.createdAt.desc
          case _                   ⇒ customer.id.asc
        }
      }
      case None    ⇒ query
    }

    Result.fromFuture(sortedQuery.paged.result.run().map { results ⇒
      results.map {
        case (customer, shipRegion, billRegion) ⇒
          build(customer, shipRegion, billRegion)
      }
    })
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    val query = Customers.filter(_.id === id).withDefaultRegions
    query.result.headOption.run().flatMap {
      case Some((customer, shipRegion, billRegion)) ⇒
        Result.right(build(customer, shipRegion, billRegion))
      case _ ⇒
        Result.failure(NotFoundFailure404(Customer, id))
    }
  }

  def create(payload: CreateCustomerPayload)(implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val customer = Customer.buildFromPayload(payload)
    val qq = Customers.save(customer).run()
    qq.flatMap { case(a) ⇒ Result.right(build(a)) }
  }

  def updateFromPayload(customerId: Int, payload: UpdateCustomerPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val finder = Customers.filter(_.id === customerId)
    finder.selectOneForUpdate { customer ⇒
      val updated = finder.map { c ⇒ (c.name, c.email, c.phoneNumber) }
        .updateReturning(Customers.map(identity),
            (payload.name.fold(customer.name)(Some(_)),
              payload.email.getOrElse(customer.email),
              payload.phoneNumber.fold(customer.phoneNumber)(Some(_)))).head

      updated.flatMap(updCustomer ⇒ DbResult.good(build(updCustomer)))
    }
  }
}

