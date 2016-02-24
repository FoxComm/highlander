package services

import cats.data.Xor
import models.customer.Customers
import models.order.{OrderPayments, Orders}
import models.payment.storecredit.{StoreCreditAdjustments, StoreCredits}
import responses.StoreCreditAdjustmentsResponse.{Root, build}
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._
import utils.DbResultT.implicits._
import utils.DbResultT._

import scala.concurrent.ExecutionContext

object StoreCreditAdjustmentsService {

  def forStoreCredit(id: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = (for {
    storeCredit ← * <~ StoreCredits.mustFindById404(id)
    query = StoreCreditAdjustments.filterByStoreCreditId(storeCredit.id)
      .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
      .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

    queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, ((storeCreditAdj, _), _)) ⇒
      StoreCreditAdjustments.matchSortColumn(s, storeCreditAdj)
    }

    response ← * <~ queryWithMetadata.result.map(_.map {
      case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
      case ((adj, _), _) ⇒ build(adj)
    }).toTheResponse
  } yield response).run()

  def forCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = (for {
    _ ← * <~ Customers.mustFindById404(customerId)

    query = StoreCreditAdjustments
      .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
      .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)
      .joinLeft(StoreCredits).on(_._1._1.storeCreditId === _.id)
      .filter(_._2.map(_.customerId) === customerId).map { case (((adjs, pmts), orders), _) ⇒
      (adjs, orders.map(_.referenceNumber))
    }

    paginated = query.withMetadata.sortAndPageIfNeeded { case (s, (adj, _)) ⇒
      StoreCreditAdjustments.matchSortColumn(s, adj)
    }.result.map { results ⇒
      results.map((build _).tupled)
    }

    response ← * <~ ResultWithMetadata(result = paginated.result, metadata = paginated.metadata).toTheResponse
  } yield response).run()
}
