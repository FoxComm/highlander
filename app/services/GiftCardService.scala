package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import shapeless._
import models.{GiftCardAdjustments, GiftCard, Customer, Customers, GiftCards, Reasons, StoreAdmin,
StoreAdmins}
import models.GiftCard.Canceled
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId = 1

  type Account = Customer :+: StoreAdmin :+: CNil
  type QuerySeq = Query[GiftCards, GiftCard, Seq]

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

  def updateStatusByCsr(code: String, payload: payloads.GiftCardUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val finder = GiftCards.findByCode(code)

    finder.findOneAndRun { gc ⇒
      gc.transitionTo(payload.status) match {
        case Xor.Left(message) ⇒ DbResult.failure(GeneralFailure(message))
        case Xor.Right(_) ⇒ (payload.status, payload.reason) match {
          case (Canceled, Some(reason)) ⇒
            cancelByCsr(finder, gc, payload, admin)
          case (Canceled, None) ⇒
            DbResult.failure(EmptyCancellationReasonFailure)
          case (_, _) ⇒
            val update = finder.map(_.status).updateReturning(GiftCards.map(identity), payload.status).head
            DbResult.fromDbio(update.flatMap { gc ⇒ DBIO.successful(GiftCardResponse.build(gc)) })
        }
      }
    }
  }

  private def cancelByCsr(finder: QuerySeq, gc: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr,
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database) = {

    GiftCardAdjustments.lastAuthByGiftCardId(gc.id).one.flatMap {
      case Some(adjustment) ⇒
        DbResult.failure(OpenTransactionsFailure)
      case None ⇒
        Reasons.findById(payload.reason.get).flatMap {
          case None ⇒
            DbResult.failure(InvalidCancellationReasonFailure)
          case _ ⇒
            val data = (payload.status, Some(gc.availableBalance), payload.reason)
            val cancellation = finder
              .map { gc ⇒ (gc.status, gc.canceledAmount, gc.canceledReason) }
              .updateReturning(GiftCards.map(identity), data)
              .head

            Console.println("Cancel adjustment generation...")
            val cancelAdjustment = GiftCards.cancelByCsr(gc, admin)

            DbResult.fromDbio(cancelAdjustment >> cancellation.flatMap {
              gc ⇒ DBIO.successful(GiftCardResponse.build(gc))
            })
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
