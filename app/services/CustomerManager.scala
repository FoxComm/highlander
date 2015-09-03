package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.data.Xor.{left, right}
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.{CreditCard, CreditCards, Customer, Customers, OrderPayments, Orders, StoreAdmin}
import models.Orders.scope._
import models.OrderPayments.scope._
import org.joda.time.DateTime
import payloads.EditCreditCard
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils._
import utils.jdbc.withUniqueConstraint

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
    (implicit ec: ExecutionContext, db: Database): Result[Int] = {

    def edit(cc: CreditCard): Result[DBIO[CreditCard]] = {
      new StripeGateway().editCard(cc, payload).map {
        case Xor.Left(f) ⇒
          left(f)

        case Xor.Right(_) ⇒
          val updated = cc.copy(
            parentId    = Some(cc.id),
            holderName  = payload.holderName.getOrElse(cc.holderName),
            expYear     = payload.expYear.getOrElse(cc.expYear),
            expMonth    = payload.expMonth.getOrElse(cc.expMonth)
          )

          val newVersion  = CreditCards.save(updated)
          val deactivate  = CreditCards._findById(cc.id).extract.map(_.inWallet).update(false)

          right(deactivate >> newVersion)
      }
    }

    def cascadeChangesToCarts(edits: DBIO[CreditCard], old: CreditCard) = edits.flatMap { updated ⇒
      val paymentIds = for {
        orders ← Orders.findByCustomerId(customerId).cartOnly
        pmts ← OrderPayments.filter(_.paymentMethodId === old.id).giftCards if pmts.orderId == orders.id
      } yield pmts.id

      OrderPayments.filter(_.id in paymentIds).map(_.paymentMethodId).update(updated.id)
    }

    db.run(getCard(customerId, id)).flatMap {
      case None     ⇒
        Result.failures(creditCardNotFound(id))

      case Some(cc) ⇒
        if (!cc.inWallet)
          Result.failure(CannotUseInactiveCreditCard(cc))
        else
          edit(cc).flatMap { xor ⇒
            xor.fold(Result.left(_), cascadeChangesToCarts(_, cc).transactionally.run().map(Xor.right))
          }
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

