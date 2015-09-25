package services

import scala.concurrent.ExecutionContext

import responses.GiftCardAdjustmentsResponse
import models.{Orders, OrderPayments, GiftCardAdjustments, GiftCards}
import responses.GiftCardAdjustmentsResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object GiftCardAdjustmentsService {
  def forGiftCard(code: String)(implicit db: Database, ec: ExecutionContext): Result[Seq[Root]] = {
    val finder = GiftCards.findByCode(code)

    finder.findOneAndRun { gc ⇒
      val query = GiftCardAdjustments.filterByGiftCardId(gc.id)
        .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
        .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

      val adjustments = db.run(query.result).map { results ⇒
        results.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, gc, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj, gc)
        }
      }

      DbResult.fromFuture(adjustments)
    }
  }
}
