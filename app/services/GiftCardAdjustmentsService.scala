package services

import scala.concurrent.ExecutionContext

import models._
import models.{GiftCardAdjustment, Orders, OrderPayments, GiftCardAdjustments, GiftCards}
import responses.GiftCardAdjustmentsResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._

object GiftCardAdjustmentsService {

  type QuerySeq = GiftCardAdjustments.QuerySeq

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeq = {
    val sortedQuery = sortAndPage.sort match {
      case Some(s) ⇒ query.sortBy { giftCardAdj ⇒
        s.sortColumn match {
          case "id"               ⇒ if(s.asc) giftCardAdj.id.asc               else giftCardAdj.id.desc
          case "giftCardId"       ⇒ if(s.asc) giftCardAdj.giftCardId.asc       else giftCardAdj.giftCardId.desc
          case "orderPaymentId"   ⇒ if(s.asc) giftCardAdj.orderPaymentId.asc   else giftCardAdj.orderPaymentId.desc
          case "storeAdminId"     ⇒ if(s.asc) giftCardAdj.storeAdminId.asc     else giftCardAdj.storeAdminId.desc
          case "credit"           ⇒ if(s.asc) giftCardAdj.credit.asc           else giftCardAdj.credit.desc
          case "debit"            ⇒ if(s.asc) giftCardAdj.debit.asc            else giftCardAdj.debit.desc
          case "availableBalance" ⇒ if(s.asc) giftCardAdj.availableBalance.asc else giftCardAdj.availableBalance.desc
          case "status"           ⇒ if(s.asc) giftCardAdj.status.asc           else giftCardAdj.status.desc
          case "createdAt"        ⇒ if(s.asc) giftCardAdj.createdAt.asc        else giftCardAdj.createdAt.desc
          case _                  ⇒ giftCardAdj.id.asc
        }
      }
      case None    ⇒ query
    }
    sortedQuery.paged
  }

  def forGiftCard(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[Seq[Root]] = {
    val finder = GiftCards.findByCode(code)

    finder.selectOneForUpdate { gc ⇒
      val query = sortedAndPaged(GiftCardAdjustments.filterByGiftCardId(gc.id))
        .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
        .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

      val adjustments = query.result.run().map { results ⇒
        results.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj)
        }
      }

      DbResult.fromFuture(adjustments)
    }
  }
}
