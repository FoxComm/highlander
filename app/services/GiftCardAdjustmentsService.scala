package services

import scala.concurrent.ExecutionContext

import models.{GiftCardAdjustments, GiftCards, OrderPayments, Orders}
import responses.GiftCardAdjustmentsResponse._
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._

object GiftCardAdjustmentsService {

  def forGiftCard(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    query = GiftCardAdjustments.filterByGiftCardId(giftCard.id)
          .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
          .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

    queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, ((giftCardAdj, _), _)) ⇒
          GiftCardAdjustments.matchSortColumn(s, giftCardAdj)
        }

    response ← * <~ queryWithMetadata.result.map(_.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _) ⇒ build(adj)
    }).toTheResponse
  } yield response).run()
}
