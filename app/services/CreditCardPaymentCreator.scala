package services

import models._

import responses.FullOrder
import payloads.CreditCardPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import com.stripe.model.{Token, Card => StripeCard, Customer => StripeCustomer}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import collection.JavaConversions.mapAsJavaMap

import utils.Validation
import utils.Validation.Result.{ Success}
import utils.{ Validation ⇒ validation }

// TODO(yax): make this abstract to handle multiple Gateways
case class CreditCardPaymentCreator(order: Order, customer: Customer, cardPayload: CreditCardPayload)
                                   (implicit ec: ExecutionContext, db: Database) {

  val gateway = StripeGateway()
  import CreditCardPaymentCreator._

  def run(): Response = cardPayload.validate match {
    case failure @ validation.Result.Failure(violations) ⇒
      Future.successful(Bad(List(ValidationFailure(failure))))
    case Success ⇒
      // creates the customer, card, and gives us getDefaultCard as the token
      gateway.createCustomerAndCard(customer, this.cardPayload).flatMap {
        case Good(stripeCustomer) =>
          createRecords(stripeCustomer, order, customer).flatMap { optOrder =>
            optOrder.map { (o: Order) =>
              FullOrder.fromOrder(o).map { root =>
                root.map(Good(_)).getOrElse(Bad(List(GeneralFailure("could not render order"))))
              }
            }.getOrElse(Future.successful(Bad(List(NotFoundFailure(s"could not find order with id=${order.id}")))))
          }

        case Bad(errors) ⇒ Future.successful(Bad(errors))
      }
  }

  // creates CreditCardGateways, uses its id for an AppliedPayment record, and attempts to associate billing info
  // from stripe to a BillingAddress
  private [this] def createRecords(stripeCustomer: StripeCustomer, order: Order, customer: Customer)
                                  (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {

    val appliedPayment = AppliedPayment.fromStripeCustomer(stripeCustomer, order)
    val cc = CreditCardGateway.build(stripeCustomer, this.cardPayload).copy(customerId = customer.id)
    val billingAddress = this.cardPayload.address.map(Address.fromPayload(_).copy(customerId = customer.id))

    val queries = for {
      ccId <- CreditCardGateways.returningId += cc
      appliedPaymentId <- AppliedPayments.returningId += appliedPayment.copy(paymentMethodId = ccId)
      _ <- billingAddress.map(BillingAddresses._create(_, appliedPaymentId)).getOrElse(DBIO.successful(Unit))
      c <- Orders._findById(order.id).result.headOption
    } yield c

    db.run(queries.transactionally)
  }
}

object CreditCardPaymentCreator {
  type Response = Future[FullOrder.Root Or List[Failure]]

  def run(order: Order, customer: Customer, payload: CreditCardPayload)
         (implicit ec: ExecutionContext,
          db: Database): Response = {
    new CreditCardPaymentCreator(order, customer, payload).run()
  }
}

