package services.rmas

import scala.concurrent.ExecutionContext

import models._
import responses.AllRmas
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._

object RmaQueries {

  def findAll(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): ResultWithMetadata[Seq[AllRmas.Root]] = {

    val sortedQuery = Rmas.withMetadata.sortAndPageIfNeeded { case (s, rma) ⇒
      s.sortColumn match {
        case "id"                         ⇒ if (s.asc) rma.id.asc                   else rma.id.desc
        case "referenceNumber"            ⇒ if (s.asc) rma.referenceNumber.asc      else rma.referenceNumber.desc
        case "customerId"                 ⇒ if (s.asc) rma.customerId.asc           else rma.customerId.desc
        case "status"                     ⇒ if (s.asc) rma.status.asc               else rma.status.desc
        case "locked"                     ⇒ if (s.asc) rma.locked.asc               else rma.locked.desc
        case other                        ⇒ invalidSortColumn(other)
      }
    }

    sortedQuery.result.flatMap(xor ⇒ xorMapDbio(xor) { results ⇒
      DBIO.sequence(results.map(AllRmas.build))
    })
  }
}