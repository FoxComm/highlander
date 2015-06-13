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
      Future.successful(Bad(cardPayload.validationFailures))
    } else {
      createStripeCustomer()
      Future.successful(Bad(Set("implement me!")))
    }
  }

  def createStripeCustomer(apiKey: String = "sk_test_eyVBk2Nd9bYbwl01yFsfdVLZ") = {
    val options = StripeRequestOptions.builder().setApiKey(apiKey).build()
    val params: Map[String, Object] = Map(
      "description" -> "FoxCommerce",
      "email" -> this.customer.email,
      "source" -> Map(
        "object" -> "card",
        "number" -> cardPayload.number,
        "exp_month" -> cardPayload.expMonth,
        "exp_year" -> cardPayload.expYear,
        "cvc" -> cardPayload.cvv,
        "name" -> cardPayload.holderName
        // TODO(yax): would like to add fields "address_line1", "address_city", "address_state", "address_zip"
      )
    )

    Future {
      try {
        val stripeCustomer = StripeCustomer.create(mapAsJavaMap(params), options)
        // store stripeCustomer.getId to customer (maybe as JSON data?)
        // Create card on their behalf in stripe —> https://stripe.com/docs/api#cards
        // store card token to tokenized_credit_cards
      } catch {
        case t: com.stripe.exception.InvalidRequestException =>
          Bad(t)
      }
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
