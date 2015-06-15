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

case class CreditCardPaymentCreator(cart: Cart, customer: Customer, cardPayload: CreditCardPayload)
                                   (implicit ec: ExecutionContext,
                                   db: Database) {

  val gateway = StripeGateway()
  import CreditCardPaymentCreator._

  def run(): Response = {
    Future.successful(Bad(cardPayload.validationFailures.toList))
//    if (!cardPayload.isValid) {
//      Future.successful(Bad(cardPayload.validationFailures.toList))
//    } else {
//      for {
//        result <- gateway.createCustomerAndCard(customer, this.cardPayload)
//        stripeCustomer <- result
//        //_ <- gateway.listCards(stripeCustomer)
//      } yield stripeCustomer
//      Future.successful(Bad(List("blah")))
//    }
  }

    // store stripeCustomer.getId to customer (maybe as JSON data?)
    // Create card on their behalf in stripe —> https://stripe.com/docs/api#cards
    // StripeCard.create
    // store card token to tokenized_credit_cards

  def createStripeCard(stripeCustomer: StripeCustomer): Unit = {
//    for {
//      // do stuff
//    }
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
