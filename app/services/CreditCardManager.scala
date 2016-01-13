package services

import java.time.Instant

import cats.implicits._
import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import models.OrderPayments.scope._
import models.Orders.scope._
import models.activity.ActivityContext
import models.{Address, Addresses, CreditCard, CreditCards, Customer, Customers, OrderPayments, Orders, Region, Regions, StoreAdmin}
import payloads.{CreateAddressPayload, CreateCreditCard, EditCreditCard}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

object CreditCardManager {
  private def gateway(implicit ec: ExecutionContext, apis: Apis): Stripe = Stripe()

  type Root = responses.CreditCardsResponse.Root

  def buildResponse(card: CreditCard, region: Region): Root =
    responses.CreditCardsResponse.build(card, region)

  def buildResponses(records: Seq[(CreditCard, Region)]): Seq[Root] =
    records.map((buildResponse _).tupled)

  def createCardThroughGateway(admin: StoreAdmin, customerId: Int, payload: CreateCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis, ac: ActivityContext): Result[Root] = {

    def createCard(customer: Customer, sCustomer: StripeCustomer, sCard: StripeCard, address: Address) = ResultT((for {
      _       ← * <~ (if (address.isNew) Addresses.create(address.copy(customerId = customerId)) else DbResult.unit)
      cc = CreditCard.build(customerId, sCustomer, sCard, payload, address)
      newCard ← * <~ CreditCards.create(cc)
      region  ← * <~ Regions.findOneById(newCard.regionId).safeGet.toXor
      _       ← * <~ LogActivity.ccCreated(admin, customer, cc)
    } yield buildResponse(newCard, region)).runTxn())

    def getExistingStripeIdAndAddress = ResultT((for {
      stripeId ← * <~ CreditCards.filter(_.customerId === customerId).map(_.gatewayCustomerId).one.toXor
      address ← * <~ getAddressFromPayload(payload.addressId, payload.address).mustFindOr(CreditCardMustHaveAddress)
    } yield (stripeId, address)).runTxn())

    (for {
      _                  ← ResultT.fromXor(payload.validate.toXor)
      customer           ← ResultT(Customers.mustFindById(customerId).run())
      stripeIdAndAddress ← getExistingStripeIdAndAddress
      (stripeId, address) = stripeIdAndAddress
      stripeStuff        ← ResultT(gateway.createCard(customer.email, payload, stripeId, address))
      (stripeCustomer, stripeCard) = stripeStuff
      newCard            ← createCard(customer, stripeCustomer, stripeCard, address)
    } yield newCard).value

  }

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    _       ← * <~ CreditCards.findDefaultByCustomerId(customerId).map(_.isDefault).update(false)
    cc      ← * <~ CreditCards.mustFindByIdAndCustomer(cardId, customerId)
    // TODO: please fucking replace me with diffing update
    default = cc.copy(isDefault = true)
    _       ← * <~ CreditCards.filter(_.id === cardId).map(_.isDefault).update(true)
    region  ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
  } yield buildResponse(default, region)).runTxn()

  def deleteCreditCard(admin: StoreAdmin, customerId: Int, id: Int)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Unit] = {

    (for {
      customer  ← * <~ Customers.mustFindById(customerId)
      cc        ← * <~ CreditCards.mustFindByIdAndCustomer(id, customerId)
      region    ← * <~ Regions.findOneById(cc.regionId).safeGet.toXor
      update    ← * <~ CreditCards.update(cc, cc.copy(inWallet = false, deletedAt = Some(Instant.now())))
      _         ← * <~ LogActivity.ccDeleted(admin, customer, cc)
    } yield ()).runTxn()
  }

  def editCreditCard(admin: StoreAdmin, customerId: Int, id: Int, payload: EditCreditCard)
    (implicit ec: ExecutionContext, db: Database, apis: Apis, ac: ActivityContext): Result[Root] = {

    def update(customer: Customer, cc: CreditCard) = {
      val updated = cc.copy(
        parentId = Some(cc.id),
        holderName = payload.holderName.getOrElse(cc.holderName),
        expYear = payload.expYear.getOrElse(cc.expYear),
        expMonth = payload.expMonth.getOrElse(cc.expMonth)
      )
      for {
        _ ← ResultT(gateway.editCard(updated))

        cc ← ResultT((for {
          _ ← * <~ (if (!cc.inWallet) DbResult.failure(CannotUseInactiveCreditCard(cc)) else DbResult.unit)
          _ ← * <~ CreditCards.update(cc, cc.copy(inWallet = false))
          cc ← * <~ CreditCards.create(updated)
          _ ← * <~ LogActivity.ccUpdated(admin, customer, updated, cc)
        } yield cc).runTxn())
      } yield cc
    }

    def createNewAddressIfProvided(cc: CreditCard) = ResultT(payload.address.fold(DbResult.good(cc))(_ ⇒ (for {
      address ← * <~ Addresses.create(Address.fromCreditCard(cc).copy(customerId = customerId))
    } yield cc).value).run())

    def cascadeChangesToCarts(updated: CreditCard) = {
      val paymentIds = for {
        orders ← Orders.findByCustomerId(customerId).cartOnly
        pmts ← OrderPayments.filter(_.paymentMethodId === updated.parentId).creditCards if pmts.orderId === orders.id
      } yield pmts.id

      ResultT.right((for {
        cc ← OrderPayments.filter(_.id.in(paymentIds)).map(_.paymentMethodId).update(updated.id).map(_ ⇒ updated)
        region ← Regions.findOneById(cc.regionId).safeGet
      } yield buildResponse(cc, region)).run())
    }

    val getCardAndAddressChange = ResultT((for {
      creditCard ← * <~ CreditCards.findById(id).extract.filter(_.customerId === customerId).one
        .mustFindOr(NotFoundFailure404(CreditCard, id))
      address ← * <~ getAddressFromPayload(payload.addressId, payload.address).toXor
    } yield address.fold(creditCard)(creditCard.copyFromAddress)).runTxn())

    (for {
      _           ← ResultT.fromXor(payload.validate.toXor)
      customer    ← ResultT(Customers.mustFindById(customerId).run())
      creditCard  ← getCardAndAddressChange
      updated     ← update(customer, creditCard)
      withAddress ← createNewAddressIfProvided(updated)
      payment     ← cascadeChangesToCarts(withAddress)
    } yield payment).value
  }

  def creditCardsInWalletFor(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[Root]] = (for {
    cc      ← CreditCards.findInWalletByCustomerId(customerId)
    region  ← cc.region
  } yield (cc, region)).result.map(buildResponses).run()

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

}
