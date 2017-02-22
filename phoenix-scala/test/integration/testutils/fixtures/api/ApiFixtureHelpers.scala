package testutils.fixtures.api

import cats.implicits._
import faker.Internet.{email ⇒ randomEmail}
import faker.Name.{name ⇒ randomName}
import payloads.CartPayloads.CreateCart
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import responses.{CustomerResponse, GiftCardResponse, StoreCreditResponse}
import responses.cord.CartResponse
import testutils._
import testutils.apis.PhoenixAdminApi

trait ApiFixtureHelpers extends PhoenixAdminApi with ApiFixtures { self: FoxSuite ⇒

  def api_newCustomer(): CustomerResponse.Root = {
    val name = randomName
    customersApi
      .create(CreateCustomerPayload(name = name.some, email = randomEmail(name)))
      .as[CustomerResponse.Root]
  }

  def api_newGuestCart(): CartResponse =
    cartsApi.create(CreateCart(email = randomEmail.some)).as[CartResponse]

  def api_newCustomerCart(customerId: Int): CartResponse =
    cartsApi.create(CreateCart(customerId = customerId.some)).as[CartResponse]

  def api_newGiftCard(amount: Int, reasonId: Int): GiftCardResponse.Root =
    giftCardsApi
      .create(GiftCardCreateByCsr(balance = amount, reasonId = reasonId))
      .as[GiftCardResponse.Root]

  def api_newStoreCredit(amount: Int, reasonId: Int, customerId: Int): StoreCreditResponse.Root =
    giftCardsApi(api_newGiftCard(amount = amount, reasonId = reasonId).code)
      .convertToStoreCredit(customerId)
      .as[StoreCreditResponse.Root]
}
