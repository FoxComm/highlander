package services

import scala.concurrent.{ExecutionContext, Future}

import models.{GiftCard, Customer, Customers, GiftCards, StoreAdmin, StoreAdmins}
import responses.{GiftCardResponse, CustomerResponse, StoreAdminResponse}
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).flatMap {
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
      giftCard ← GiftCards.findByCode(code).one.run()
      customer ← fetchCustomer(giftCard)
      storeAdmin ← fetchStoreAdmin(giftCard)
    } yield (giftCard, customer, storeAdmin)
  }

  private def fetchCustomer(gc: Option[GiftCard])(implicit db: Database, ec: ExecutionContext): Future[Option[Customer]] = {
    val default = Future.successful(None)
    gc.map {
      _.originType match {
        case GiftCard.CustomerPurchase ⇒ Customers.findById(1)
        case _                         ⇒ default
      }
    }.getOrElse(default)
  }

  private def fetchStoreAdmin(gc: Option[GiftCard])(implicit db: Database, ec: ExecutionContext): Future[Option[StoreAdmin]] = {
    val default = Future.successful(None)
    gc.map { giftCard ⇒
      giftCard.originType match {
        case GiftCard.CsrAppeasement  ⇒ StoreAdmins.findById(giftCard.originId)
        case _                        ⇒ default
      }
    }.getOrElse(default)
  }
}