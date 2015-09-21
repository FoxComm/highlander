package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import shapeless._
import models.{GiftCardAdjustment, GiftCardAdjustments, GiftCard, Customer, Customers, GiftCards, StoreAdmin,
StoreAdmins}
import models.GiftCard.{Canceled, Active, OnHold}
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
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

  def updateStatusByCsr(code: String, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val genericFailure = GeneralFailure("Unable to update GiftCard")

    val actions = for {
      oldGiftCard ← GiftCards.findByCode(code).one

      rowsAffected ← oldGiftCard.map { gc ⇒
        isUpdateAllowed(gc, payload) match {
          case Xor.Right(updatedGc) ⇒ GiftCards.update(updatedGc).map(Xor.right)
          case Xor.Left(failure)    ⇒ DBIO.successful(Xor.left(failure))
        }
      }.getOrElse(DBIO.successful(Xor.left(genericFailure)))

      newGiftCard ← GiftCards.findByCode(code).one
    } yield (rowsAffected, newGiftCard)

    db.run(actions.transactionally).flatMap {
      case (_, None)                ⇒ Result.failure(GiftCardNotFoundFailure(code))
      case (Xor.Left(failure), _)   ⇒ Result.failure(failure)
      case (Xor.Right(1), Some(gc)) ⇒ Result.good(GiftCardResponse.build(gc))
      case (_, _)                   ⇒ Result.failure(genericFailure)
    }
  }

  private def isUpdateAllowed(gc: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr): Xor[Failure, GiftCard] = {
    //val hasAuths = GiftCardAdjustments.filterAuthByGiftCardId(gc.id).run()
    val hasAuths = gc.availableBalance != gc.currentBalance

    gc.transitionTo(payload.status) match {
      case Xor.Left(message)  ⇒ Xor.Left(GeneralFailure(message))
      case Xor.Right(_)       ⇒ (payload.status, payload.reason) match {
        case (Canceled, Some(reason)) ⇒
          if (hasAuths) {
            Xor.Left(GeneralFailure("Open transactions should be canceled/completed"))
          } else {
            Xor.Right(gc.copy(status = payload.status, canceledAmount = Some(gc.availableBalance),
              canceledReason = payload.reason))
          }
        case (Canceled, None)                     ⇒
          Xor.Left(GeneralFailure("Please provide cancellation reason"))
        case (_, _)                               ⇒
          Xor.Right(gc.copy(status = payload.status))
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
