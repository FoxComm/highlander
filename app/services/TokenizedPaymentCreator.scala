package services

import models._
import responses.FullCart

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import slick.driver.PostgresDriver.api._
import com.stripe.model.{Card => StripeCard}

case class TokenizedPaymentCreator(cart: Cart, customer: Customer, paymentToken: String)
                                  (implicit ec: ExecutionContext,
                                   db: Database) {

  import TokenizedPaymentCreator._
  val tokenCardsTable = TableQuery[TokenizedCreditCards]

  def run(): Response = {
    StripeGateway(paymentToken = paymentToken).getTokenizedCard.flatMap {
      case Good((card, stripeCard)) =>
        db.run(States._findByName(stripeCard.getAddressState)).flatMap { optState =>
          optState.map { s =>
            createRecords(card, stripeCard, s)
          }.getOrElse {
            Future.successful(Bad(One(s"could not find state with name=${stripeCard.getAddressState}")))
          }
        }

      case Bad(t) =>
        Future.successful(Bad(One(t.getMessage)))
    }
  }

  private [this] def createRecords(card: TokenizedCreditCard,
                                   stripeCard: StripeCard,
                                   state: State): Response = {

    val appliedPayment = AppliedPayment(cartId = cart.id, paymentMethodId = 1, // TODO: would do a lookup
      paymentMethodType = card.paymentGateway,
      appliedAmount = 0, status = Auth.toString, // TODO: use type and marshalling
      responseCode = "ok") // TODO: make this real

    val billingAddress = Address(customerId = customer.id, stateId = state.id, name = "Stripe",
      street1 = stripeCard.getAddressLine1, street2 = Option(stripeCard.getAddressLine2),
      city = stripeCard.getAddressCity, zip = stripeCard.getAddressZip)

    /*
      Create the TokenizedCreditCard, AppliedPayment, and billing Address (populated by the StripeCard)
     */
    val queries = for {
      tokenId <- tokenCardsTable.returning(tokenCardsTable.map(_.id)) += card.copy(customerId = customer.id)
      appliedPaymentId <- AppliedPayments.returningId += appliedPayment.copy(paymentMethodId = tokenId)
      addressId <- BillingAddresses._create(billingAddress, appliedPaymentId)
      c <- Carts._findById(cart.id).result.headOption
    } yield c

    db.run(queries.transactionally).flatMap { optCart =>
      optCart.map { c =>
        FullCart.fromCart(c).map { root =>
          root.map(Good(_)).getOrElse(Bad(One("could not render cart")))
        }
      }.getOrElse(Future.successful(Bad(One(s"could not find cart with id=${cart.id}"))))
    }
  }
}

object TokenizedPaymentCreator {
  type Response = Future[FullCart.Root Or One[ErrorMessage]]

  def run(cart: Cart, customer: Customer, paymentToken: String)
         (implicit ec: ExecutionContext,
          db: Database): Response = {
    new TokenizedPaymentCreator(cart, customer, paymentToken).run()
  }
}
