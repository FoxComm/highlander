package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.implicits._

import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import models.OrderPayments.scope._
import models.Orders.scope._
import models.{Region, Regions, Address, Addresses, CreditCard, CreditCards, Customer, OrderPayments, Orders,
StoreAdmin, Customers}
import payloads.{CreateAddressPayload, CreateCreditCard, EditCreditCard}

import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import utils.jdbc._

import utils.DbResultT._
import utils.DbResultT.implicits._

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

import models.activity.ActivityContext

object CreditCardManager {
  private def gateway(implicit ec: ExecutionContext, apis: Apis): Stripe = Stripe()

  type Root = responses.CreditCardsResponse.Root

  def buildResponse(card: CreditCard, region: Region): Root =
    responses.CreditCardsResponse.build(card, region)

  def buildResponses(records: Seq[(CreditCard, Region)]): Seq[Root] =
    records.map((buildResponse _).tupled)

  def createCardThroughGateway(admin: StoreAdmin, customerId: Int, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis, ac: ActivityContext): Result[Root] = {

    def saveCardAndAddress(customer: Customer, stripeCustomer: StripeCustomer, stripeCard: StripeCard, address: Address):
    ResultT[DBIO[CreditCard]] = {
      val cc = CreditCard.build(customerId, stripeCustomer, stripeCard, payload, address)
      val saveAddress = if (address.isNew)
        Addresses.saveNew(address.copy(customerId = customerId))
      else
        DBIO.successful(address)

      val logActivity = LogActivity.ccCreated(admin, customer, cc)
      ResultT.rightAsync(logActivity >> saveAddress >> CreditCards.saveNew(cc))
    }

    def getExistingStripeIdAndAddress: ResultT[(Option[String], Address)] = {
      val queries = for {
        stripeId  ← CreditCards.filter(_.customerId === customerId).map(_.gatewayCustomerId).one
        address   ← getAddressFromPayload(payload.addressId, payload.address)
      } yield (stripeId, address)

      ResultT(queries.run().map {
        case (stripeId, Some(address))  ⇒ Xor.right((stripeId, address))
        case (_, None)                  ⇒ Xor.left(CreditCardMustHaveAddress.single)
      })
    }

    def processCreditCardCreation(customer: Customer) = {
      val transformer = for {
        _               ← ResultT.fromXor(payload.validate.toXor)
        res             ← getExistingStripeIdAndAddress
        res2            ← res match { case (sId, address) ⇒
          ResultT(gateway.createCard(customer.email, payload, sId, address))
        }
        newCard         ← res2 match { case (sCustomer, sCard) ⇒
          saveCardAndAddress(customer, sCustomer, sCard, res._2)
        }
      } yield newCard

      transformer.value.flatMap { res ⇒
        res.fold(
          Result.left,
          _.flatMap { newCard ⇒
            DBIO.successful(newCard).zip(Regions.findOneById(newCard.regionId).safeGet)
          }.transactionally.run().flatMap { case (cc, r) ⇒ Result.good(buildResponse(cc, r)) }
        )
      }
    }

    Customers.findById(customerId).result.headOption.run().flatMap {
      case Some(customer) ⇒ processCreditCardCreation(customer)
      case _              ⇒ Result.failure(NotFoundFailure404(Customer, customerId))
    }
  }

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    _       ← * <~ CreditCards.findDefaultByCustomerId(customerId).map(_.isDefault).update(false)
    cc      ← * <~ CreditCards.mustFindByIdAndCustomer(cardId, customerId)
    // TODO: please fucking replace me with diffing update
    default = cc.copy(isDefault = true)
    _       ← * <~ CreditCards.filter(_.id === cardId).map(_.isDefault).update(true)
    region  ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
  } yield buildResponse(default, region)).runT()

  def deleteCreditCard(admin: StoreAdmin, customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Unit] = {

    (for {
      customer  ← * <~ Customers.mustFindById(customerId)
      cc        ← * <~ CreditCards.mustFindByIdAndCustomer(id, customerId)
      region    ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
      update    ← * <~ CreditCards.update(cc, cc.copy(inWallet = false, deletedAt = Some(Instant.now())))
      _         ← * <~ LogActivity.ccDeleted(admin, customer, cc)
    } yield ()).runT()
  }

  def editCreditCard(admin: StoreAdmin, customerId: Int, id: Int, payload: EditCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis, ac: ActivityContext): Result[Root] = {

    def update(customer: Customer, cc: CreditCard): ResultT[DBIO[CreditCard]] = {
      if (!cc.inWallet)
        ResultT.leftAsync(CannotUseInactiveCreditCard(cc).single)
      else {
        val updated = cc.copy(
          parentId = Some(cc.id),
          holderName = payload.holderName.getOrElse(cc.holderName),
          expYear = payload.expYear.getOrElse(cc.expYear),
          expMonth = payload.expMonth.getOrElse(cc.expMonth)
        )

        val newVersion  = CreditCards.saveNew(updated)
        val deactivate  = CreditCards.findById(cc.id).extract.map(_.inWallet).update(false)
        val logActivity = LogActivity.ccUpdated(admin, customer, updated, cc)
        val operations  = logActivity >> deactivate >> newVersion

        ResultT(gateway.editCard(updated).map(xor ⇒ xor.map(_ ⇒ operations)))
      }
    }

    def createNewAddressIfProvided(dbio: DBIO[CreditCard]): ResultT[DBIO[CreditCard]] = ResultT.rightAsync(
      if (payload.address.isDefined)
        dbio.flatMap { cc ⇒
          Addresses.saveNew(Address.fromCreditCard(cc).copy(customerId = customerId)) >> DBIO.successful(cc)
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

    def processCreditCardUpdate(customer: Customer) = {
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
        _             ← ResultT.fromXor(payload.validate.toXor)
        cc            ← getCardAndAddressChange
        updated       ← update(customer, cc)
        withAddress   ← createNewAddressIfProvided(updated)
        payment       ← cascadeChangesToCarts(withAddress)
      } yield payment

      transformer.value.flatMap { res ⇒
        res.fold(
          Result.left,
          _.flatMap { newCard ⇒
              DBIO.successful(newCard).zip(Regions.findOneById(newCard.regionId).safeGet)
          }.transactionally.run().flatMap { case (cc, r) ⇒ Result.good(buildResponse(cc, r)) }
        )
      }
    }

    Customers.findById(customerId).result.headOption.run().flatMap {
      case Some(customer) ⇒ processCreditCardUpdate(customer)
      case _              ⇒ Result.failure(NotFoundFailure404(Customer, customerId))
    }
  }

  def creditCardsInWalletFor(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[Root]] = (for {
    cc      ← CreditCards.findInWalletByCustomerId(customerId)
    region  ← cc.region
  } yield (cc, region)).result.map(buildResponses).run()

  private def getCard(customerId: Int, id: Int)
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

  private def creditCardNotFound(id: Int) = NotFoundFailure404(CreditCard, id).single
}
