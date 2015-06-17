package services

import models._

import responses.FullCart
import payloads.CreditCardPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import com.stripe.model.{Token, Card => StripeCard, Customer => StripeCustomer}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import collection.JavaConversions.mapAsJavaMap

// TODO(yax): make this abstract to handle multiple Gateways
case class CreditCardPaymentCreator(cart: Cart, customer: Customer, cardPayload: CreditCardPayload)
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
          createRecords(stripeCustomer, cart, customer).flatMap { optCart =>
            optCart.map { c =>
              FullCart.fromCart(c).map { root =>
                root.map(Good(_)).getOrElse(Bad(List("could not render cart")))
              }
            }.getOrElse(Future.successful(Bad(List(s"could not find cart with id=${cart.id}"))))
          }

        case Bad(errors)          =>
          Future.successful(Bad(errors))
      }
    }
  }

  // creates CreditCardGateways, uses its id for an AppliedPayment record, and attempts to associate billing info
  // from stripe to a BillingAddress
  private [this] def createRecords(stripeCustomer: StripeCustomer, cart: Cart, customer: Customer)
                                  (implicit ec: ExecutionContext, db: Database): Future[Option[Cart]] = {

    val appliedPayment = AppliedPayment.fromStripeCustomer(stripeCustomer, cart)
    val cc = CreditCardGateway.build(stripeCustomer, this.cardPayload).copy(customerId = customer.id)
    val billingAddress = this.cardPayload.address.map(Address.fromPayload(_).copy(customerId = customer.id))

    val queries = for {
      ccId <- CreditCardGateways.returningId += cc
      appliedPaymentId <- AppliedPayments.returningId += appliedPayment.copy(paymentMethodId = ccId)
      _ <- BillingAddresses._create(billingAddress.get, appliedPaymentId) if billingAddress.isDefined
      c <- Carts._findById(cart.id).result.headOption
    } yield c

    db.run(queries.transactionally)
  }
}

object CreditCardPaymentCreator {
  type Response = Future[FullCart.Root Or List[ErrorMessage]]

  def run(cart: Cart, customer: Customer, payload: CreditCardPayload)
         (implicit ec: ExecutionContext,
          db: Database): Response = {
    new CreditCardPaymentCreator(cart, customer, payload).run()
  }
}
