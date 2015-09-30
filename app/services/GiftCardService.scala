package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import shapeless._
import models.{GiftCardAdjustments, GiftCard, Customer, Customers, GiftCards, Reasons, StoreAdmin,
StoreAdmins}
import models.GiftCard.Canceled
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse._
import responses.GiftCardBulkUpdateResponse._
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

  def createBulkByAdmin(admin: StoreAdmin, payload: payloads.GiftCardBulkCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {

    payload.validate match {
      case Valid(_)        ⇒
        val payloadSingle = payloads.GiftCardCreateByCsr(balance = payload.balance, currency = payload.currency)
        val toInsert = (1 to payload.quantity).map { _ ⇒ GiftCard.buildAppeasement(admin, payloadSingle) }

        // Validate only first, since all other are the same
        toInsert.head.validate match {
          case Valid(_) ⇒
            // Insert multiple values in a single transaction
            val query = (for {
              giftCards ← (GiftCards ++= toInsert.toSeq) >> GiftCards.sortBy(_.id.desc).take(payload.quantity).result
            } yield giftCards).flatMap { seq ⇒
              val storeAdminResponse = Some(StoreAdminResponse.build(admin))
              lift(seq.map(build(_, None, storeAdminResponse)).reverse)
            }

            Result.fromFuture(query.transactionally.run())
          case Invalid(errors) ⇒
            Result.failures(errors.failure)
        }
      case Invalid(errors) ⇒
        Result.failures(errors.failure)
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    payload.validate match {
      case Invalid(errors) ⇒ Result.failures(errors.failure)
      case Valid(_)        ⇒ createGiftCardModel(admin, payload)
    }
  }

  def bulkUpdateStatusByCsr(payload: payloads.GiftCardBulkUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Responses] = {

    payload.validate match {
      case Valid(_) ⇒
        val responses = payload.codes.map { code ⇒
          val itemPayload = payloads.GiftCardUpdateStatusByCsr(payload.status, payload.reason)
          updateStatusByCsr(code, itemPayload, admin).map {
            case Xor.Left(errors) ⇒ buildResponse(code, None, Some(errors.map(_.description.mkString)))
            case Xor.Right(sc)    ⇒ buildResponse(code, Some(sc))
          }
        }

        val future = Future.sequence(responses).flatMap { seq ⇒
          Future.successful(buildResponses(seq))
        }

        Result.fromFuture(future)
      case Invalid(errors) ⇒
        Result.failures(errors.failure)
    }
  }

  def updateStatusByCsr(code: String, payload: payloads.GiftCardUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    payload.validate match {
      case Valid(_) ⇒
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
                DbResult.fromDbio(update.flatMap { gc ⇒ lift(GiftCardResponse.build(gc)) })
            }
          }
        }
      case Invalid(errors) ⇒
        Result.failures(errors.failure)
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

            val cancelAdjustment = GiftCards.cancelByCsr(gc, admin)

            DbResult.fromDbio(cancelAdjustment >> cancellation.flatMap {
              gc ⇒ lift(GiftCardResponse.build(gc))
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
      case Valid(_)        ⇒ Result.fromFuture(GiftCards.save(gc).run())
      case Invalid(errors) ⇒ Result.failures(errors.failure)
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
        lift(Xor.left(None))
    }
  }.getOrElse(lift(Xor.left(None)))
}
