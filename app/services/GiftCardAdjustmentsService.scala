package services

import scala.concurrent.{Future, ExecutionContext}

import models._
import models.{GiftCardAdjustment, Orders, OrderPayments, GiftCardAdjustments, GiftCards}
import responses.GiftCardAdjustmentsResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._

object GiftCardAdjustmentsService {

  type QuerySeq = GiftCardAdjustments.QuerySeq

  def forGiftCard(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[Root]]] = {
    val finder = GiftCards.findByCode(code)

    finder.selectOneWithMetadata { gc ⇒
      val query = GiftCardAdjustments.filterByGiftCardId(gc.id)
        .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
        .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

      val queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, ((giftCardAdj, _), _)) ⇒
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
          case other              ⇒ invalidSortColumn(other)
        }
      }

      queryWithMetadata.result.map { _.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj)
        }
      }
    }
  }
}
