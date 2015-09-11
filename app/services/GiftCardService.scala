package services

import scala.concurrent.{ExecutionContext, Future}

import models.{GiftCard, Customer, Customers, GiftCards, StoreAdmin, StoreAdmins}
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId = 1

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).run().flatMap {
      case (Some(giftCard), Some(customer), _) ⇒
        val customerResponse = Some(CustomerResponse.build(customer))
        Result.right(GiftCardResponse.build(giftCard, customerResponse))
      case (Some(giftCard), _, Some(storeAdmin)) ⇒
        val storeAdminResponse = Some(StoreAdminResponse.build(storeAdmin))
        Result.right(GiftCardResponse.build(giftCard, None, storeAdminResponse))
      case _ ⇒
        Result.failure(GiftCardNotFoundFailure(code))
    }
  }

  private def fetchDetails(code: String)(implicit db: Database, ec: ExecutionContext) = {
    for {
      giftCard ← GiftCards.findByCode(code).one
      customer ← fetchCustomer(giftCard)
      storeAdmin ← fetchStoreAdmin(giftCard)
    } yield (giftCard, customer, storeAdmin)
  }

  private def fetchCustomer(gc: Option[GiftCard])(implicit db: Database, ec: ExecutionContext): DBIO[Option[Customer]]
  = {
    val default = DBIO.successful(None)
    gc.map {
      _.originType match {
        case GiftCard.CustomerPurchase ⇒ Customers._findById(mockCustomerId).extract.one
        case _                         ⇒ default
      }
    }.getOrElse(default)
  }

  private def fetchStoreAdmin(gc: Option[GiftCard])(implicit db: Database, ec: ExecutionContext): DBIO[Option[StoreAdmin]] = {
    val default = DBIO.successful(None)
    gc.map { giftCard ⇒
      giftCard.originType match {
        case GiftCard.CsrAppeasement  ⇒ StoreAdmins._findById(giftCard.originId).extract.one
        case _                        ⇒ default
      }
    }.getOrElse(default)
  }
}