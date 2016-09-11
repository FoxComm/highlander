package services

import java.time.Instant

import scala.concurrent.Future

import cats.data.Xor
import cats.implicits._
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.GiftCardFailures.CreditCardMustHaveAddress
import failures.{Failures, NotFoundFailure404}
import models.account.User
import models.cord.OrderPayments.scope._
import models.cord._
import models.customer._
import models.location._
import models.payment.creditcard.{CreditCard, CreditCards}
import payloads.AddressPayloads.CreateAddressPayload
import payloads.PaymentPayloads._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.aliases.stripe._
import utils.apis.Apis
import utils.db._

object CreditCardManager {
  private def gateway(implicit ec: EC, apis: Apis): Stripe = Stripe()

  type Root = responses.CreditCardsResponse.Root

  def buildResponse(card: CreditCard, region: Region): Root =
    responses.CreditCardsResponse.build(card, region)

  def buildResponses(records: Seq[(CreditCard, Region)]): Seq[Root] =
    records.map((buildResponse _).tupled)

  def createCardThroughGateway(accountId: Int,
                               payload: CreateCreditCard,
                               admin: Option[User] = None)(implicit ec: EC,
                                                                 db: DB,
                                                                 apis: Apis,
                                                                 ac: AC): DbResultT[Root] = {

    def createCard(customer: User,
                   sCustomer: StripeCustomer,
                   sCard: StripeCard,
                   address: Address) =
      for {
        _ ← * <~ (if (address.isNew) Addresses.create(address.copy(accountId = accountId))
                  else DbResultT.unit)
        cc = CreditCard.build(accountId, sCustomer, sCard, payload, address)
        newCard ← * <~ CreditCards.create(cc)
        region  ← * <~ Regions.findOneById(newCard.regionId).safeGet
        _       ← * <~ LogActivity.ccCreated(customer, cc, admin)
      } yield buildResponse(newCard, region)

    def getExistingStripeIdAndAddress =
      for {
        stripeId ← * <~ CreditCards
                    .filter(_.accountId === accountId)
                    .map(_.gatewayAccountId)
                    .one
        shippingAddress ← * <~ getOptionalShippingAddress(payload.addressId, payload.isShipping)
        address ← * <~ getAddressFromPayload(payload.addressId, payload.address, shippingAddress)
                   .mustFindOr(CreditCardMustHaveAddress)
        _ ← * <~ validateOptionalAddressOwnership(Some(address), accountId)
      } yield (stripeId, address)

    for {
      _                  ← * <~ payload.validate
      customer           ← * <~ Users.mustFindByAccountId(accountId)
      stripeIdAndAddress ← * <~ getExistingStripeIdAndAddress
      (stripeId, address) = stripeIdAndAddress
      stripeStuff ← * <~ DBIO.from(gateway.createCard(customer.email, payload, stripeId, address))
      (stripeCustomer, stripeCard) = stripeStuff
      newCard ← * <~ createCard(customer, stripeCustomer, stripeCard, address)
    } yield newCard
  }

  def toggleCreditCardDefault(accountId: Int, cardId: Int, isDefault: Boolean)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      _  ← * <~ CreditCards.findDefaultByAccountId(accountId).map(_.isDefault).update(false)
      cc ← * <~ CreditCards.mustFindByIdAndCustomer(cardId, accountId)
      default = cc.copy(isDefault = true)
      _      ← * <~ CreditCards.filter(_.id === cardId).map(_.isDefault).update(true)
      region ← * <~ Regions.findOneById(cc.regionId).safeGet
    } yield buildResponse(default, region)

  def deleteCreditCard(accountId: Int, id: Int, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      apis: Apis,
      ac: AC): DbResultT[Unit] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      cc       ← * <~ CreditCards.mustFindByIdAndCustomer(id, accountId)
      region   ← * <~ Regions.findOneById(cc.regionId).safeGet
      update ← * <~ CreditCards.update(cc,
                                       cc.copy(inWallet = false, deletedAt = Some(Instant.now())))
      _ ← * <~ gateway.deleteCard(cc)
      _ ← * <~ LogActivity.ccDeleted(customer, cc, admin)
    } yield ()

  def editCreditCard(accountId: Int,
                     id: Int,
                     payload: EditCreditCard,
                     admin: Option[User] = None)(implicit ec: EC,
                                                       db: DB,
                                                       apis: Apis,
                                                       ac: AC): DbResultT[Root] = {

    def update(customer: User, cc: CreditCard) = {
      val updated = cc.copy(
          parentId = Some(cc.id),
          holderName = payload.holderName.getOrElse(cc.holderName),
          expYear = payload.expYear.getOrElse(cc.expYear),
          expMonth = payload.expMonth.getOrElse(cc.expMonth)
      )
      for {
        _ ← * <~ DBIO.from(gateway.editCard(updated))
        _ ← * <~ (if (!cc.inWallet) DbResultT.failure(CannotUseInactiveCreditCard(cc))
                  else DbResultT.unit)
        _  ← * <~ CreditCards.update(cc, cc.copy(inWallet = false))
        cc ← * <~ CreditCards.create(updated)
        _  ← * <~ LogActivity.ccUpdated(customer, updated, cc, admin)
      } yield cc
    }

    def createNewAddressIfProvided(cc: CreditCard) =
      payload.address.fold(DbResultT.good(cc)) { _ ⇒
        for {
          address ← * <~ Addresses.create(Address.fromCreditCard(cc).copy(accountId = accountId))
        } yield cc
      }

    def cascadeChangesToCarts(updated: CreditCard) = {
      val paymentIds = for {
        carts ← Carts.findByAccountId(accountId)
        pmts  ← OrderPayments.filter(_.paymentMethodId === updated.parentId).creditCards
        if pmts.cordRef === carts.referenceNumber
      } yield pmts.id

      for {
        cc ← OrderPayments
              .filter(_.id.in(paymentIds))
              .map(_.paymentMethodId)
              .update(updated.id)
              .map(_ ⇒ updated)
        region ← Regions.findOneById(cc.regionId).safeGet
      } yield buildResponse(cc, region)
    }

    val getCardAndAddressChange = for {
      creditCard ← * <~ CreditCards
                    .findById(id)
                    .extract
                    .filter(_.accountId === accountId)
                    .mustFindOneOr(NotFoundFailure404(CreditCard, id))
      shippingAddress ← * <~ getOptionalShippingAddress(payload.addressId, payload.isShipping)
      address         ← * <~ getAddressFromPayload(payload.addressId, payload.address, shippingAddress)
      _               ← * <~ validateOptionalAddressOwnership(address, accountId)
    } yield address.fold(creditCard)(creditCard.copyFromAddress)

    for {
      _           ← * <~ payload.validate
      customer    ← * <~ Users.mustFindByAccountId(accountId)
      creditCard  ← * <~ getCardAndAddressChange
      updated     ← * <~ update(customer, creditCard)
      withAddress ← * <~ createNewAddressIfProvided(updated)
      payment     ← * <~ cascadeChangesToCarts(withAddress)
    } yield payment
  }

  def creditCardsInWalletFor(accountId: Int)(implicit ec: EC, db: DB): Future[Seq[Root]] =
    (for {
      cc     ← CreditCards.findInWalletByAccountId(accountId)
      region ← cc.region
    } yield (cc, region)).result.map(buildResponses).run()

  def getByIdAndCustomer(creditCardId: Int, customer: User)(implicit ec: EC,
                                                                db: DB): DbResultT[Root] =
    for {
      cc ← * <~ CreditCards
            .findByIdAndAccountId(creditCardId, customer.accountId)
            .mustFindOneOr(NotFoundFailure404(CreditCard, creditCardId))
      region ← * <~ Regions.mustFindById404(cc.regionId)
    } yield buildResponse(cc, region)

  private def validateOptionalAddressOwnership(address: Option[Address],
                                               accountId: Int): Failures Xor Unit = {
    address match {
      case Some(a) ⇒ a.mustBelongToAccount(accountId).map(_ ⇒ Unit)
      case _       ⇒ Xor.Right(Unit)
    }
  }

  private def getAddressFromPayload(
      id: Option[Int],
      payload: Option[CreateAddressPayload],
      shippingAddress: Option[OrderShippingAddress]): DBIO[Option[Address]] = {

    (shippingAddress, id, payload) match {
      case (Some(osa), _, _) ⇒
        DBIO.successful(Address.fromOrderShippingAddress(osa).some)

      case (None, Some(addressId), _) ⇒
        Addresses.findById(addressId).extract.one

      case (None, _, Some(createAddress)) ⇒
        DBIO.successful(Address.fromPayload(createAddress).some)

      case _ ⇒
        DBIO.successful(None)
    }
  }

  private def getOptionalShippingAddress(id: Option[Int],
                                         isShipping: Boolean): DBIO[Option[OrderShippingAddress]] =
    id match {
      case Some(addressId) if isShipping ⇒ OrderShippingAddresses.findById(addressId).extract.one
      case _                             ⇒ DBIO.successful(None)
    }
}
