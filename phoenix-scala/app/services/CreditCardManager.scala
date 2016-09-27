package services

import java.time.Instant

import scala.concurrent.Future

import cats.implicits._
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.StoreAdmin
import models.cord.OrderPayments.scope._
import models.cord._
import models.customer._
import models.location._
import models.payment.creditcard.{CreditCard, CreditCards}
import payloads.PaymentPayloads._
import responses.CreditCardsResponse
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.aliases.stripe._
import utils.apis.Apis
import utils.db._

object CreditCardManager {

  type Root = CreditCardsResponse.Root

  def buildResponse(card: CreditCard, region: Region): Root =
    CreditCardsResponse.build(card, region)

  def buildResponses(records: Seq[(CreditCard, Region)]): Seq[Root] =
    records.map((buildResponse _).tupled)

  def createCardFromToken(customerId: Int,
                          payload: CreateCreditCardFromTokenPayload,
                          admin: Option[StoreAdmin] = None)(implicit ec: EC,
                                                            db: DB,
                                                            apis: Apis,
                                                            ac: AC): DbResultT[Root] =
    createCreditCard(customerId = customerId,
                     payload = payload,
                     stripeCreateCardFn = apis.stripe.createCardFromToken,
                     buildCardFn = CreditCard.buildFromToken,
                     admin = admin)

  @deprecated(message = "Use `createCardFromToken` instead", "Until we are PCI compliant")
  def createCardFromSource(customerId: Int,
                           payload: CreateCreditCardFromSourcePayload,
                           admin: Option[StoreAdmin] = None)(implicit ec: EC,
                                                             db: DB,
                                                             apis: Apis,
                                                             ac: AC): DbResultT[Root] =
    createCreditCard(customerId = customerId,
                     payload = payload,
                     stripeCreateCardFn = apis.stripe.createCardFromSource,
                     buildCardFn = CreditCard.buildFromSource,
                     admin = admin)

  def toggleCreditCardDefault(customerId: Int, cardId: Int, isDefault: Boolean)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      _  ← * <~ CreditCards.findDefaultByCustomerId(customerId).map(_.isDefault).update(false)
      cc ← * <~ CreditCards.mustFindByIdAndCustomer(cardId, customerId)
      default = cc.copy(isDefault = true)
      _      ← * <~ CreditCards.filter(_.id === cardId).map(_.isDefault).update(true)
      region ← * <~ Regions.findOneById(cc.regionId).safeGet
    } yield buildResponse(default, region)

  def deleteCreditCard(customerId: Int, ccId: Int, admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      apis: Apis,
      ac: AC): DbResultT[Unit] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      cc       ← * <~ CreditCards.mustFindByIdAndCustomer(ccId, customerId)
      _        ← * <~ CreditCards.update(cc, cc.copy(inWallet = false, deletedAt = Some(Instant.now())))
      _        ← * <~ apis.stripe.deleteCard(cc)
      _        ← * <~ LogActivity.ccDeleted(customer, cc, admin)
    } yield ()

  def editCreditCard(customerId: Int,
                     id: Int,
                     payload: EditCreditCardPayload,
                     admin: Option[StoreAdmin] = None)(implicit ec: EC,
                                                       db: DB,
                                                       apis: Apis,
                                                       ac: AC): DbResultT[Root] = {
    def updateCard(customer: Customer, cc: CreditCard) = {
      val newVersion = {
        val ccWithoutAddressUpd = cc.copy(
            parentId = cc.id.some,
            holderName = payload.holderName.getOrElse(cc.holderName),
            expYear = payload.expYear.getOrElse(cc.expYear),
            expMonth = payload.expMonth.getOrElse(cc.expMonth)
        )

        payload.address.map { addressPayload ⇒
          ccWithoutAddressUpd.copyFromAddress(
              Address.fromPayload(addressPayload.address, customerId))
        }.getOrElse(ccWithoutAddressUpd)
      }

      for {
        _  ← * <~ failIf(!cc.inWallet, CannotUseInactiveCreditCard(cc))
        _  ← * <~ DBIO.from(apis.stripe.editCard(newVersion))
        _  ← * <~ CreditCards.update(cc, cc.copy(inWallet = false))
        cc ← * <~ CreditCards.create(newVersion)
        _  ← * <~ LogActivity.ccUpdated(customer, newVersion, cc, admin)
      } yield cc
    }

    def updateAddress() = {
      def updateExistingAddress(id: Int, newAddress: Address, customerId: Int) =
        for {
          oldAddress ← * <~ Addresses
                        .findByIdAndCustomer(id, customerId)
                        .mustFindOneOr(NotFoundFailure400(Address, id))
          _ ← * <~ Addresses.update(oldAddress, newAddress)
        } yield {}

      payload.address.fold(DbResultT.unit) { ccAddressPayload ⇒
        val newAddress = Address.fromPayload(ccAddressPayload.address, customerId)
        ccAddressPayload.id match {
          case Some(addressId) ⇒ updateExistingAddress(addressId, newAddress, customerId)
          case None            ⇒ Addresses.create(newAddress).meh
        }
      }
    }

    def updateCarts(updated: CreditCard) = {
      val paymentIds = for {
        carts ← Carts.findByCustomerId(customerId)
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

    for {
      _          ← * <~ payload.validate
      customer   ← * <~ Customers.mustFindById404(customerId)
      creditCard ← * <~ CreditCards.mustFindByIdAndCustomer(id, customerId)
      updatedCc  ← * <~ updateCard(customer, creditCard)
      _          ← * <~ updateAddress()
      response   ← * <~ updateCarts(updatedCc)
    } yield response
  }

  def creditCardsInWalletFor(customerId: Int)(implicit ec: EC, db: DB): Future[Seq[Root]] =
    (for {
      cc     ← CreditCards.findInWalletByCustomerId(customerId)
      region ← cc.region
    } yield (cc, region)).result.map(buildResponses).run()

  def getByIdAndCustomer(ccId: Int, customer: Customer)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      cc ← * <~ CreditCards
            .findByIdAndCustomerId(ccId, customer.id)
            .mustFindOneOr(NotFoundFailure404(CreditCard, ccId))
      region ← * <~ Regions.mustFindById404(cc.regionId)
    } yield buildResponse(cc, region)

  private def createCreditCard[Payload <: CreateCreditCardPayloadsBase[Payload]](
      customerId: Int,
      payload: Payload,
      stripeCreateCardFn: (Option[String], Payload, Option[String],
                           Address) ⇒ Result[(StripeCustomer, StripeCard)],
      buildCardFn: (Customer, StripeCustomer, StripeCard, Payload, Address) ⇒ CreditCard,
      admin: Option[StoreAdmin])(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      _        ← * <~ Regions.mustFindById400(payload.billingAddress.address.regionId)
      customer ← * <~ Customers.mustFindById404(customerId)
      customerToken ← * <~ CreditCards
                       .filter(_.customerId === customerId)
                       .take(1)
                       .map(_.gatewayCustomerId)
                       .one
      address = Address.fromPayload(payload.billingAddress.address, customer.id)
      _       ← * <~ (if (payload.billingAddress.isNew) Addresses.create(address) else DbResultT.unit)
      stripes ← * <~ stripeCreateCardFn(customer.email, payload, customerToken, address)
      (stripeCustomer, stripeCard) = stripes
      cc ← * <~ CreditCards.create(
              buildCardFn(customer, stripeCustomer, stripeCard, payload, address))
      _        ← * <~ LogActivity.ccCreated(customer, cc, admin)
      response ← * <~ CreditCardsResponse.buildFromCreditCard(cc)
    } yield response

}
