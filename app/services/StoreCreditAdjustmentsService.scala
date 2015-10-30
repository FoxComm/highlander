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
        s.sortColumn match {
          case "id"               ⇒ if(s.asc) storeCreditAdj.id.asc               else storeCreditAdj.id.desc
          case "storeCreditId"    ⇒ if(s.asc) storeCreditAdj.storeCreditId.asc    else storeCreditAdj.storeCreditId.desc
          case "orderPaymentId"   ⇒ if(s.asc) storeCreditAdj.orderPaymentId.asc   else storeCreditAdj.orderPaymentId.desc
          case "debit"            ⇒ if(s.asc) storeCreditAdj.debit.asc            else storeCreditAdj.debit.desc
          case "availableBalance" ⇒ if(s.asc) storeCreditAdj.availableBalance.asc else storeCreditAdj.availableBalance.desc
          case "status"           ⇒ if(s.asc) storeCreditAdj.status.asc           else storeCreditAdj.status.desc
          case "createdAt"        ⇒ if(s.asc) storeCreditAdj.createdAt.asc        else storeCreditAdj.createdAt.desc
          case other              ⇒ invalidSortColumn(other)
        }
      }
      queryWithMetadata.result.map {
        _.map {
          case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
          case ((adj, _), _)                       ⇒ build(adj)
        }
      }
    }
  }
}
