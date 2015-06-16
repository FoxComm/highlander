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
          Future.successful(Bad(cardPayload.validationFailures.toList))
//          createRecords(stripeCustomer, cart, customer)

        case Bad(errors)          =>
          Future.successful(Bad(errors))
      }
    }
  }

//  private [this] def createRecords(stripeCustomer: StripeCustomer, cart: Cart, customer: Customer)
//                                  (implicit ec: ExecutionContext, db: Database): Response = {
//
//    val appliedPayment = AppliedPayment(cartId = cart.id, paymentMethodId = 1, // TODO: would do a lookup
//      paymentMethodType = card.paymentGateway,
//      appliedAmount = 0, status = Auth.toString, // TODO: use type and marshalling
//      responseCode = "ok") // TODO: make this real
//
//    val billingAddress = Address(customerId = customer.id, stateId = state.id, name = "Stripe",
//      street1 = stripeCard.getAddressLine1, street2 = Option(stripeCard.getAddressLine2),
//      city = stripeCard.getAddressCity, zip = stripeCard.getAddressZip)
//
//    /*
//      Create the TokenizedCreditCard, AppliedPayment, and billing Address (populated by the StripeCard)
//     */
//    val queries = for {
//      tokenId <- .returning(tokenCardsTable.map(_.id)) += card.copy(customerId = customer.id)
//      appliedPaymentId <- AppliedPayments.returningId += appliedPayment.copy(paymentMethodId = tokenId)
//      addressId <- BillingAddresses._create(billingAddress, appliedPaymentId)
//      c <- Carts._findById(cart.id).result.headOption
//    } yield c
//
//    db.run(queries.transactionally).flatMap { optCart =>
//      optCart.map { c =>
//        FullCart.fromCart(c).map { root =>
//          root.map(Good(_)).getOrElse(Bad(List("could not render cart")))
//        }
//      }.getOrElse(Future.successful(Bad(List(s"could not find cart with id=${cart.id}"))))
//    }
//  }
}

object CreditCardPaymentCreator {
  type Response = Future[FullCart.Root Or List[ErrorMessage]]

  def run(cart: Cart, customer: Customer, payload: CreditCardPayload)
         (implicit ec: ExecutionContext,
          db: Database): Response = {
    new CreditCardPaymentCreator(cart, customer, payload).run()
  }
}
