package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Customers, CreditCard, CreditCards, Customer, StoreAdmin}
import org.scalactic._
import com.github.tototoshi.slick.JdbcJodaSupport._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Slick.UpdateReturning._
import utils.jdbc.{RecordNotUnique, withUniqueConstraint}

object CustomerManager {
  def toggleDisabled(customerId: Int, disabled: Boolean, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Future[Customer Or Failures] = {
    db.run(for {
      updated ← Customers.filter(_.id === customerId).map { t ⇒ (t.disabled, t.disabledBy) }.
        updateReturning(Customers.map(identity), (disabled, Some(admin.id))).headOption
    } yield updated).map {
      case Some(c) ⇒ Good(c)
      case None ⇒ Bad(NotFoundFailure(Customer, customerId).single)
    }
  }

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Future[CreditCard Or Failures] = {

    val result = withUniqueConstraint {
      CreditCards._findById(cardId).extract.filter(_.customerId === customerId).map(_.isDefault).
        updateReturning(CreditCards.map(identity), isDefault).headOption.run()
    } { notUnique ⇒ CustomerHasDefaultCreditCard }

    result.map {
      case Good(Some(cc)) ⇒ Good(cc)
      case Good(None)     ⇒ Bad(NotFoundFailure(CreditCard, cardId).single)
      case Bad(f)         ⇒ Bad(f.single)
    }
  }

  def deleteCreditCard(customerId: Int, adminId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Int Or NotFoundFailure] = {

    db.run(CreditCards._findById(id).extract
      .filter(_.customerId === customerId)
      .map { cc ⇒ (cc.isDefault, cc.deletedAt, cc.deletedBy) }
      .update((false, Some(DateTime.now()), Some(adminId)))
    ).map {
      case 1  ⇒ Good(1)
      case _  ⇒ Bad(NotFoundFailure(CreditCard, id))
    }
  }
}

