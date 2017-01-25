import cats.implicits._
import failures.AuthFailures.LoginFailed
import models.account.User
import payloads.CustomerPayloads.{CreateCustomerPayload, UpdateCustomerPayload}
import payloads.LoginPayload
import responses.CustomerResponse.Root
import services.Authenticator
import services.Authenticator.{AuthData, UserAuthenticator}
import services.account.AccountCreateContext
import services.customers.CustomerManager
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures._
import testutils.{IntegrationTestBase, TestActivityContext, _}

class GuestAuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixMyApi
    with PhoenixPublicApi
    with BakedFixtures {

  "POST /v1/my/" - {
    val createContext = AccountCreateContext(List("customer"), "merchant", 2)
    val payload       = LoginPayload("test@example.com", "letmein", "merchant")
    val customer      = CreateCustomerPayload(email = payload.email, password = payload.password.some)

    "should create customer" in new Guest_Seed {
      publicApi.doLogin(payload).mustFailWith400(LoginFailed)

      // todo create customer here?

      private val myAccount = myApi.myAccount()
      myAccount.mustBeOk()
      info(myAccount.entity.toString)

      myApi.patchAccount(UpdateCustomerPayload(email = payload.email.some)).mustBeOk()
    }

    "/my api should work" in new Guest_Seed {
      info("my cart")
      myApi.myCart().mustBeOk()

      info("my account")
      val myAccount = myApi.myAccount()
      myAccount.mustBeOk()
      info(myAccount.entity.toString)

      myApi.myCart().mustBeOk()

      info("patched account")
      val patchAccount = myApi.patchAccount(UpdateCustomerPayload(email = payload.email.some))
      patchAccount.mustBeOk()
      info(patchAccount.entity.toString)
    }

    trait Guest_Seed extends Customer_Seed
  }

  // acting as a guest
  override def overrideUserAuth: UserAuthenticator = {
    val customerCreateContext = AccountCreateContext(List("customer"), "merchant", 2)
    Authenticator.forUser(customerCreateContext)
  }

}
