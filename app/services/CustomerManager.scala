package services

import scala.concurrent.{ExecutionContext, Future}

import models.{CreditCard, CreditCards, Customer, Customers ⇒ CustomersTable, StoreAdmin}
import org.scalactic._
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object CustomerManager {
  // TODO: use UPDATE _ RETURNING *
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

  // TODO: use UPDATE _ RETURNING *
  def setDefaultCreditCard(customer: Customer, cardId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[CreditCard Or Failure] = {
    val actions = for {
      existing ← CreditCards._findDefaultByCustomerId(customer.id)
      _ ← existing.fold(CreditCards._toggleDefault(cardId, true)) { cc ⇒ DBIO.successful(0) }
      creditCard ← CreditCards._findById(cardId).result.headOption
    } yield (existing, creditCard)

    db.run(actions.transactionally).map {
      case (None, None) ⇒
        Bad(NotFoundFailure(CreditCards, cardId))
      case (Some(_), _) ⇒
        Bad(GeneralFailure("customer already has default credit card"))
      case (None, Some(cc)) ⇒
        Good(cc)
    }
  }
}

