package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import shapeless._
import models.{GiftCard, Customer, Customers, GiftCards, StoreAdmin, StoreAdmins}
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse.Root
import slick.dbio
import slick.dbio.Effect.All
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId  = 1
  val codeLength      = 16
  val generator       = scala.util.Random

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

  def createByAdmin(admin: StoreAdmin, payload: payloads.GiftCardCreatePayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    createGiftCardModel(admin, payload)
  }

  private def createGiftCardModel(admin: StoreAdmin, payload: payloads.GiftCardCreatePayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val storeAdminResponse = Some(StoreAdminResponse.build(admin))

    createGiftCard(GiftCard(
      code = generateCode,
      originId = admin.id,
      originType = GiftCard.CsrAppeasement,
      status = GiftCard.Active,
      currency = payload.currency,
      originalBalance = payload.balance,
      availableBalance = payload.balance,
      currentBalance = payload.balance
    )).map(_.map(GiftCardResponse.build(_, None, storeAdminResponse)))
  }

  private def generateCode: String = {
    generator.alphanumeric.take(codeLength).mkString.toUpperCase
  }

  private def createGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): Result[GiftCard] = {
    gc.validate match {
      case Valid(_)         ⇒ Result.fromFuture(GiftCards.save(gc).run())
      case Invalid(errors)  ⇒ Result.failure(errors.head)
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
