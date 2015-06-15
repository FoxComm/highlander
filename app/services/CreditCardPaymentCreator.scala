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

  import CreditCardPaymentCreator._

  def run(): Response = {
    if (!cardPayload.isValid) {
      Future.successful(Bad(cardPayload.validationFailures.toList))
    } else {
      createStripeCustomer().flatMap { result =>
        FullCart.fromCart(cart).map { opt =>
          opt.map(Good(_)).getOrElse(Bad(List(s"could not render cart with id=${cart.id}")))
        }
      }
    }
  }

  def createStripeCustomer(): Future[StripeCustomer Or List[ErrorMessage]] = {
    val gateway = StripeGateway()
    gateway.createCustomer(customer, this.cardPayload).map { result =>
      result.fold({ stripeCustomer =>
        Good(stripeCustomer)
      }, { error =>
        Bad(List(error))
        // bad case
      })
    }
    // store stripeCustomer.getId to customer (maybe as JSON data?)
    // Create card on their behalf in stripe —> https://stripe.com/docs/api#cards
    // StripeCard.create
    // store card token to tokenized_credit_cards
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
