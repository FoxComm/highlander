import cats.implicits._
import payloads.CustomerPayloads.{CreateCustomerPayload, UpdateCustomerPayload}
import payloads.LoginPayload
import responses.CustomerResponse.Root
import services.customers.CustomerManager
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures._
import testutils.{IntegrationTestBase, TestActivityContext, _}
import utils.seeds.CustomerSeeds

class AuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixMyApi
    with PhoenixPublicApi
    with TestActivityContext.AdminAC
    with BakedFixtures
    with CustomerSeeds
    with AccountContext {

  // todo get org from seeds or context?
  val payload = LoginPayload("test@example.com", "letmein", "merchant")
  val customerPayload =
    CreateCustomerPayload(email = payload.email, password = payload.password.some)

  "POST /v1/public/login" - new Customer_Seed {
    val payload = LoginPayload(customer.email.get, "password", "tenant")
    "explore login" in {
      def login = publicApi.doLogin(payload)
      info("login " + login.bodyText)
      login.mustBeOk()

    }
  }

  "accounts" - {
    pending

    def create(name: Option[String], isGuest: Option[Boolean] = None) = {
      val root = customersApi
        .create(
            CreateCustomerPayload(email = payload.email,
                                  name = name,
                                  password = payload.password.some,
                                  isGuest = isGuest))
        .as[Root]
    }

    def checkAcc() = {
      val myAccount = myApi.myAccount()
      myAccount.mustBeOk()
      info(myAccount.entity.toString)
    }

    "should create customer and get logged in" in new Customer_Seed {
      private def login = publicApi.doLogin(payload)
      info("login")
      login.mustBeOk()

      val patchAccount = myApi.patchAccount(UpdateCustomerPayload(email = payload.email.some))
      patchAccount.mustBeOk()
      info(patchAccount.entity.toString)

      info("do login")
      login.mustBeOk()
    }

    "should create guest with same email" in new Customer_Seed {
      normalAuth()
      create("test".some)
      publicApi.doLogin(payload).mustBeOk()
      create("test".some, isGuest = true.some)
      publicApi.doLogin(payload).mustBeOk()
    }
  }

}
