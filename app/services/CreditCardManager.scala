package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import cats.implicits._

import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import models.OrderPayments.scope._
import models.Orders.scope._
import models.{Address, Addresses, CreditCard, CreditCards, Customer, OrderPayments, Orders}
import payloads.{CreateAddressPayload, CreateCreditCard, EditCreditCard}
import slick.dbio
import slick.dbio.Effect.{All, Read}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives.SortAndPage
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import utils.jdbc.withUniqueConstraint

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

object CreditCardManager {
  val gateway = StripeGateway()

  def createCardThroughGateway(customer: Customer, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis): Result[CreditCard] = {

    def saveCardAndAddress(stripeCustomer: StripeCustomer, stripeCard: StripeCard, address: Address):
    ResultT[DBIO[CreditCard]] = {
      val cc = CreditCard.build(customer.id, stripeCustomer, stripeCard, payload, address)
      val saveAddress = if (address.isNew)
        Addresses.save(address.copy(customerId = customer.id))
      else
        DBIO.successful(address)

      ResultT.rightAsync(saveAddress >> CreditCards.save(cc))
    }

    def getExistingStripeIdAndAddress: ResultT[(Option[String], Address)] = {
      val queries = for {
        stripeId  ← CreditCards.filter(_.customerId === customer.id).map(_.gatewayCustomerId).one
        address   ← getAddressFromPayload(payload.addressId, payload.address)
      } yield (stripeId, address)

      ResultT(queries.run().map {
        case (stripeId, Some(address))  ⇒ Xor.right((stripeId, address))
        case (_, None)                  ⇒ Xor.left(CreditCardMustHaveAddress.single)
      })
    }

    val transformer = for {
      _       ← ResultT.fromXor(payload.validate.toXor)
      res     ← getExistingStripeIdAndAddress
      res2    ← res match { case (sId, address) ⇒
        ResultT(gateway.createCard(customer.email, payload, sId, address))
      }
      newCard ← res2 match { case (sCustomer, sCard) ⇒
        saveCardAndAddress(sCustomer, sCard, res._2)
      }
    } yield newCard

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Result[CreditCard] = {
    val result = withUniqueConstraint {
      CreditCards.findById(cardId).extract.filter(_.customerId === customerId).map(_.isDefault).
        updateReturning(CreditCards.map(identity), isDefault).headOption.run()
    } { notUnique ⇒ CustomerHasDefaultCreditCard }

    result.flatMap {
      case Xor.Right(Some(cc)) ⇒ Result.good(cc)
      case Xor.Right(None)     ⇒ Result.failure(NotFoundFailure(CreditCard, cardId))
      case Xor.Left(f)         ⇒ Result.failure(f)
    }
  }

  def deleteCreditCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val updateCc = CreditCards.findById(id).extract
      .filter(_.customerId === customerId)
      .map { cc ⇒ (cc.inWallet, cc.deletedAt) }
      .update((false, Some(Instant.now())))

    db.run(updateCc.map { rows ⇒
      if (rows == 1) Xor.right(Unit) else Xor.left(creditCardNotFound(id))
    })
  }

  def editCreditCard(customerId: Int, id: Int, payload: EditCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis): Result[CreditCard] = {

    def update(cc: CreditCard): ResultT[DBIO[CreditCard]] = {
      if (!cc.inWallet)
        ResultT.leftAsync(CannotUseInactiveCreditCard(cc).single)
      else {
        val updated = cc.copy(
          parentId = Some(cc.id),
          holderName = payload.holderName.getOrElse(cc.holderName),
          expYear = payload.expYear.getOrElse(cc.expYear),
          expMonth = payload.expMonth.getOrElse(cc.expMonth)
        )

        val newVersion = CreditCards.save(updated)
        val deactivate = CreditCards.findById(cc.id).extract.map(_.inWallet).update(false)

        ResultT(new StripeGateway().editCard(updated).map {
          case Xor.Left(f)  ⇒ Xor.left(f)
          case Xor.Right(_) ⇒ Xor.right(deactivate >> newVersion)
        })
      }
    }

    def createNewAddressIfProvided(dbio: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = ResultT.rightAsync(
      if (payload.address.isDefined)
        dbio.flatMap { cc ⇒
          Addresses.save(Address.fromCreditCard(cc).copy(customerId = customerId)) >> DBIO.successful(cc)
        }
      else
        dbio
    )

    def cascadeChangesToCarts(edits: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = {
      val action = edits.flatMap { updated ⇒
        val paymentIds = for {
          orders ← Orders.findByCustomerId(customerId).cartOnly
          pmts ← OrderPayments.filter(_.paymentMethodId === updated.parentId).creditCards if pmts.orderId === orders.id
        } yield pmts.id

        OrderPayments.filter(_.id in paymentIds).map(_.paymentMethodId).update(updated.id).map(_ ⇒ updated)
      }

      ResultT.rightAsync(action)
    }

    val getCardAndAddressChange: ResultT[CreditCard] = {
      val queries = for {
        creditCard  ← getCard(customerId, id)
        address     ← getAddressFromPayload(payload.addressId, payload.address)
      } yield (creditCard, address)

      ResultT(queries.run().map {
        case (Some(cc), address)  ⇒ Xor.right(address.fold(cc)(cc.copyFromAddress))
        case (None, _)            ⇒ Xor.left(creditCardNotFound(id))
      })
    }

    val transformer: ResultT[DBIO[CreditCard]] = for {
      _       ← ResultT.fromXor(payload.validate.toXor)
      cc      ← getCardAndAddressChange
      updated ← update(cc)
      withAddress ← createNewAddressIfProvided(updated)
      payment ← cascadeChangesToCarts(withAddress)
    } yield payment

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  def creditCardsInWalletFor(customerId: Int)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Future[Seq[CreditCard]] = {
    val query = CreditCards.findInWalletByCustomerId(customerId)

    val sortedQuery = sortAndPage.sort match {
      case Some(s) if s.sortColumn == "expDate" ⇒ query.sortBy { creditCard ⇒
        if (s.asc) (creditCard.expYear.asc, creditCard.expMonth.asc)
        else (creditCard.expYear.desc, creditCard.expMonth.desc)
      }
      case Some(s)                              ⇒ query.sortBy { creditCard ⇒
        s.sortColumn match {
          case "id"                  => if(s.asc) creditCard.id.asc                else creditCard.id.desc
          case "parentId"            => if(s.asc) creditCard.parentId.asc          else creditCard.parentId.desc
          case "gatewayCustomerId"   => if(s.asc) creditCard.gatewayCustomerId.asc else creditCard.gatewayCustomerId.desc
          case "gatewayCardId"       => if(s.asc) creditCard.gatewayCardId.asc     else creditCard.gatewayCardId.desc
          case "holderName"          => if(s.asc) creditCard.holderName.asc        else creditCard.holderName.desc
          case "lastFour"            => if(s.asc) creditCard.lastFour.asc          else creditCard.lastFour.desc
          case "isDefault"           => if(s.asc) creditCard.isDefault.asc         else creditCard.isDefault.desc
          case "address1Check"       => if(s.asc) creditCard.address1Check.asc     else creditCard.address1Check.desc
          case "zipCheck"            => if(s.asc) creditCard.zipCheck.asc          else creditCard.zipCheck.desc
          case "inWallet"            => if(s.asc) creditCard.inWallet.asc          else creditCard.inWallet.desc
          case "deletedAt"           => if(s.asc) creditCard.deletedAt.asc         else creditCard.deletedAt.desc
          case "regionId"            => if(s.asc) creditCard.regionId.asc          else creditCard.regionId.desc
          case "addressName"         => if(s.asc) creditCard.addressName.asc       else creditCard.addressName.desc
          case "address1"            => if(s.asc) creditCard.address1.asc          else creditCard.address1.desc
          case "address2"            => if(s.asc) creditCard.address2.asc          else creditCard.address2.desc
          case "city"                => if(s.asc) creditCard.city.asc              else creditCard.city.desc
          case "zip"                 => if(s.asc) creditCard.zip.asc               else creditCard.zip.desc
          case _                     => creditCard.id.asc
        }
      }
      case None    ⇒ query
    }

    sortedQuery.paged.result.run()
  }

  def getCard(customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database): DBIO[Option[CreditCard]] =
    CreditCards.findById(id).extract.filter(_.customerId === customerId).one

  private def getAddressFromPayload(id: Option[Int], payload: Option[CreateAddressPayload]): DBIO[Option[Address]] = {
    (id, payload) match {
      case (Some(addressId), _) ⇒
        Addresses.findById(addressId).extract.one

      case (_, Some(createAddress)) ⇒
        DBIO.successful(Address.fromPayload(createAddress).some)

      case _ ⇒
        DBIO.successful(None)
    }
  }

  private def creditCardNotFound(id: Int) = NotFoundFailure(CreditCard, id).single
}


