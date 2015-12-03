package services

import models.{GiftCardAdjustments, GiftCards, OrderPayments, Orders}
import responses.GiftCardAdjustmentsResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

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
        GiftCardAdjustments.matchSortColumn(s, giftCardAdj)
      }

      queryWithMetadata.result.map { _.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj)
        }
      }
    }
  }
}
