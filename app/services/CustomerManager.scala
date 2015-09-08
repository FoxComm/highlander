package services

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

import cats._
import cats.data.{OptionTInstances, XorT, OptionT, Xor}
import cats.data.Xor.{left, right}
import cats.data.Validated.{Invalid, Valid}
import com.github.tototoshi.slick.PostgresJodaSupport._
import models.{Address, Addresses, CreditCard, CreditCards, Customer, Customers, OrderPayments, Orders, StoreAdmin}
import models.Orders.scope._
import models.OrderPayments.scope._
import org.joda.time.DateTime
import payloads.EditCreditCard
import slick.dbio
import slick.dbio.Effect.All
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import cats.implicits._
import utils.Slick.implicits._
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
    (implicit ec: ExecutionContext, db: Database): Result[CreditCard] = {

    def update(cc: CreditCard): ResultT[DBIO[CreditCard]] = {
      if (!cc.inWallet)
        ResultT.leftAsync(CannotUseInactiveCreditCard(cc).single)
      else {
        val updated = cc.copy(
          parentId = Some(cc.id),
          holderName = payload.holderName.getOrElse(cc.holderName),
          expYear = payload.expYear.getOrElse(cc.expYear),
          expMonth = payload.expMonth.getOrElse(cc.expMonth)
        )

        val newVersion = CreditCards.save(updated)
        val deactivate = CreditCards._findById(cc.id).extract.map(_.inWallet).update(false)

        ResultT(new StripeGateway().editCard(updated).map {
          case Xor.Left(f)  ⇒ Xor.left(f)
          case Xor.Right(_) ⇒ Xor.right(deactivate >> newVersion)
        })
      }
    }

    def createNewAddressIfProvided(dbio: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = ResultT.rightAsync(
      if (payload.address.isDefined)
        dbio.flatMap { cc ⇒
          Addresses.save(Address.fromCreditCard(cc).copy(customerId = customerId)) >> DBIO.successful(cc)
        }
      else
        dbio
    )

    def cascadeChangesToCarts(edits: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = {
      val action = edits.flatMap { updated ⇒
        val paymentIds = for {
          orders ← Orders.findByCustomerId(customerId).cartOnly
          pmts ← OrderPayments.filter(_.paymentMethodId === updated.parentId).giftCards if pmts.orderId == orders.id
        } yield pmts.id

        OrderPayments.filter(_.id in paymentIds).map(_.paymentMethodId).update(updated.id).map(_ ⇒ updated)
      }

      ResultT.rightAsync(action)
    }

    val getCardAndAddressChange: ResultT[CreditCard] = {
      val queries = for {
        creditCard  ← getCard(customerId, id)
        address     ← (payload.addressId, payload.address) match {
          case (Some(addressId), _) ⇒
            Addresses._findById(addressId).extract.one

          case (_, Some(addressPayload)) ⇒
            DBIO.successful(Address.fromPayload(addressPayload).some)

          case _ ⇒
            DBIO.successful(None)
        }
      } yield (creditCard, address)

      ResultT(queries.run().map {
        case (Some(cc), address)  ⇒ Xor.right(address.fold(cc)(cc.copyFromAddress))
        case (None, _)            ⇒ Xor.left(creditCardNotFound(id))
      })
    }

    val transformer: ResultT[DBIO[CreditCard]] = for {
      _       ← ResultT.fromXor(payload.validate.toXor.leftMap(_.failure))
      cc      ← getCardAndAddressChange
      updated ← update(cc)
      withAddress ← createNewAddressIfProvided(updated)
      payment ← cascadeChangesToCarts(withAddress)
    } yield payment

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  def creditCardsInWalletFor(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[CreditCard]] =
    CreditCards.findInWalletByCustomerId(customerId).result.run()

  def getCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): DBIO[Option[CreditCard]] =
    CreditCards._findById(id).extract.filter(_.customerId === customerId).one

  private def creditCardNotFound(id: Int) = NotFoundFailure(CreditCard, id).single
}

