package services

import scala.concurrent.ExecutionContext

import responses.StoreCreditAdjustmentsResponse.{Root, build}
import models.{Orders, OrderPayments, StoreCreditAdjustments, StoreCredit, StoreCredits}
import responses.StoreCreditAdjustmentsResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object StoreCreditAdjustmentsService {
  def forStoreCredit(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Seq[Root]] = {
    val finder = StoreCredits.filter(_.id === id)

    finder.selectOneForUpdate { sc ⇒
      val query = StoreCreditAdjustments.filterByStoreCreditId(sc.id)
        .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
        .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

      val adjustments = db.run(query.result).map { results ⇒
        results.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj)
        }
      }

      DbResult.fromFuture(adjustments)
    }
  }
}
