import cats.implicits._
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import phoenix.responses.cord.CartResponse
import phoenix.responses.users.{CustomerResponse, StoreAdminResponse}
import testutils._
import testutils.apis._
import testutils.fixtures.api.ApiFixtureHelpers

class JwtAuthTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with ApiFixtureHelpers {

  "Real test auth" - {

    "must create an admin" in withNewAdminAuth(TestLoginData("admin@admin.com")) { implicit auth ⇒
      storeAdminsApi(auth.adminId).get().as[StoreAdminResponse].email.value must === ("admin@admin.com")
    }

    "must create an admin 2" in {
      withNewAdminAuth(TestLoginData("admin@admin.com")) { implicit auth ⇒
        storeAdminsApi(auth.adminId).get().as[StoreAdminResponse].email.value must === ("admin@admin.com")
      }
    }

    "must allow to login as different user" in {
      val skuCode = ProductSku_ApiFixture().skuCode

      val adminLoginData   = TestLoginData.random
      val daisyLoginData   = TestLoginData.random
      val dudebroLoginData = TestLoginData.random

      // Daisy phones call center and asks to register her an account and add a thing to cart
      // Let's assume she just got her nails done so she can only hit refresh on the cart page and talk to CSR
      val (adminId, daisyId, daisyCartRef) = withNewAdminAuth(adminLoginData) { implicit auth ⇒
        val customer = customersApi
          .create(
            CreateCustomerPayload(email = daisyLoginData.email,
                                  password = daisyLoginData.password.some,
                                  name = "Daisy Bloom".some))
          .as[CustomerResponse]

        val cartRef = api_newCustomerCart(customer.id).referenceNumber

        cartsApi(cartRef).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 1))).mustBeOk()

        (auth.adminId, customer.id, cartRef)
      }

      // She then checks her cart on her own
      withCustomerAuth(daisyLoginData, daisyId) { implicit auth ⇒
        storefrontCartsApi.get().as[CartResponse].lineItems.skus.onlyElement.quantity must === (1)
      }

      // Dudebro comes by, registers and adds some things to his cart
      // Our store only carries one product because test api is cumbersome
      // ANNA FIX
      withNewCustomerAuth(dudebroLoginData) { implicit auth ⇒
        storefrontCartsApi.lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 10))).mustBeOk()
      }

      // Daisy strikes again: "Yeah so can you add one more of the same item?" -- "Yes m'am"
      withAdminAuth(adminLoginData, adminId) { implicit auth ⇒
        cartsApi(daisyCartRef).lineItems.add(Seq(UpdateLineItemsPayload(skuCode, 2))).mustBeOk()
      }

      // R U HAPPY DAISY
      // This tests 2 things: Daisy's cart got updated AND it doesn't interfere with dudebro's one
      withCustomerAuth(daisyLoginData, daisyId) { implicit auth ⇒
        storefrontCartsApi.get().as[CartResponse].lineItems.skus.onlyElement.quantity must === (2)
      }
    }
  }
}
