package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Reasons, Reason}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object ReasonService {

  type QuerySeq = Reasons.QuerySeq

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeq = {
    val sortedQuery = sortAndPage.sort match {
      case Some(s) ⇒ query.sortBy { reason ⇒
        s.sortColumn match {
          case "id"           ⇒ if(s.asc) reason.id.asc           else reason.id.desc
          case "storeAdminId" ⇒ if(s.asc) reason.storeAdminId.asc else reason.storeAdminId.desc
          case "parentId"     ⇒ if(s.asc) reason.parentId.asc     else reason.parentId.desc
          case "body"         ⇒ if(s.asc) reason.body.asc         else reason.body.desc
          case _                  ⇒ reason.id.asc
        }
      }
      case None    ⇒ query
    }
    sortedQuery.paged
  }

  def queryAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeq =
    sortedAndPaged(Reasons)

  def listAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[Seq[Reason]] = {
    Result.fromFuture(queryAll.result.run())
  }
}
