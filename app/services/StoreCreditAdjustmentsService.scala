package services

import scala.concurrent.{Future, ExecutionContext}

import responses.StoreCreditAdjustmentsResponse.{Root, build}
import models._
import responses.StoreCreditAdjustmentsResponse.Root
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._

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
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Future[ResultWithMetadata[Seq[Root]]] = {

    val query = StoreCreditAdjustments
      .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
      .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)
      .join(Customers).on(_._1._1.storeCreditId === _.id)
      .filter(_._2.id === customerId)

    val queryWithMetadata = query.withMetadata.sortAndPageIfNeeded { case (s, (((storeCreditAdj, _), _), _)) ⇒
      StoreCreditAdjustments.matchSortColumn(s, storeCreditAdj)
    }

    val response = queryWithMetadata.result.map {
      _.map {
        case (((adj, Some(payment)), Some(order)), _) ⇒ build(adj, Some(order.referenceNumber))
        case (((adj, _), _), _)                       ⇒ build(adj)
      }
    }

    Future.successful(response)
  }
}
