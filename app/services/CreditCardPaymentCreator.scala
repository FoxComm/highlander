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

// TODO(yax): make this abstract to handle multiple Gateways
case class CreditCardPaymentCreator(order: Order, customer: Customer, cardPayload: CreditCardPayload)
                                   (implicit ec: ExecutionContext, db: Database) {

  val gateway = StripeGateway()
  import CreditCardPaymentCreator._

  def run(): Response = {
    if (!cardPayload.isValid) {
      Future.successful(Bad(cardPayload.validationFailures.toList))
    } else {
      // creates the customer, card, and gives us getDefaultCard as the token
      gateway.createCustomerAndCard(customer, this.cardPayload).flatMap {
        case Good(stripeCustomer) =>
          createRecords(stripeCustomer, order, customer).flatMap { optOrder =>
            optOrder.map { c =>
              FullOrder.fromOrder(c).map { root =>
                root.map(Good(_)).getOrElse(Bad(List("could not render order")))
              }
            }.getOrElse(Future.successful(Bad(List(s"could not find order with id=${order.id}"))))
          }

        case Bad(errors)          =>
          Future.successful(Bad(errors))
      }
    }
  }

  // creates CreditCardGateways, uses its id for an AppliedPayment record, and attempts to associate billing info
  // from stripe to a BillingAddress
  private [this] def createRecords(stripeCustomer: StripeCustomer, order: Order, customer: Customer)
                                  (implicit ec: ExecutionContext, db: Database): Future[Option[Order]] = {

    val appliedPayment = AppliedPayment.fromStripeCustomer(stripeCustomer, order)

    // TODO: attempt to get billingAddress
//    val billingAddress = Address(customerId = customer.id, stateId = state.id, name = "Stripe",
//      street1 = stripeCard.getAddressLine1, street2 = Option(stripeCard.getAddressLine2),
//      city = stripeCard.getAddressCity, zip = stripeCard.getAddressZip)

    val cc = CreditCardGateway.build(stripeCustomer, this.cardPayload).copy(customerId = customer.id)

    /*
      Create the TokenizedCreditCard, AppliedPayment, and billing Address (populated by the StripeCard)
     */
    val queries = for {
      ccId <- CreditCardGateways.returningId += cc
      appliedPaymentId <- AppliedPayments.returningId += appliedPayment.copy(paymentMethodId = ccId)
      // addressId <- BillingAddresses._create(billingAddress, appliedPaymentId)
      c <- Orders._findById(order.id).result.headOption
    } yield c

    db.run(queries.transactionally)
  }
}

object CreditCardPaymentCreator {
  type Response = Future[FullOrder.Root Or List[ErrorMessage]]

  def run(order: Order, customer: Customer, payload: CreditCardPayload)
         (implicit ec: ExecutionContext,
          db: Database): Response = {
    new CreditCardPaymentCreator(order, customer, payload).run()
  }
}
