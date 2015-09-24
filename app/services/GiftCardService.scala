package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import shapeless._
import models.{GiftCardAdjustment, GiftCardAdjustments, GiftCard, Customer, Customers, GiftCards, Reasons, StoreAdmin,
StoreAdmins}
import models.GiftCard.{Canceled, Active, OnHold}
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse._
import responses.GiftCardBulkCreateResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId = 1
  val bulkCreateLimit = 20

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

  def createBulkByAdmin(admin: StoreAdmin, payload: payloads.GiftCardBulkCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[BulkResponses] = {

    (payload.count > 0, payload.count <= bulkCreateLimit) match {
      case (true, true) ⇒
        val responses = (1 to payload.count).map { number ⇒
          val payloadSingle = payloads.GiftCardCreateByCsr(balance = payload.balance, currency = payload.currency)
          createGiftCardModel(admin, payloadSingle).flatMap {
            case Xor.Left(errors) ⇒ Future.successful(buildBulkResponse(None, Some(errors.map(_.description.mkString))))
            case Xor.Right(sc)    ⇒ Future.successful(buildBulkResponse(Some(sc)))
          }
        }

        val future = Future.sequence(responses).flatMap { seq ⇒
          Future.successful(buildBulkResponses(seq))
        }

        Result.fromFuture(future)
      case (false, _)   ⇒ Result.failure(GeneralFailure("Count value must be greater than zero"))
      case (_, false)   ⇒ Result.failure(GeneralFailure("Bulk create limit exceeded"))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    createGiftCardModel(admin, payload)
  }

  def updateStatusByCsr(code: String, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val finder = GiftCards.findByCode(code)

    finder.findOneAndRun { gc ⇒
      gc.transitionTo(payload.status) match {
        case Xor.Left(message) ⇒ DbResult.failure(GeneralFailure(message))
        case Xor.Right(_) ⇒ (payload.status, payload.reason) match {
          case (Canceled, Some(reason)) ⇒
            cancelByCsr(finder, gc, payload)
          case (Canceled, None) ⇒
            DbResult.failure(GeneralFailure("Please provide cancellation reason"))
          case (_, _) ⇒
            val update = finder.map(_.status).updateReturning(GiftCards.map(identity), payload.status).head
            DbResult.fromDbio(update.flatMap { gc ⇒ DBIO.successful(GiftCardResponse.build(gc)) })
        }
      }
    }
  }

  private def cancelByCsr(finder: QuerySeq, gc: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database) = {

    GiftCardAdjustments.lastAuthByGiftCardId(gc.id).one.flatMap {
      case Some(adjustment) ⇒
        DbResult.failure(GeneralFailure("Open transactions should be canceled/completed"))
      case None ⇒
        Reasons.findById(payload.reason.get).flatMap {
          case None ⇒
            DbResult.failure(GeneralFailure("Cancellation reason doesn't exist"))
          case _ ⇒
            val data = (payload.status, Some(gc.availableBalance), payload.reason)
            val cancellation = finder
              .map { gc ⇒ (gc.status, gc.canceledAmount, gc.canceledReason) }
              .updateReturning(GiftCards.map(identity), data)
              .head

            DbResult.fromDbio(cancellation.flatMap { gc ⇒ DBIO.successful(GiftCardResponse.build(gc)) })
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
    (gc.originalBalance, gc.validate) match {
      case (0, _)               ⇒ Result.failure(GeneralFailure("Balance must be greater than zero"))
      case (_, Valid(_))        ⇒ Result.fromFuture(GiftCards.save(gc).run())
      case (_, Invalid(errors)) ⇒ Result.failures(errors.failure)
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
