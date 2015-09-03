package services

import cats.data.Xor
import models._

import responses.FullOrder
import payloads.{CreateAddressPayload, CreateCreditCard}


import scala.concurrent.{Future, ExecutionContext}
import slick.dbio.Effect.All
import slick.driver.PostgresDriver.api._
import com.stripe.model.{Token, Card => StripeCard, Customer => StripeCustomer}
import com.stripe.net.{RequestOptions => StripeRequestOptions}
import collection.JavaConversions.mapAsJavaMap

import slick.profile.FixedSqlAction
import utils.Validation
import utils.Validation.Result.{ Success}
import utils.{ Validation ⇒ validation }

// TODO(yax): make this abstract to handle multiple Gateways
final case class CreditCardPaymentCreator(order: Order, customer: Customer, cardPayload: CreateCreditCard)
  (implicit ec: ExecutionContext, db: Database) {

  val gateway = StripeGateway()
  import CreditCardPaymentCreator._

  def run(): Response = cardPayload.validate match {
    case failure @ validation.Result.Failure(violations) ⇒
      Result.failure(ValidationFailure(failure))
    case Success ⇒
      // creates the customer, card, and gives us getDefaultCard as the token
      gateway.createCustomerAndCard(customer, this.cardPayload).flatMap {
        case Xor.Right((stripeCustomer, stripeCard)) =>
          createRecords(stripeCustomer, stripeCard, order, customer)

        case left @ Xor.Left(errors) ⇒ Future.successful(left)
      }
  }

  private [this] def createRecords(stripeCustomer: StripeCustomer, stripeCard: StripeCard,
    order: Order, customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Result[CreditCard] = {

    val appliedPayment = OrderPayment.fromStripeCustomer(stripeCustomer, order)

    def copyAddressToNewCard(addressId: Int) = {
      Addresses._findById(addressId).extract.filter(_.customerId === customer.id).result.headOption.flatMap {
        case None ⇒
          DBIO.successful(Xor.left(NotFoundFailure(Address, addressId).single))

        case Some(address) ⇒
          val cc = CreditCard.build(stripeCustomer, stripeCard, this.cardPayload, address)
          CreditCards.save(cc).map(Xor.right)
      }
    }

    def createAddressAndCard(cap: CreateAddressPayload) = {
      val newAddress = Address.fromPayload(cap)

      (for {
        address ← Addresses.save(newAddress)
        cc ← CreditCards.save(CreditCard.build(stripeCustomer, stripeCard, this.cardPayload, newAddress))
      } yield cc).map(Xor.right)
    }

    val savedCard = (this.cardPayload.address, this.cardPayload.addressId) match {
      case (_, Some(addressId)) ⇒ copyAddressToNewCard(addressId)
      case (Some(cap), _)       ⇒ createAddressAndCard(cap)
      case (None, None)         ⇒ DBIO.successful(Xor.left(CreditCardMustHaveAddress.single))
    }

    db.run(savedCard.transactionally)
  }
}

object CreditCardPaymentCreator {
  type Response = Result[CreditCard]

  def run(order: Order, customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Response = {
    new CreditCardPaymentCreator(order, customer, payload).run()
  }
}

