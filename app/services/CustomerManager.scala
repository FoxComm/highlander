package services

import scala.concurrent.{ExecutionContext, Future}

import models.{CreditCard, CreditCards, Customer, Customers ⇒ CustomersTable, StoreAdmin}
import org.scalactic._
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

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

  def toggleDefaultCreditCard(cc: CreditCard, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Future[CreditCard Or Failure] = {

    CreditCards.returning(CreditCards).insertOrUpdate(cc.copy(isDefault = isDefault)).run().map {
      case Some(updated)  ⇒ Good(updated)
      case None           ⇒ Bad(NotFoundFailure.fromModel(cc))
    }
  }
}
