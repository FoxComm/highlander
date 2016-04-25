package services.rmas

import cats.implicits._
import models.customer.Customers
import models.rma.Rmas
import models.StoreAdmins
import responses.{AllRmas, TheResponse}
import services.Result
import slick.driver.PostgresDriver.api._
import utils.http.CustomDirectives
import utils.http.CustomDirectives.{EmptySortAndPage, SortAndPage}
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object RmaQueries {

  def findAll(query: Rmas.QuerySeq)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage = EmptySortAndPage): Result[TheResponse[Seq[AllRmas.Root]]] =
    findAllDbio(query).run()

  def findAllDbio(query: Rmas.QuerySeq)(implicit ec: EC,
    sortAndPage: SortAndPage = EmptySortAndPage): DbResultT[TheResponse[Seq[AllRmas.Root]]] = {

    val rmasAndCustomers = query.join(Customers).on(_.customerId === _.id)

    val withStoreAdmins = rmasAndCustomers.joinLeft(StoreAdmins).on(_._1.storeAdminId === _.id)

    val sortedQuery = withStoreAdmins.withMetadata.sortAndPageIfNeeded { case (s, ((rma, customer), admin)) ⇒
      s.sortColumn match {
        case "id"                         ⇒ if (s.asc) rma.id.asc                     else rma.id.desc
        case "referenceNumber"            ⇒ if (s.asc) rma.referenceNumber.asc        else rma.referenceNumber.desc
        case "orderId"                    ⇒ if (s.asc) rma.orderId.asc                else rma.orderId.desc
        case "orderRefNum"                ⇒ if (s.asc) rma.orderRefNum.asc            else rma.orderRefNum.desc
        case "rmaType"                    ⇒ if (s.asc) rma.rmaType.asc                else rma.rmaType.desc
        case "state"                      ⇒ if (s.asc) rma.state.asc                  else rma.state.desc
        case "isLocked"                   ⇒ if (s.asc) rma.isLocked.asc               else rma.isLocked.desc
        case "customerId"                 ⇒ if (s.asc) rma.customerId.asc             else rma.customerId.desc
        case "storeAdminId"               ⇒ if (s.asc) rma.storeAdminId.asc           else rma.storeAdminId.desc
        case "customer_id"                ⇒ if (s.asc) customer.id.asc                else customer.id.desc
        case "customer_isDisabled"        ⇒ if (s.asc) customer.isDisabled.asc        else customer.isDisabled.desc
        case "customer_disabledBy"        ⇒ if (s.asc) customer.disabledBy.asc        else customer.disabledBy.desc
        case "customer_isBlacklisted"     ⇒ if (s.asc) customer.isBlacklisted.asc     else customer.isBlacklisted.desc
        case "customer_blacklistedBy"     ⇒ if (s.asc) customer.blacklistedBy.asc     else customer.blacklistedBy.desc
        case "customer_blacklistedReason" ⇒ if (s.asc) customer.blacklistedReason.asc else customer.blacklistedReason.desc
        case "customer_email"             ⇒ if (s.asc) customer.email.asc             else customer.email.desc
        case "customer_name"              ⇒ if (s.asc) customer.name.asc              else customer.name.desc
        case "customer_phoneNumber"       ⇒ if (s.asc) customer.phoneNumber.asc       else customer.phoneNumber.desc
        case "customer_location"          ⇒ if (s.asc) customer.location.asc          else customer.location.desc
        case "customer_modality"          ⇒ if (s.asc) customer.modality.asc          else customer.modality.desc
        case "customer_isGuest"           ⇒ if (s.asc) customer.isGuest.asc           else customer.isGuest.desc
        case "customer_createdAt"         ⇒ if (s.asc) customer.createdAt.asc         else customer.createdAt.desc
        case other                        ⇒ invalidSortColumn(other)
      }
    }

    sortedQuery.result.map(_.map { case ((rma, customer), admin) ⇒
      AllRmas.build(rma, customer.some, admin)
    }).toTheResponse
  }
}
