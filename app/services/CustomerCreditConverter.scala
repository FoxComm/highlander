package services

import cats.data.Xor
import cats.implicits._

import models._
import responses._

import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object CustomerCreditConverter {
  def toStoreCredit(code: String, customerId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[StoreCreditResponse.Root] = {

    def getDetails(code: String, customerId: Int): ResultT[(GiftCard, Customer)] = {
      val queries = for {
        gc ← GiftCards.findByCode(code).one
        customer ← Customers.findById(customerId).extract.one
        adj ← gc match {
          case Some(giftCard) ⇒ GiftCardAdjustments.lastAuthByGiftCardId(giftCard.id).one
          case _              ⇒ DBIO.successful(None)
        }
      } yield (gc, customer, adj)

      ResultT(queries.run().map {
        case (Some(gc), Some(customer), None) if gc.isActive ⇒
          Xor.right((gc, customer))
        case (Some(gc), _, _) if !gc.isActive ⇒
          Xor.left(GiftCardConvertFailure(gc).single)
        case (_, _, Some(_)) ⇒
          Xor.left(OpenTransactionsFailure.single)
        case (None, _, _) ⇒
          Xor.left(NotFoundFailure404(GiftCard, code).single)
        case (_, None, _) ⇒
          Xor.left(NotFoundFailure404(Customer, customerId).single)
      })
    }

    def saveStoreCredit(gc: GiftCard, admin: StoreAdmin): ResultT[DBIO[StoreCreditResponse.Root]] = {
      val queries = for {
        // Update status and make adjustment
        _ ← GiftCards.findActiveByCode(gc.code).map(_.status).update(GiftCard.FullyRedeemed)
        adjustment ← GiftCards.redeemToStoreCredit(gc, admin)

        // Finally, convert to Store Credit
        conversion ← StoreCreditFromGiftCards.saveNew(StoreCreditFromGiftCard(giftCardId = gc.id))
        sc ← StoreCredits.saveNew(StoreCredit.buildFromGcTransfer(customerId, gc).copy(originId = conversion.id))
      } yield sc

      ResultT.rightAsync(queries.flatMap(sc ⇒ lift(StoreCreditResponse.build(sc))))
    }

    val transformer = for {
      details ← getDetails(code, customerId)
      sc ← details match { case (gc, customer) ⇒
        saveStoreCredit(gc, admin)
      }
    } yield sc

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  def toGiftCard(id: Int, customerId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[GiftCardResponse.Root] = {

    def getDetails(id: Int, customerId: Int): ResultT[(StoreCredit, Customer)] = {
      val queries = for {
        sc ← StoreCredits.findActiveById(id).one
        customer ← Customers.findById(customerId).extract.one
        adj ← sc match {
          case Some(storeCredit)  ⇒ StoreCreditAdjustments.lastAuthByStoreCreditId(storeCredit.id).one
          case _                  ⇒ DBIO.successful(None)
        }
      } yield (sc, customer, adj)

      ResultT(queries.run().map {
        case (Some(sc), Some(customer), None) if sc.isActive ⇒
          Xor.right((sc, customer))
        case (Some(sc), _, _) if !sc.isActive ⇒
          Xor.left(StoreCreditConvertFailure(sc).single)
        case (_, _, Some(_)) ⇒
          Xor.left(OpenTransactionsFailure.single)
        case (None, _, _) ⇒
          Xor.left(NotFoundFailure404(StoreCredit, id).single)
        case (_, None, _) ⇒
          Xor.left(NotFoundFailure404(Customer, customerId).single)
      })
    }

    def saveGiftCard(sc: StoreCredit, admin: StoreAdmin): ResultT[DBIO[GiftCardResponse.Root]] = {
      val adminResponse = StoreAdminResponse.build(admin)
      val giftCard = GiftCard(originId = 0, originType = GiftCard.FromStoreCredit, currency = sc.currency,
        originalBalance = sc.currentBalance, currentBalance = sc.currentBalance)

      val queries = for {
        // Update status and make adjustment
        scUpdated ← StoreCredits.findActiveById(sc.id).map(_.status).update(StoreCredit.FullyRedeemed)
        adjustment ← StoreCredits.redeemToGiftCard(sc, admin)

        // Convert to Gift Card
        conversion ← GiftCardFromStoreCredits.saveNew(GiftCardFromStoreCredit(storeCreditId = sc.id))
        gc ← GiftCards.saveNew(giftCard.copy(originId = conversion.id))
      } yield gc

      ResultT.rightAsync(queries.flatMap(gc ⇒ lift(GiftCardResponse.build(gc, None, Some(adminResponse)))))
    }

    val transformer = for {
      details ← getDetails(id, customerId)
      gc ← details match { case (sc, customer) ⇒
        saveGiftCard(sc, admin)
      }
    } yield gc

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }
}
