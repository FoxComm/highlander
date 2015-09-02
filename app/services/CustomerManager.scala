package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Customers, CreditCard, CreditCards, Customer, StoreAdmin}

import com.github.tototoshi.slick.JdbcJodaSupport._
import org.joda.time.DateTime
import payloads.EditCreditCard
import slick.dbio.Effect.Read
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import slick.profile.SqlAction
import utils.Slick.UpdateReturning._
import utils.jdbc.withUniqueConstraint

import cats.data.Xor
import cats.data.Xor.{left, right}

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

  def deleteCreditCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val updateCc = CreditCards._findById(id).extract
      .filter(_.customerId === customerId)
      .map { cc ⇒ (cc.inWallet, cc.deletedAt) }
      .update((false, Some(DateTime.now())))

    db.run(updateCc.map { rows ⇒
      if (rows == 1) right({}) else left(creditCardNotFound(id))
    })
  }

  def editCreditCard(customerId: Int, id: Int, payload: EditCreditCard)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    def edit(cc: CreditCard) = {
      new StripeGateway().editCard(cc, payload).flatMap {
        case Xor.Left(f) ⇒
          Result.failures(f)

        case Xor.Right(_) ⇒
          val copied = cc.copy(
            parentId    = Some(cc.id),
            holderName  = payload.holderName.getOrElse(cc.holderName),
            expYear     = payload.expYear.getOrElse(cc.expYear),
            expMonth    = payload.expMonth.getOrElse(cc.expMonth)
          )

          val copyVersion = CreditCards.save(copied)
          val deactivate  = CreditCards._findById(cc.id).extract.map(_.inWallet).update(false)

          db.run((copyVersion >> deactivate).transactionally).map(_ ⇒ right({}))
      }
    }

    db.run(getCard(customerId, id)).flatMap {
      case None     ⇒
        Result.failures(creditCardNotFound(id))

      case Some(cc) ⇒
        if (!cc.inWallet)
          Result.failure(CannotUseInactiveCreditCard(cc))
        else
          edit(cc)
    }
  }

  def creditCardsInWalletFor(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[CreditCard]] =
    CreditCards.findInWalletByCustomerId(customerId).result.run()

  def getCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): DBIO[Option[CreditCard]] =
    CreditCards._findById(id).extract.filter(_.customerId === customerId).result.headOption

  private def creditCardNotFound(id: Int)     = NotFoundFailure(CreditCard, id).single
}

