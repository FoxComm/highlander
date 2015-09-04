package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import models.{Customer, Addresses, Address, CreditCard, CreditCards}
import payloads.{CreateAddressPayload, CreateCreditCard}
import slick.driver.PostgresDriver.api._
import utils.Validation.Result.Success
import utils.{Validation ⇒ validation}

object CreditCardManager {
  val gateway = StripeGateway()

  def createCardThroughGateway(customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Result[CreditCard] = {

    payload.validate match {
      case failure@validation.Result.Failure(violations) ⇒
        Result.failure(ValidationFailure(failure))

      case Success ⇒
        // creates the customer, card, and gives us getDefaultCard as the token
        gateway.createCustomerAndCard(customer, payload).flatMap {
          case Xor.Right((sCust, sCard))  ⇒ createRecords(sCust, sCard, customer, payload)
          case left@Xor.Left(errors)      ⇒ Future.successful(left)
        }
    }
  }

  private def createRecords(stripeCustomer: StripeCustomer, stripeCard: StripeCard,
    customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database): Result[CreditCard] = {

    def copyAddressToNewCard(addressId: Int) = {
      Addresses._findById(addressId).extract.filter(_.customerId === customer.id).result.headOption.flatMap {
        case None ⇒
          DBIO.successful(Xor.left(NotFoundFailure(Address, addressId).single))

        case Some(address) ⇒
          val cc = CreditCard.build(customer.id, stripeCustomer, stripeCard, payload, address)
          CreditCards.save(cc).map(Xor.right)
      }
    }

    def createAddressAndCard(cap: CreateAddressPayload) = {
      val newAddress = Address.fromPayload(cap)

      (for {
        address ← Addresses.save(newAddress)
        cc ← CreditCards.save(CreditCard.build(customer.id, stripeCustomer, stripeCard, payload, newAddress))
      } yield cc).map(Xor.right)
    }

    val savedCard = (payload.address, payload.addressId) match {
      case (_, Some(addressId)) ⇒ copyAddressToNewCard(addressId)
      case (Some(cap), _)       ⇒ createAddressAndCard(cap)
      case (None, None)         ⇒ DBIO.successful(Xor.left(CreditCardMustHaveAddress.single))
    }

    db.run(savedCard.transactionally)
  }
}


