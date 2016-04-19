package services.orders

import cats.implicits._
import failures.NotFoundFailure404
import models.customer.{Customer, Customers}
import models.order._
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.{StoreAdmin, javaTimeSlickMapper}
import models.objects.ObjectContext
import responses.TheResponse
import responses.order._
import services.{CartValidator, LogActivity, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.{EmptySortAndPage, SortAndPage}
import utils.Slick._
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object OrderQueries {

  def findAllByQuery(query: Orders.QuerySeq = Orders)(implicit ec: EC,
    sortAndPage: SortAndPage = EmptySortAndPage): DbResultT[TheResponse[Seq[AllOrders.Root]]] = {

    val ordersAndCustomers = query.join(Customers).on(_.customerId === _.id)

    val sortedQuery = ordersAndCustomers.withMetadata.sortAndPageIfNeeded { case (s, (order, customer)) ⇒
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
      DBIO.sequence(results.map {
        case (order, customer) ⇒
          OrderQueries.getPaymentState(order.id).map { paymentState ⇒
            AllOrders.build(order, customer.some, paymentState.some)
          }
      })
    }).toTheResponse
  }

  def findAll(implicit ec: EC, sortAndPage: SortAndPage = EmptySortAndPage): DbResultT[TheResponse[Seq[AllOrders.Root]]] =
    findAllByQuery(Orders)

  def list(implicit ec: EC, db: DB, sortAndPage: SortAndPage = EmptySortAndPage): Result[TheResponse[Seq[AllOrders.Root]]] =
    findAllByQuery(Orders).run()

  def listByCustomer(customer: Customer)(implicit ec: EC, db: DB,
    sortAndPage: SortAndPage = EmptySortAndPage): Result[TheResponse[Seq[AllOrders.Root]]] =
    findAllByQuery(Orders.filter(_.customerId === customer.id)).run()

  def findOne(refNum: String)(implicit ec: EC, db: DB): Result[TheResponse[FullOrder.Root]] = (for {
    order     ← * <~ Orders.mustFindByRefNum(refNum)
    validated ← * <~ CartValidator(order).validate()
    response  ← * <~ FullOrder.fromOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).run()

  def findOneByCustomer(refNum: String, customer: Customer)
    (implicit ec: EC, db: DB): Result[TheResponse[FullOrder.Root]] = (for {
    order     ← * <~ Orders.findOneByRefNumAndCustomer(refNum, customer)
                           .one
                           .mustFindOr(NotFoundFailure404(Orders, refNum))
    validated ← * <~ CartValidator(order).validate()
    response  ← * <~ FullOrder.fromOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).run()

  def findOrCreateCartByCustomer(customer: Customer, context: ObjectContext, admin: Option[StoreAdmin] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[FullOrder.Root] =
    findOrCreateCartByCustomerInner(customer, context, admin).runTxn()

  def findOrCreateCartByCustomerId(customerId: Int, context: ObjectContext, admin: Option[StoreAdmin] = None)
    (implicit ec: EC, db: DB, ac: AC): Result[FullOrder.Root] = (for {
    customer  ← * <~ Customers.mustFindById404(customerId)
    fullOrder ← * <~ findOrCreateCartByCustomerInner(customer, context, admin)
  } yield fullOrder).runTxn()

  def findOrCreateCartByCustomerInner(customer: Customer, context: ObjectContext, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC): DbResultT[FullOrder.Root] = for {
    result                  ← * <~ Orders.findActiveOrderByCustomer(customer).one
      .findOrCreateExtended(Orders.create(Order.buildCart(customer.id, context.id)))
    (order, foundOrCreated) = result
    fullOrder               ← * <~ FullOrder.fromOrder(order).toXor
    _                       ← * <~ logCartCreation(foundOrCreated, fullOrder, admin)
  } yield fullOrder

  private def logCartCreation(foundOrCreated: FoundOrCreated, order: FullOrder.Root, admin: Option[StoreAdmin])
    (implicit ec: EC, ac: AC) = foundOrCreated match {
    case Created ⇒ LogActivity.cartCreated(admin, order)
    case Found   ⇒ DbResult.unit
  }

  def getPaymentState(orderId: Int)(implicit ec: EC): DBIO[CreditCardCharge.State] = for {
    payments ← OrderPayments.findAllByOrderId(orderId).result
    authorized ← DBIO.sequence(payments.map(payment ⇒ payment.paymentMethodType match {
      case PaymentMethod.CreditCard ⇒
        import CreditCardCharge._
        CreditCardCharges.filter(_.orderPaymentId === payment.id).filter(_.state === (Auth: State)).size.result
      case PaymentMethod.GiftCard ⇒
        import GiftCardAdjustment._
        GiftCardAdjustments.filter(_.orderPaymentId === payment.id).filter(_.state === (Auth: State)).size.result
      case PaymentMethod.StoreCredit ⇒
        import StoreCreditAdjustment._
        StoreCreditAdjustments.filter(_.orderPaymentId === payment.id).filter(_.state === (Auth: State)).size.result
    }))
  // Using CreditCardCharge here as it has both Cart and Auth states. Consider refactoring.
  } yield (payments.size, authorized.sum) match {
    case (0, _) ⇒ CreditCardCharge.Cart
    case (pmt, auth) if pmt == auth ⇒ CreditCardCharge.Auth
    case _ ⇒ CreditCardCharge.Cart
  }

}
