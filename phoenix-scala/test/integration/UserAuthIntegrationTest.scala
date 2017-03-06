import cats.implicits._
import failures.AuthFailures.LoginFailed
import payloads.CustomerPayloads.{CreateCustomerPayload, UpdateCustomerPayload}
import payloads.LoginPayload
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures
import testutils._

class UserAuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with PhoenixAdminApi
    with PhoenixMyApi
    with ApiFixtures
    with BakedFixtures
    with JWTAuth {

  val userEmail    = "guest@guest.com"
  val loginPayload = LoginPayload(userEmail, "123", "merchant")
  val customerPayload = CreateCustomerPayload(email = userEmail,
                                              name = Some("guest"),
                                              password = Some(loginPayload.password))
  val updateCustomerPayload = UpdateCustomerPayload(email = userEmail.some)

  "Smoke test auth" - {
    "should login for a new user account created" in {
      publicApi.prepare.login(loginPayload).run.mustFailWith400(LoginFailed)
      publicApi.prepare.register(customerPayload).run.mustBeOk()
      publicApi.prepare.login(loginPayload).run.mustBeOk()
    }

    "should create customer and get logged in" in {
      Seq(
          publicApi.prepare.register(customerPayload),
          myApi.prepare.myAccount(),
          myApi.prepare.patchAccount(updateCustomerPayload)
      ).run.mustBeOk()
    }

    "logout shoud be parsed properly" in {
      Seq(
          publicApi.prepare.register(customerPayload),
          publicApi.prepare.logout()
      ).run.mustBeOk()
    }

  }

  "Simulations should be successful" - {
    "guest with the same email should not break customer account" in {
      publicApi.prepare.register(customerPayload).run.mustBeOk()

      // we run requests in a separate lists to ensure token not being passed along
      Seq(
          myApi.prepare.myCart(),
          myApi.prepare.myAccount(),
          myApi.prepare.patchAccount(updateCustomerPayload)
      ).run.mustBeOk()

      publicApi.prepare.login(loginPayload).run.mustBeOk()
    }

    "admin should be able to log in" in new StoreAdmin_Seed {
      val adminLoginPayload = LoginPayload(storeAdmin.email.get, password, TENANT)
      publicApi.prepare.login(adminLoginPayload).run.mustBeOk()
    }
  }
}
