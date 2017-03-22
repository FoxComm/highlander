import cats.implicits._
import models.account.{Accounts, Users}
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.{CustomerResponse, StoreAdminResponse}
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixStorefrontApi}
import testutils.fixtures.api.ApiFixtureHelpers
import utils.MockedApis
import cats.implicits._
import responses.cord.CartResponse

class RealTestAuthTest
    extends IntegrationTestBase
    with MockedApis
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with ApiFixtureHelpers {

  "Real test auth" - {

    "must create an admin" in withAdminAuth(TestLoginData("admin@admin.com")) { implicit auth ⇒
      storeAdminsApi(auth.adminId).get().as[StoreAdminResponse.Root].email.value must === (
          "admin@admin.com")
    }

    "must create an admin 2" in {
      withAdminAuth(TestLoginData("admin@admin.com")) { implicit auth ⇒
        storeAdminsApi(auth.adminId).get().as[StoreAdminResponse.Root].email.value must === (
            "admin@admin.com")
      }
    }

    "must allow to login as different user" in {
      val skuCode = new ProductSku_ApiFixture {}.skuCode

      val adminLoginData   = TestLoginData.random
      val daisyLoginData   = TestLoginData.random
      val dudebroLoginData = TestLoginData.random

      // Daisy phones call center and asks to register her an account and add a thing to cart
      // Let's assume she just got her nails done so she can only hit refresh on the cart page and talk to CSR
      val (adminId, daisyId, daisyCartRef) = withAdminAuth(adminLoginData) { implicit auth ⇒
        val customer = customersApi
          .create(CreateCustomerPayload(email = daisyLoginData.email,
                                        password = daisyLoginData.password.some,
                                        name = "Daisy Bloom".some))
          .as[CustomerResponse.Root]

        val cartRef = api_newCustomerCart(customer.id).referenceNumber

        cartsApi(cartRef).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1))).mustBeOk()

        (auth.adminId, customer.id, cartRef)
      }

      // She then checks her cart on her own
      withCustomerAuth(daisyLoginData, userIdIfExists = daisyId.some) { implicit auth ⇒
        storefrontApi.cart.get().as[CartResponse].lineItems.skus.onlyElement.quantity must === (1)
      }

      // Dudebro comes by, registers and adds some things to his cart
      // Our store only carries one product because test api is cubersome
      // ANNA FIX
      withCustomerAuth(dudebroLoginData) { implicit auth ⇒
        storefrontApi.cart.lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 10))).mustBeOk()
      }

      // Daisy strikes again: "Yeah so can you add one more of the same item?" -- "Yes m'am"
      withAdminAuth(adminLoginData, userIdIfExists = adminId.some) { implicit auth ⇒
        cartsApi(daisyCartRef).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 2))).mustBeOk()
      }

      // R U HAPPY DAISY
      // This tests 2 things: Daisy's cart got updated AND it doesn't interfere with dudebro's one
      withCustomerAuth(daisyLoginData, userIdIfExists = daisyId.some) { implicit auth ⇒
        storefrontApi.cart.get().as[CartResponse].lineItems.skus.onlyElement.quantity must === (2)
      }
    }
  }
}
