import cats.implicits._
import failures.AuthFailures.LoginFailed
import payloads.CustomerPayloads.{CreateCustomerPayload, UpdateCustomerPayload}
import payloads.LoginPayload
import responses.CustomerResponse.Root
import services.account.AccountCreateContext
import services.customers.CustomerManager
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures._
import testutils.{IntegrationTestBase, TestActivityContext, _}

class AuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixMyApi
    with PhoenixPublicApi
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/public/login" - {
    // todo get org from seeds or context?
    val createContext = AccountCreateContext(List("customer"), "merchant", 2)
    val payload       = LoginPayload("test@example.com", "letmein", "merchant")
    val customer      = CreateCustomerPayload(email = payload.email, password = payload.password.some)

    def create(name: Option[String], isGuest: Option[Boolean] = None) = {
      val root = customersApi
        .create(
            CreateCustomerPayload(email = payload.email,
                                  name = name,
                                  password = payload.password.some,
                                  isGuest = isGuest))
        .as[Root]
    }

    def createCustomer() = {
      CustomerManager.createFromAdmin(payload = customer, context = createContext)
    }

    def createGuest() = {
      CustomerManager.createGuest(createContext)
    }

    def checkAcc() = {
      val myAccount = myApi.myAccount()
      myAccount.mustBeOk()
      info(myAccount.entity.toString)
    }

    "should create customer and get logged in" in new Customer_Seed {
      normalAuth()
      create("test".some)
      myApi.myCart().mustBeOk()
      checkAcc()

      noAuth()
      checkAcc()

      myApi.patchAccount(UpdateCustomerPayload(email = payload.email.some)).mustBeOk()
      checkAcc()

      info("do login")
      publicApi.doLogin(payload).mustBeOk()
    }

//    "should create guest with same email" in new Customer_Seed {
//      create("test".some)
//      publicApi.doLogin(payload).mustBeOk()
//      create("test".some, isGuest = true.some)
//      publicApi.doLogin(payload).mustBeOk()
//    }

  }

}
