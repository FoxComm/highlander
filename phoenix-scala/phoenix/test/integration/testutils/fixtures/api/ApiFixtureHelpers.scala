package testutils.fixtures.api

import cats.implicits._
import faker.Internet.{email => randomEmail}
import faker.Name.{name => randomName}
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.payloads.PaymentPayloads.{CreateCreditCardFromTokenPayload, CreateManualStoreCredit}
import phoenix.responses.cord.CartResponse
import phoenix.responses.{CreditCardsResponse, CustomerResponse, GiftCardResponse, StoreCreditResponse}
import phoenix.utils.aliases._
import testutils._
import testutils.apis.PhoenixAdminApi

trait ApiFixtureHelpers extends PhoenixAdminApi with ApiFixtures { self: FoxSuite ⇒
  def api_newCustomer()(implicit sl: SL, sf: SF): CustomerResponse.Root = {
    val name = randomName
    customersApi
      .create(CreateCustomerPayload(name = name.some, email = randomEmail(name)))(defaultAdminAuth)
      .as[CustomerResponse.Root]
  }

  def api_newCustomerWithLogin()(implicit sl: SL, sf: SF): (CustomerResponse.Root, TestLoginData) = {
    val name      = randomName
    val loginData = TestLoginData(randomEmail(name))
    val customer = customersApi
      .create(
          CreateCustomerPayload(name = name.some,
                                email = loginData.email,
                                password = loginData.password.some))(defaultAdminAuth)
      .as[CustomerResponse.Root]
    (customer, loginData)
  }

  def api_newGuestCart()(implicit sl: SL, sf: SF): CartResponse =
    cartsApi.create(CreateCart(email = randomEmail.some))(defaultAdminAuth).as[CartResponse]

  def api_newCustomerCart(customerId: Int)(implicit sl: SL, sf: SF): CartResponse =
    cartsApi.create(CreateCart(customerId = customerId.some))(defaultAdminAuth).as[CartResponse]

  def api_newCreditCard(customerId: Int, payload: CreateCreditCardFromTokenPayload)(
      implicit sl: SL,
      sf: SF): CreditCardsResponse.Root =
    customersApi(customerId).payments.creditCards
      .create(payload)(defaultAdminAuth)
      .as[CreditCardsResponse.Root]

  def api_newGiftCard(payload: GiftCardCreateByCsr)(implicit sl: SL,
                                                    sf: SF): GiftCardResponse.Root =
    giftCardsApi.create(payload)(defaultAdminAuth).as[GiftCardResponse.Root]

  def api_newStoreCredit(customerId: Int, payload: CreateManualStoreCredit)(
      implicit sl: SL,
      sf: SF): StoreCreditResponse.Root =
    customersApi(customerId).payments.storeCredit
      .create(payload)(defaultAdminAuth)
      .as[StoreCreditResponse.Root]
}
