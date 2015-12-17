package services

import cats.data.Xor
import models.{StoreCreditAdjustment, Customers, OrderPayments, Orders, StoreCreditAdjustments, StoreCredits}
import responses.StoreCreditAdjustmentsResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.DbResultT.implicits._
import utils.DbResultT._

import scala.concurrent.{ExecutionContext, Future}

object StoreCreditAdjustmentsService {
  def forStoreCredit(id: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[Root]]] = {

    val finder = StoreCredits.filter(_.id === id)

    finder.selectOneWithMetadata { sc ⇒
      val query = StoreCreditAdjustments.filterByStoreCreditId(sc.id)
        .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
        .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

      val queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, ((storeCreditAdj, _), _)) ⇒
        StoreCreditAdjustments.matchSortColumn(s, storeCreditAdj)
      }

      queryWithMetadata.result.map {
        _.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj)
        }
      }
    }
  }

  def forCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[ResponseWithMetadata[Seq[Root]]] = {

    val query = StoreCreditAdjustments
      .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
      .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)
      .joinLeft(StoreCredits).on(_._1._1.storeCreditId === _.id)
      .filter(_._2.map(_.customerId) === customerId).map { case (((adjs, pmts), orders), _) ⇒
      (adjs, orders.map(_.referenceNumber))
    }

    val paginated = query.withMetadata.sortAndPageIfNeeded { case (s, (adj, _)) ⇒
      StoreCreditAdjustments.matchSortColumn(s, adj)
    }.result.map { results ⇒
      results.map((build _).tupled)
    }

    (for {
      _         ← * <~ Customers.mustFindById(customerId)
      response  ← * <~ ResultWithMetadata(result = paginated.result, metadata = paginated.metadata)
    } yield response).value.run().flatMap {
      case Xor.Left(f)    ⇒ Result.failures(f)
      case Xor.Right(res) ⇒ res.asResponseFuture.flatMap(Result.good)
    }
  }
}
