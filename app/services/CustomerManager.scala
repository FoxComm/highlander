package services

import scala.concurrent.ExecutionContext

import models.{CreditCards, Regions, Customer, Customers, StoreAdmin}
import responses.CustomerResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._

object CustomerManager {
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
    val customersWithShipRegion = Customers
      .joinLeft(models.Addresses).on { case(a,b) => a.id === b.customerId  && b.isDefaultShipping === true}
      .joinLeft(Regions).on(_._2.map(_.regionId) === _.id)

    val creditCardsWithRegion = CreditCards.join(Regions).on(_.regionId === _.id)

    val query = customersWithShipRegion.joinLeft(creditCardsWithRegion).on(_._1._1.id === _._1.customerId)

    Result.fromFuture(db.run(query.result).map { results ⇒
      results.map {
        case (((customer, _), shipRegion), billRegion) ⇒
          build(customer, shipRegion, billRegion.map(_._2))
      }
    })
  }

}

