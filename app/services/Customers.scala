package services

import models.{Customers ⇒ CustomersTable, StoreAdmin, Customer}
import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

object Customers {
  def toggleDisabled(customer: Customer, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Future[Customer Or Failure] = {
    if (disabled == customer.disabled) {
      val state = if (customer.disabled) "disabled" else "enabled"
      Future.successful(Bad(GeneralFailure(s"customer is already $state")))
    } else {
      for {
        bah ← CustomersTable.filter(_.id === customer.id).
          map { t ⇒ (t.disabled, t.disabledBy) }.
          update((disabled, Some(admin.id)))
      } yield bah
    }
    Future.successful(Good(customer))
  }
}
