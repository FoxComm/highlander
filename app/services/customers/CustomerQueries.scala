package services.customers

object CustomerQueries

/*
import models.customer.Customers
import responses.TheResponse
import responses.CustomerResponse.{RootSimple, buildForList}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.db._
import utils.db._
import utils.db.DbResultT._
import utils.aliases._

object CustomerQueries {

  def findAllByQuery(query: Customers.QuerySeq = Customers)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DbResultT[TheResponse[Seq[RootSimple]]] = {

    val sortedQuery = query.withMetadata.sortAndPageIfNeeded { case (s, customer) ⇒
      s.sortColumn match {
        case "id"                ⇒ if(s.asc) customer.id.asc                 else customer.id.desc
        case "isDisabled"        ⇒ if(s.asc) customer.isDisabled.asc         else customer.isDisabled.desc
        case "disabledBy"        ⇒ if(s.asc) customer.disabledBy.asc         else customer.disabledBy.desc
        case "isBlacklisted"     ⇒ if(s.asc) customer.isBlacklisted.asc      else customer.isBlacklisted.desc
        case "blacklistedBy"     ⇒ if(s.asc) customer.blacklistedBy.asc      else customer.blacklistedBy.desc
        case "blacklistedReason" ⇒ if(s.asc) customer.blacklistedReason.asc  else customer.blacklistedReason.desc
        case "email"             ⇒ if(s.asc) customer.email.asc              else customer.email.desc
        case "name"              ⇒ if(s.asc) customer.name.asc               else customer.name.desc
        case "phoneNumber"       ⇒ if(s.asc) customer.phoneNumber.asc        else customer.phoneNumber.desc
        case "location"          ⇒ if(s.asc) customer.location.asc           else customer.location.desc
        case "modality"          ⇒ if(s.asc) customer.modality.asc           else customer.modality.desc
        case "isGuest"           ⇒ if(s.asc) customer.isGuest.asc            else customer.isGuest.desc
        case "createdAt"         ⇒ if(s.asc) customer.createdAt.asc          else customer.createdAt.desc
        case other               ⇒ invalidSortColumn(other)
      }
    }

    sortedQuery.result.map(_.map(buildForList)).toTheResponse
  }

  def findAll(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DbResultT[TheResponse[Seq[RootSimple]]] =
    findAllByQuery(Customers)
}
*/