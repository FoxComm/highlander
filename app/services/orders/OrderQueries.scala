package services.orders

import scala.concurrent.ExecutionContext

import models.{Customer, OrderPayments, Customers, Orders, javaTimeSlickMapper}
import OrderPayments.scope._
import responses.{FullOrder, TheResponse, AllOrders}
import services.CartFailures.CustomerHasNoActiveOrder
import services.{Result, CartValidator}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._

object OrderQueries {

  def findAll(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): ResultWithMetadata[Seq[AllOrders.Root]] = {

    val ordersAndCustomers = Orders.join(Customers).on(_.customerId === _.id)
    val query = ordersAndCustomers.joinLeft(OrderPayments.creditCards).on(_._1.id === _.orderId)

    val sortedQuery = query.withMetadata.sortAndPageIfNeeded { case (s, ((order, customer), _)) ⇒
      s.sortColumn match {
        case "id"                         ⇒ if (s.asc) order.id.asc                   else order.id.desc
        case "referenceNumber"            ⇒ if (s.asc) order.referenceNumber.asc      else order.referenceNumber.desc
        case "customerId"                 ⇒ if (s.asc) order.customerId.asc           else order.customerId.desc
        case "state"                      ⇒ if (s.asc) order.state.asc                else order.state.desc
        case "isLocked"                   ⇒ if (s.asc) order.isLocked.asc             else order.isLocked.desc
        case "placedAt"                   ⇒ if (s.asc) order.placedAt.asc             else order.placedAt.desc
        case "remorsePeriodEnd"           ⇒ if (s.asc) order.remorsePeriodEnd.asc     else order.remorsePeriodEnd.desc
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

    sortedQuery.result.flatMap(xor ⇒ xorMapDbio(xor) { results ⇒
      DBIO.successful(results.map {
        case ((order, customer), payment) ⇒
          AllOrders.build(order, customer, payment)
      })
    })
  }

  def findOne(refNum: String)
    (implicit ec: ExecutionContext, db: Database): Result[TheResponse[FullOrder.Root]] = (for {
    order     ← * <~ Orders.mustFindByRefNum(refNum)
    validated ← * <~ CartValidator(order).validate
    response  ← * <~ FullOrder.fromOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).run()

  def findActiveOrderByCustomer(customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = (for {
    order     ← * <~ Orders.findActiveOrderByCustomer(customer).one.mustFindOr(CustomerHasNoActiveOrder(customer.id))
    fullOrder ← * <~ FullOrder.fromOrder(order).toXor
  } yield fullOrder).run()
}
