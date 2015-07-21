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
      case None ⇒ Bad(NotFoundFailure(customer))
    }
  }

  def toggleDefaultCreditCard(customer: Customer, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Future[CreditCard Or Failure] = {
    db.run(for {
      default ← CreditCards.findDefaultByCustomerId(customer.id)
      _ ← CreditCards._findById(cardId).extract.map(_.isDefault).
        update(isDefault)
      cc ← CreditCards._findById(cardId).result.headOption
    } yield cc).map {
      case Some(c) ⇒ Good(c)
      case None ⇒ Bad(NotFoundFailure(CreditCard, cardId))
    }.recoverWith()
  }
}

