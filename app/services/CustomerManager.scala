package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Customers, CreditCard, CreditCards, Customer, StoreAdmin}
import org.scalactic._
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Slick.UpdateReturning._

object CustomerManager {
  def toggleDisabled(customerId: Int, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Future[Customer Or Failure] = {
    db.run(for {
      updated ← Customers.filter(_.id === customerId).map { t ⇒ (t.disabled, t.disabledBy) }.
        updateReturning(Customers.map(identity), (disabled, Some(admin.id))).headOption
    } yield updated).map {
      case Some(c)  ⇒ Good(c)
      case None     ⇒ Bad(NotFoundFailure(Customer, customerId))
    }
  }

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Future[CreditCard Or Failure] = {

    if (isDefault) {
      setDefaultCreditCard(customerId, cardId)
    } else {
      CreditCards._findById(cardId).extract.filter(_.customerId === customerId).map(_.isDefault).
        updateReturning(CreditCards.map(identity), false).headOption.run().map {
        case Some(cc) ⇒ Good(cc)
        case None     ⇒ Bad(NotFoundFailure(CreditCards, cardId))
      }
    }
  }

  def setDefaultCreditCard(customerId: Int, cardId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[CreditCard Or Failure] = {

    val actions = for {
      existing ← CreditCards._findDefaultByCustomerId(customerId)
      updated ← existing match {
        case None ⇒ CreditCards._findById(cardId).extract.map(_.isDefault).
          updateReturning(CreditCards.map(identity), true).headOption
        case Some(_) =>
          DBIO.successful(None)
      }
    } yield (existing, updated)

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

