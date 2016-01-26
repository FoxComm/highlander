package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{GiftCardAdjustments, GiftCards, OrderPayments, Orders}
import responses.GiftCardAdjustmentsResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object GiftCardAdjustmentsService {

  def forGiftCard(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[Root]]] = {
    // FIXME #714
    db.run(GiftCards.mustFindByCode(code).map {
      case Xor.Right(giftCard) ⇒
        val query = GiftCardAdjustments.filterByGiftCardId(giftCard.id)
          .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
          .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

        val queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, ((giftCardAdj, _), _)) ⇒
          GiftCardAdjustments.matchSortColumn(s, giftCardAdj)
        }

        queryWithMetadata.result.map(_.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _) ⇒ build(adj)
        })

      case Xor.Left(failures) ⇒
        ResultWithMetadata.fromFailures(failures)
    })
  }
}
