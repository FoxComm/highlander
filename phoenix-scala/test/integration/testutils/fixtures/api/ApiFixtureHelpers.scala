package testutils.fixtures.api

import cats.implicits._
import faker.Internet.{email ⇒ randomEmail}
import faker.Name.{name ⇒ randomName}
import payloads.CartPayloads.CreateCart
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import payloads.PaymentPayloads.{CreateCreditCardFromTokenPayload, CreateManualStoreCredit}
import responses.{CreditCardsResponse, CustomerResponse, GiftCardResponse, StoreCreditResponse}
import responses.cord.CartResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import utils.aliases._

trait ApiFixtureHelpers extends PhoenixAdminApi with ApiFixtures { self: FoxSuite ⇒

  def api_newCustomer()(implicit sl: SL, sf: SF): CustomerResponse.Root = {
    val name = randomName
    customersApi
      .create(CreateCustomerPayload(name = name.some, email = randomEmail(name)))
      .as[CustomerResponse.Root]
  }

  def api_newGuestCart()(implicit sl: SL, sf: SF): CartResponse =
    cartsApi.create(CreateCart(email = randomEmail.some)).as[CartResponse]

  def api_newCustomerCart(customerId: Int)(implicit sl: SL, sf: SF): CartResponse =
    cartsApi.create(CreateCart(customerId = customerId.some)).as[CartResponse]

  def api_newCreditCard(customerId: Int, payload: CreateCreditCardFromTokenPayload)(
      implicit sl: SL,
      sf: SF): CreditCardsResponse.Root =
    customersApi(customerId).payments.creditCards.create(payload).as[CreditCardsResponse.Root]

  def api_newGiftCard(payload: GiftCardCreateByCsr)(implicit sl: SL,
                                                    sf: SF): GiftCardResponse.Root =
    giftCardsApi.create(payload).as[GiftCardResponse.Root]

  def api_newStoreCredit(customerId: Int, payload: CreateManualStoreCredit)(
      implicit sl: SL,
      sf: SF): StoreCreditResponse.Root =
    customersApi(customerId).payments.storeCredit.create(payload).as[StoreCreditResponse.Root]
}
