package phoenix.services

import java.time.Instant

import cats.implicits._
import core.db._
import core.failures.{Failures, NotFoundFailure404}
import phoenix.failures.CreditCardFailures.CannotUseInactiveCreditCard
import phoenix.failures.GiftCardFailures.CreditCardMustHaveAddress
import phoenix.models.account._
import phoenix.models.cord.OrderPayments.scope._
import phoenix.models.cord._
import phoenix.models.location._
import phoenix.models.payment.creditcard.{CreditCard, CreditCards}
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.PaymentPayloads._
import phoenix.responses.CreditCardsResponse
import phoenix.utils.aliases._
import phoenix.utils.aliases.stripe._
import phoenix.utils.apis.Apis
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

object CreditCardManager {

  type Root = CreditCardsResponse.Root

  def buildResponse(card: CreditCard, region: Region): Root =
    CreditCardsResponse.build(card, region)

  def buildResponses(records: Seq[(CreditCard, Region)]): Seq[Root] =
    records.map((buildResponse _).tupled)

  def createCardFromToken(
      accountId: Int,
      payload: CreateCreditCardFromTokenPayload,
      admin: Option[User] = None)(implicit ec: EC, db: DB, apis: Apis, ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ Regions.mustFindById400(payload.billingAddress.regionId)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      customerToken ← * <~ CreditCards
                       .filter(_.accountId === accountId)
                       .take(1)
                       .map(_.gatewayCustomerId)
                       .one
      address = Address.fromPayload(payload.billingAddress, customer.accountId)
      _ ← * <~ when(payload.addressIsNew, Addresses.create(address).void)
      stripes ← * <~ apis.stripe.createCardFromToken(email = customer.email,
                                                     token = payload.token,
                                                     stripeCustomerId = customerToken,
                                                     address = address)
      (stripeCustomer, stripeCard) = stripes
      cc ← * <~ CreditCards.create(
            CreditCard.buildFromToken(accountId = accountId,
                                      customerToken = stripeCustomer.getId,
                                      payload = payload,
                                      address = address,
                                      cardToken = stripeCard.getId))
      _        ← * <~ LogActivity().ccCreated(customer, cc, admin)
      response ← * <~ CreditCardsResponse.buildFromCreditCard(cc)
    } yield response

  @deprecated(message = "Use `createCardFromToken` instead", "Until we are PCI compliant")
  def createCardFromSource(
      accountId: Int,
      payload: CreateCreditCardFromSourcePayload,
      admin: Option[User] = None)(implicit ec: EC, db: DB, apis: Apis, ac: AC): DbResultT[Root] = {

    def createCard(customer: User, sCustomer: StripeCustomer, sCard: StripeCard, address: Address) =
      for {
        _ ← * <~ when(address.isNew, Addresses.create(address.copy(accountId = accountId)).void)
        cc = CreditCard.buildFromSource(accountId, sCustomer, sCard, payload, address)
        newCard ← * <~ CreditCards.create(cc)
        region  ← * <~ Regions.findOneById(newCard.address.regionId).safeGet
        _       ← * <~ LogActivity().ccCreated(customer, cc, admin)
      } yield buildResponse(newCard, region)

    def getExistingStripeIdAndAddress =
      for {
        stripeId        ← * <~ CreditCards.filter(_.accountId === accountId).map(_.gatewayCustomerId).one
        shippingAddress ← * <~ getOptionalShippingAddress(payload.addressId, payload.isShipping)
        address ← * <~ getAddressFromPayload(payload.addressId, payload.address, shippingAddress, accountId)
                   .mustFindOr(CreditCardMustHaveAddress)
        _ ← * <~ validateOptionalAddressOwnership(Some(address), accountId)
      } yield (stripeId, address)

    for {
      customer           ← * <~ Users.mustFindByAccountId(accountId)
      stripeIdAndAddress ← * <~ getExistingStripeIdAndAddress
      (stripeId, address) = stripeIdAndAddress
      stripeStuff ← * <~ apis.stripe
                     .createCardFromSource(customer.email, payload, stripeId, address)
      (stripeCustomer, stripeCard) = stripeStuff
      newCard ← * <~ createCard(customer, stripeCustomer, stripeCard, address)
    } yield newCard
  }

  def setDefaultCreditCard(accountId: Int, cardId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      _  ← * <~ removeDefaultCreditCard(accountId)
      cc ← * <~ CreditCards.mustFindByIdAndAccountId(cardId, accountId)
      default = cc.copy(isDefault = true)
      _      ← * <~ CreditCards.filter(_.id === cardId).map(_.isDefault).update(true)
      region ← * <~ Regions.findOneById(cc.address.regionId).safeGet
    } yield buildResponse(default, region)

  def removeDefaultCreditCard(accountId: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    CreditCards.findDefaultByAccountId(accountId).map(_.isDefault).update(false).dbresult.void

  def deleteCreditCard(accountId: Int, ccId: Int, admin: Option[User] = None)(implicit ec: EC,
                                                                              db: DB,
                                                                              apis: Apis,
                                                                              ac: AC): DbResultT[Unit] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      cc       ← * <~ CreditCards.mustFindByIdAndAccountId(ccId, accountId)
      _        ← * <~ CreditCards.update(cc, cc.copy(inWallet = false, deletedAt = Some(Instant.now())))
      _        ← * <~ apis.stripe.deleteCard(cc)
      _        ← * <~ LogActivity().ccDeleted(customer, cc, admin)
    } yield ()

  def editCreditCard(accountId: Int, id: Int, payload: EditCreditCard, admin: Option[User] = None)(
      implicit ec: EC,
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
        _  ← * <~ apis.stripe.editCard(updated)
        _  ← * <~ failIf(!cc.inWallet, CannotUseInactiveCreditCard(cc))
        _  ← * <~ CreditCards.update(cc, cc.copy(inWallet = false))
        cc ← * <~ CreditCards.create(updated)
        _  ← * <~ LogActivity().ccUpdated(customer, updated, cc, admin)
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
        region ← Regions.findOneById(cc.address.regionId).safeGet
      } yield buildResponse(cc, region)
    }

    val getCardAndAddressChange = for {
      creditCard ← * <~ CreditCards
                    .findById(id)
                    .extract
                    .filter(_.accountId === accountId)
                    .mustFindOneOr(NotFoundFailure404(CreditCard, id))
      shippingAddress ← * <~ getOptionalShippingAddress(payload.addressId, payload.isShipping)
      address         ← * <~ getAddressFromPayload(payload.addressId, payload.address, shippingAddress, accountId)
      _               ← * <~ validateOptionalAddressOwnership(address, accountId)
    } yield address.fold(creditCard)(creditCard.copyFromAddress)

    for {
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

  def getByIdAndCustomer(creditCardId: Int, customer: User)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      cc ← * <~ CreditCards
            .findByIdAndAccountId(creditCardId, customer.accountId)
            .mustFindOneOr(NotFoundFailure404(CreditCard, creditCardId))
      region ← * <~ Regions.mustFindById404(cc.address.regionId)
    } yield buildResponse(cc, region)

  private def validateOptionalAddressOwnership(address: Option[Address],
                                               accountId: Int): Either[Failures, Unit] =
    address match {
      case Some(a) ⇒ a.mustBelongToAccount(accountId).map(_ ⇒ Unit)
      case _       ⇒ Either.right(Unit)
    }

  private def getAddressFromPayload(id: Option[Int],
                                    payload: Option[CreateAddressPayload],
                                    shippingAddress: Option[OrderShippingAddress],
                                    accountId: Int): DBIO[Option[Address]] =
    (shippingAddress, id, payload) match {
      case (Some(osa), _, _) ⇒
        DBIO.successful(Address.fromOrderShippingAddress(osa).some)

      case (None, Some(addressId), _) ⇒
        Addresses.findById(addressId).extract.one

      case (None, _, Some(createAddress)) ⇒
        DBIO.successful(Address.fromPayload(createAddress, accountId).some)

      case _ ⇒
        DBIO.successful(None)
    }

  private def getOptionalShippingAddress(id: Option[Int],
                                         isShipping: Boolean): DBIO[Option[OrderShippingAddress]] =
    id match {
      case Some(addressId) if isShipping ⇒ OrderShippingAddresses.findById(addressId).extract.one
      case _                             ⇒ DBIO.successful(None)
    }
}
