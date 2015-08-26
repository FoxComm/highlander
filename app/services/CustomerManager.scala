package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Customers, CreditCard, CreditCards, Customer, StoreAdmin}

import com.github.tototoshi.slick.JdbcJodaSupport._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Slick.UpdateReturning._
import utils.jdbc.{RecordNotUnique, withUniqueConstraint}

import cats.data.Xor

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

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Result[CreditCard] = {
    val result = withUniqueConstraint {
      CreditCards._findById(cardId).extract.filter(_.customerId === customerId).map(_.isDefault).
        updateReturning(CreditCards.map(identity), isDefault).headOption.run()
    } { notUnique ⇒ CustomerHasDefaultCreditCard }

    result.flatMap {
      case Xor.Right(Some(cc)) ⇒ Result.good(cc)
      case Xor.Right(None)     ⇒ Result.failure(NotFoundFailure(CreditCard, cardId))
      case Xor.Left(f)         ⇒ Result.failure(f)
    }
  }

  def deleteCreditCard(customerId: Int, adminId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Int] = {

    db.run(CreditCards._findById(id).extract
      .filter(_.customerId === customerId)
      .map { cc ⇒ (cc.isDefault, cc.deletedAt, cc.deletedBy) }
      .update((false, Some(DateTime.now()), Some(adminId)))
    ).flatMap {
      case 1 ⇒ Result.good(1)
      case _ ⇒ Result.failure(NotFoundFailure(CreditCard, id))
    }
  }
}

