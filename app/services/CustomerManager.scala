package services

import models.{Customers ⇒ CustomersTable, StoreAdmin, Customer}
import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

object CustomerManager {
  def toggleDisabled(customer: Customer, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Future[Customer Or Failure] = {
    val actions = for {
      _ ← CustomersTable.filter(_.id === customer.id).
        map { t ⇒ (t.disabled, t.disabledBy) }.
        update((disabled, Some(admin.id)))
      updatedCustomer ← CustomersTable._findById(customer.id).result.headOption
    } yield updatedCustomer

    db.run(actions).map {
      case Some(c) ⇒ Good(c)
      case None ⇒ Bad(NotFoundFailure(s"customer id=${customer.id} not found"))
    }
  }
}
