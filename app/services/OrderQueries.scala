package services

import scala.concurrent.ExecutionContext

import models._
import responses.AllOrders
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object OrderQueries {

  def findAll(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DBIO[Seq[AllOrders.Root]] = {
    val ordersAndCustomers = for {
      (order, customer) ← Orders.join(Customers).on(_.customerId === _.id)
    } yield (order, customer)

    val creditCardPayments = for {
      (orderPayment, creditCard) ← OrderPayments.join(CreditCards).on(_.id === _.id)
    } yield (orderPayment, creditCard)

    val query = ordersAndCustomers.joinLeft(creditCardPayments).on(_._1.id === _._1.orderId)

    val sortedQuery = sortAndPage.sort match {
      case Some(s) ⇒ query.sortBy { case ((order, customer), _) ⇒
        s.sortColumn match {
          case "id"                         ⇒ if(s.asc) order.id.asc                   else order.id.desc
          case "referenceNumber"            ⇒ if(s.asc) order.referenceNumber.asc      else order.referenceNumber.desc
          case "customerId"                 ⇒ if(s.asc) order.customerId.asc           else order.customerId.desc
          case "status"                     ⇒ if(s.asc) order.status.asc               else order.status.desc
          case "locked"                     ⇒ if(s.asc) order.locked.asc               else order.locked.desc
          case "placedAt"                   ⇒ if(s.asc) order.placedAt.asc             else order.placedAt.desc
          case "remorsePeriodEnd"           ⇒ if(s.asc) order.remorsePeriodEnd.asc     else order.remorsePeriodEnd.desc
          case "customer_id"                ⇒ if(s.asc) customer.id.asc                else customer.id.desc
          case "customer_isDisabled"        ⇒ if(s.asc) customer.isDisabled.asc        else customer.isDisabled.desc
          case "customer_disabledBy"        ⇒ if(s.asc) customer.disabledBy.asc        else customer.disabledBy.desc
          case "customer_isBlacklisted"     ⇒ if(s.asc) customer.isBlacklisted.asc     else customer.isBlacklisted.desc
          case "customer_blacklistedBy"     ⇒ if(s.asc) customer.blacklistedBy.asc     else customer.blacklistedBy.desc
          case "customer_blacklistedReason" ⇒ if(s.asc) customer.blacklistedReason.asc else customer.blacklistedReason.desc
          case "customer_email"             ⇒ if(s.asc) customer.email.asc             else customer.email.desc
          case "customer_name"              ⇒ if(s.asc) customer.name.asc              else customer.name.desc
          case "customer_phoneNumber"       ⇒ if(s.asc) customer.phoneNumber.asc       else customer.phoneNumber.desc
          case "customer_location"          ⇒ if(s.asc) customer.location.asc          else customer.location.desc
          case "customer_modality"          ⇒ if(s.asc) customer.modality.asc          else customer.modality.desc
          case "customer_isGuest"           ⇒ if(s.asc) customer.isGuest.asc           else customer.isGuest.desc
          case "customer_createdAt"         ⇒ if(s.asc) customer.createdAt.asc         else customer.createdAt.desc
          case _                           ⇒ order.id.asc
        }
      }
      case None    ⇒ query
    }

    sortedQuery.paged.result.flatMap { results ⇒
      DBIO.sequence {
        results.map { case ((order, customer), payment) ⇒
          AllOrders.build(order, customer, payment.map(_._1))
        }
      }
    }
  }

}
