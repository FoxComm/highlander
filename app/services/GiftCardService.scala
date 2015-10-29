package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import cats.implicits._

import shapeless._
import models._
import models.GiftCard.{FromStoreCredit, CsrAppeasement, CustomerPurchase}
import models.GiftCard.Canceled
import models.GiftCardSubtypes.scope._
import responses.{GiftCardSubTypesResponse, GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse._
import responses.GiftCardBulkResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId = 1

  val gcManuals = TableQuery[GiftCardManuals]

  type Account = Customer :+: StoreAdmin :+: CNil
  type QuerySeq = GiftCards.QuerySeq

  def getOriginTypes(implicit db: Database, ec: ExecutionContext): Result[Seq[GiftCardSubTypesResponse.Root]] = {
    GiftCardSubtypes.select({ subTypes ⇒
      DbResult.good(GiftCardSubTypesResponse.build(GiftCard.OriginType.types.toSeq, subTypes))
    })
  }

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    GiftCards.queryAll.result.map(_.map(GiftCardResponse.build(_)))
  }

  def findByCode(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    val query = GiftCards.queryByCode(code)
    query.result.map(_.map(GiftCardResponse.build(_)))
  }

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).run().flatMap {
      case (Some(giftCard), Xor.Left(Some(customer))) ⇒
        val customerResponse = Some(CustomerResponse.build(customer))
        Result.right(GiftCardResponse.build(giftCard, customerResponse))
      case (Some(giftCard), Xor.Right(Some(storeAdmin))) ⇒
        val storeAdminResponse = Some(StoreAdminResponse.build(storeAdmin))
        Result.right(GiftCardResponse.build(giftCard, None, storeAdminResponse))
      case _ ⇒
        Result.failure(NotFoundFailure404(GiftCard, code))
    }
  }

  def createByAdmin(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    payload.validate match {
      case Invalid(errors) ⇒ Result.failures(errors)
      case Valid(_)        ⇒ createGiftCardModel(admin, payload)
    }
  }

  def createBulkByAdmin(admin: StoreAdmin, payload: payloads.GiftCardBulkCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[ItemResult]] = {

    payload.validate match {
      case Valid(_) ⇒
        val responses = (1 to payload.quantity).map { num ⇒
          val itemPayload = payloads.GiftCardCreateByCsr(balance = payload.balance, reasonId = payload.reasonId,
            currency = payload.currency)

          createByAdmin(admin, itemPayload).map(buildItemResult(_))
        }

        val future = Future.sequence(responses).flatMap { seq ⇒
          Future.successful(seq)
        }

        Result.fromFuture(future)
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  def bulkUpdateStatusByCsr(payload: payloads.GiftCardBulkUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[ItemResult]] = {

    payload.validate match {
      case Valid(_) ⇒
        val responses = payload.codes.map { code ⇒
          val itemPayload = payloads.GiftCardUpdateStatusByCsr(payload.status, payload.reasonId)
          updateStatusByCsr(code, itemPayload, admin).map(buildItemResult(_, Some(code)))
        }

        val future = Future.sequence(responses).flatMap { seq ⇒
          Future.successful(seq)
        }

        Result.fromFuture(future)
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  def updateStatusByCsr(code: String, payload: payloads.GiftCardUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    def cancelOrUpdate(finder: QuerySeq, giftCard: GiftCard) = (payload.status, payload.reasonId) match {
      case (Canceled, Some(reason)) ⇒
        cancelByCsr(finder, giftCard, payload, admin)
      case (Canceled, None) ⇒
        DbResult.failure(EmptyCancellationReasonFailure)
      case (_, _) ⇒
        val update = finder.map(_.status).updateReturning(GiftCards.map(identity), payload.status).headOption
        update.flatMap {
          case Some(gc) ⇒ DbResult.good(GiftCardResponse.build(gc))
          case _        ⇒ DbResult.failure(NotFoundFailure404(GiftCard, giftCard.code))
        }
    }

    payload.validate match {
      case Valid(_) ⇒
        val finder = GiftCards.findByCode(code)
        finder.selectOneForUpdate { gc ⇒
          gc.transitionTo(payload.status) match {
            case Xor.Left(message) ⇒ DbResult.failure(GeneralFailure(message))
            case Xor.Right(_)      ⇒ cancelOrUpdate(finder, gc)
          }
        }
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  private def cancelByCsr(finder: QuerySeq, gc: GiftCard, payload: payloads.GiftCardUpdateStatusByCsr,
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database) = {

    GiftCardAdjustments.lastAuthByGiftCardId(gc.id).one.flatMap {
      case Some(adjustment) ⇒
        DbResult.failure(OpenTransactionsFailure)
      case None ⇒
        Reasons.findOneById(payload.reasonId.get).flatMap {
          case None ⇒
            DbResult.failure(InvalidCancellationReasonFailure)
          case _ ⇒
            val data = (payload.status, Some(gc.availableBalance), payload.reasonId)
            val cancellation = finder
              .map { gc ⇒ (gc.status, gc.canceledAmount, gc.canceledReason) }
              .updateReturning(GiftCards.map(identity), data)
              .head

            val cancelAdjustment = GiftCards.cancelByCsr(gc, admin)

            DbResult.fromDbio(cancelAdjustment >> cancellation.map(GiftCardResponse.build(_)))
        }
    }
  }

  private def createGiftCardModel(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    def prepareForCreate(payload: payloads.GiftCardCreateByCsr): ResultT[(Reason, Option[GiftCardSubtype])] = {
      val queries = for {
        reason   ← Reasons.findOneById(payload.reasonId)
        subtype  ← payload.subTypeId match {
          case Some(id) ⇒ GiftCardSubtypes.csrAppeasements.filter(_.id === id).one
          case None     ⇒ lift(None)
        }
      } yield (reason, subtype)

      ResultT(queries.run().map {
        case (None, _) ⇒
          Xor.left(NotFoundFailure400(Reason, payload.reasonId).single)
        case (_, None) if payload.subTypeId.isDefined ⇒
          Xor.left(NotFoundFailure400(GiftCardSubtype, payload.subTypeId.head).single)
        case (Some(r), s) ⇒
          Xor.right((r, s))
      })
    }

    def saveGiftCard(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr): ResultT[DBIO[Root]] = {
      val actions = for {
        origin  ← GiftCardManuals.save(GiftCardManual(adminId = admin.id, reasonId = payload.reasonId))
        gc      ← GiftCards.save(GiftCard.buildAppeasement(payload, origin.id))
      } yield gc

      val storeAdminResponse = Some(StoreAdminResponse.build(admin))
      ResultT.rightAsync(actions.flatMap(gc ⇒ lift(build(gc, None, storeAdminResponse))))
    }

    val transformer = for {
      prepare ← prepareForCreate(payload)
      gc ← prepare match { case (reason, subtype) ⇒
        val newPayload = payload.copy(subTypeId = subtype.map(_.id))
        saveGiftCard(admin, newPayload)
      }
    } yield gc

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  private def fetchDetails(code: String)(implicit db: Database, ec: ExecutionContext) = for {
    giftCard  ← GiftCards.findByCode(code).one
    gcOrigin  ← giftCard match {
      case Some(gc) if gc.originType == GiftCard.CsrAppeasement ⇒ gcManuals.filter(_.id === gc.originId).one
      case _                                                    ⇒ DBIO.successful(None)
    }
    account   ← getAccount(giftCard, gcOrigin)
  } yield (giftCard, account)

  private def getAccount(giftCard: Option[GiftCard], origin: Option[GiftCardManual])
    (implicit db: Database, ec: ExecutionContext): DBIO[Option[Customer] Xor Option[StoreAdmin]] = giftCard.map { gc ⇒
    (gc.originType, origin) match {
      case (GiftCard.CustomerPurchase, _) ⇒
        Customers.findById(mockCustomerId).extract.one.map(Xor.left)
      case (GiftCard.CsrAppeasement, Some(o)) ⇒
        StoreAdmins.findById(o.adminId).extract.one.map(Xor.right)
      case _ ⇒
        lift(Xor.left(None))
    }
  }.getOrElse(lift(Xor.left(None)))
}
