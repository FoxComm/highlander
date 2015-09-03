package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import models.{Order, Orders, Customer, Addresses, Address, CreditCard, CreditCards, OrderPayments, OrderPayment,
OrderBillingAddress, OrderBillingAddresses}
import payloads.CreateCreditCard
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Validation.Result.Success
import utils.{Validation ⇒ validation}
import utils.Slick.implicits._

object CreditCardManager {
  def createCardForOrder(order: Order, customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    val gateway = StripeGateway()
    payload.validate match {
      case failure@validation.Result.Failure(violations) ⇒
        Result.failure(ValidationFailure(failure))
      case Success ⇒
        // creates the customer, card, and gives us getDefaultCard as the token
        gateway.createCustomerAndCard(customer, payload).flatMap {
          case Xor.Right((stripeCustomer, stripeCard)) =>
            createRecords(stripeCustomer, stripeCard, order, customer, payload).flatMap { optOrder =>
              optOrder.map { (o: Order) =>
                Result.fromFuture(FullOrder.fromOrder(o))
              }.getOrElse(Future.successful(Xor.left(List(NotFoundFailure(order)))))
            }

          case left@Xor.Left(errors) ⇒ Future.successful(left)
        }

    }
  }

  private def createRecords(stripeCustomer: StripeCustomer, stripeCard: StripeCard,
    order: Order, customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {

    val appliedPayment = OrderPayment.fromStripeCustomer(stripeCustomer, order)
    val cc = CreditCard.build(stripeCustomer, stripeCard, payload).copy(customerId = customer.id)
    val billingAddress = payload.address.map(Address.fromPayload(_).copy(customerId = customer.id))

    val queries = for {
      address ← billingAddress.map { address ⇒
        Addresses.save(address.copy(customerId = customer.id)).map(Some(_))
      }.getOrElse(DBIO.successful(None))

      card ← address.map { address ⇒
        CreditCards.save(cc.copy(billingAddressId = address.id)).map(Some(_))
      }.getOrElse(DBIO.successful(None))

      orderPayment ← card.map { card ⇒
        OrderPayments.save(appliedPayment.copy(paymentMethodId = card.id)).map(Some(_))
      }.getOrElse(DBIO.successful(None))

      _ ← address.map { address ⇒
        val builtAddress = OrderBillingAddress.buildFromAddress(address)
        OrderBillingAddresses.save(orderPayment.fold(builtAddress)((orderPayment: OrderPayment) ⇒
          builtAddress.copy(orderPaymentId = orderPayment.id))).map(Some(_))
      }.getOrElse(DBIO.successful(None))

      o ← Orders._findById(order.id).extract.one
    } yield o

    db.run(queries.transactionally)
  }
}


