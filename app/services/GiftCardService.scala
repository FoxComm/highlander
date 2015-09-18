package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import shapeless._
import models.{GiftCardAdjustments, GiftCard, Customer, Customers, GiftCards, StoreAdmin, StoreAdmins}
import models.GiftCard.{Canceled, Active, OnHold}
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId = 1

  type Account = Customer :+: StoreAdmin :+: CNil

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).run().flatMap {
      case (Some(giftCard), Xor.Left(Some(customer))) ⇒
        val customerResponse = Some(CustomerResponse.build(customer))
        Result.right(GiftCardResponse.build(giftCard, customerResponse))
      case (Some(giftCard), Xor.Right(Some(storeAdmin))) ⇒
        val storeAdminResponse = Some(StoreAdminResponse.build(storeAdmin))
        Result.right(GiftCardResponse.build(giftCard, None, storeAdminResponse))
      case _ ⇒
        Result.failure(GiftCardNotFoundFailure(code))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    createGiftCardModel(admin, payload)
  }

  def updateStatusByCsr2(code: String, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val actions = for {
      oldGiftCard ← GiftCards.findByCode(code).one

      rowsAffected ← oldGiftCard.map { gc ⇒
        GiftCards.update(gc.copy(status = payload.status))
        //GiftCards._findById(gc.id).extract.map(_.status).update(payload.status)
      }.getOrElse(DBIO.successful(0))

      newGiftCard ← GiftCards.findByCode(code).one
    } yield (rowsAffected, newGiftCard)

    db.run(actions.transactionally).flatMap {
      case (_, None) ⇒
        Result.failure(GiftCardNotFoundFailure(code))
      case (0, _) ⇒
        Result.failure(GeneralFailure("Unable to update address"))
      case (_, Some(gc)) ⇒
        Result.good(GiftCardResponse.build(gc))
    }
  }

  def updateStatusByCsr(code: String, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
      giftCard ← GiftCards.findByCode(code).one

    } yield giftCard).run().flatMap {
      case Some(gc) ⇒
        val hasAuths = gc.availableBalance != gc.currentBalance

        (gc.transitionTo(payload.status) match {
          case Xor.Left(message)  ⇒ Result.failure(GeneralFailure(message))
          case Xor.Right(_)       ⇒ (payload.status, payload.reason) match {
            case (Canceled, Some(reason)) if hasAuths ⇒
              Result.failure(GeneralFailure("Open transactions should be canceled/completed"))
            case (Canceled, Some(reason)) if !hasAuths ⇒
              val canceledGc = gc.copy(status = payload.status, canceledAmount = Some(gc.availableBalance),
                canceledReason = payload.reason)
              updateModel(canceledGc, payload)
            case (Canceled, None)                     ⇒
              Result.failure(GeneralFailure("Please provide cancellation reason"))
            case (_, _)                               ⇒
              updateModel(gc.copy(status = payload.status), payload)
          }
        }).map(_.map(GiftCardResponse.build(_, None, None)))
      case _              ⇒
        Result.failure(GiftCardNotFoundFailure(code))
    }

  private def updateModel(gc: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[GiftCard] = {

    val query = GiftCards.findByCode(gc.code)
    val update = query.map { x ⇒
      (x.status, x.canceledReason, x.availableBalance, x.canceledAmount)
    }.update((payload.status, payload.reason, gc.availableBalance, gc.canceledAmount))

    db.run(update).flatMap { rowsAffected ⇒
      if (rowsAffected == 1) {
        db.run(query.one).flatMap {
          case Some(giftCard) ⇒ Result.right(giftCard)
          case None           ⇒ Result.failure(GiftCardNotFoundFailure(gc.code))
        }
      } else {
        Result.failure(GiftCardNotFoundFailure(gc.code))
      }
    }
  }

  private def createGiftCardModel(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val storeAdminResponse = Some(StoreAdminResponse.build(admin))
    val giftCard = GiftCard.buildAppeasement(admin, payload)

    createGiftCard(giftCard).map(_.map(GiftCardResponse.build(_, None, storeAdminResponse)))
  }

  private def createGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): Result[GiftCard] = {
    gc.validate match {
      case Valid(_)             ⇒ Result.fromFuture(GiftCards.save(gc).run())
      case Invalid(errors)      ⇒ Result.failures(errors.failure)
    }
  }

  private def fetchDetails(code: String)(implicit db: Database, ec: ExecutionContext) = for {
    giftCard  ← GiftCards.findByCode(code).one
    account   ← getAccount(giftCard)
  } yield (giftCard, account)

  private def getAccount(giftCard: Option[GiftCard])
    (implicit db: Database, ec: ExecutionContext): DBIO[Option[Customer] Xor Option[StoreAdmin]] = giftCard.map { gc ⇒
    gc.originType match {
      case GiftCard.CustomerPurchase ⇒
        Customers._findById(mockCustomerId).extract.one.map(Xor.left)
      case GiftCard.CsrAppeasement ⇒
        StoreAdmins._findById(gc.originId).extract.one.map(Xor.right)
      case _ ⇒
        DBIO.successful(Xor.left(None))
    }
  }.getOrElse(DBIO.successful(Xor.left(None)))
}
