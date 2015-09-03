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

final case class CreditCardManager(order: Order, customer: Customer, cardPayload: CreateCreditCard)
  (implicit ec: ExecutionContext, db: Database) {

  val gateway = StripeGateway()
  import CreditCardManager._

  def run(): Response = cardPayload.validate match {
    case failure @ validation.Result.Failure(violations) ⇒
      Result.failure(ValidationFailure(failure))
    case Success ⇒
      // creates the customer, card, and gives us getDefaultCard as the token
      gateway.createCustomerAndCard(customer, this.cardPayload).flatMap {
        case Xor.Right((stripeCustomer, stripeCard)) =>
          createRecords(stripeCustomer, stripeCard, order, customer).flatMap { optOrder =>
            optOrder.map { (o: Order) =>
              Result.fromFuture(FullOrder.fromOrder(o))
            }.getOrElse(Future.successful(Xor.left(List(NotFoundFailure(order)))))
          }

        case left @ Xor.Left(errors) ⇒ Future.successful(left)
      }
  }

  private [this] def createRecords(stripeCustomer: StripeCustomer, stripeCard: StripeCard,
    order: Order, customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {

    val appliedPayment = OrderPayment.fromStripeCustomer(stripeCustomer, order)
    val cc = CreditCard.build(stripeCustomer, stripeCard, this.cardPayload).copy(customerId = customer.id)
    val billingAddress = this.cardPayload.address.map(Address.fromPayload(_).copy(customerId = customer.id))

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

      o ← Orders._findById(order.id).result.headOption
    } yield o

    db.run(queries.transactionally)
  }
}

object CreditCardManager {
  type Response = Future[Failures Xor FullOrder.Root]

  def run(order: Order, customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Response = {
    new CreditCardManager(order, customer, payload).run()
  }
}

