package services

import models._
import responses.FullCart
import payloads.CreditCardPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import slick.driver.PostgresDriver.api._
import com.stripe.model.{Card => StripeCard}

case class CreditCardPaymentCreator(cart: Cart, customer: Customer, payload: CreditCardPayload)
                                   (implicit ec: ExecutionContext,
                                   db: Database) {

  import CreditCardPaymentCreator._

  def run(): Response = {
    if (payload.isValid) {
      Future.successful(Bad(Set("implement me!")))
    } else {
      Future.successful(Bad(payload.validationFailures))
    }
  }
}

object CreditCardPaymentCreator {
  type Response = Future[FullCart.Root Or Set[ErrorMessage]]

  def run(cart: Cart, customer: Customer, payload: CreditCardPayload)
         (implicit ec: ExecutionContext,
          db: Database): Response = {
    new CreditCardPaymentCreator(cart, customer, payload).run()
  }
}
