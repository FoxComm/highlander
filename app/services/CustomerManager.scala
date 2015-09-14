package services

import scala.concurrent.ExecutionContext

import models.{Customer, Customers, StoreAdmin}
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

}

