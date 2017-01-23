import cats.implicits._
import failures.AuthFailures.LoginFailed
import models.account.Users
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LoginPayload
import responses.CustomerResponse.Root
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixPublicApi}
import testutils.fixtures._
import testutils.{AutomaticAuth, IntegrationTestBase, TestActivityContext, _}
import utils.MockedApis
import utils.seeds.Seeds.Factories

class AuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixPublicApi
    with MockedApis
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/public/login" - {
    val email  = "test@example.com"
    val secret = "letmein".some
    val org    = "merchant" // todo get from seeds or context?

    def createCustomer(name: Option[String], isGuest: Option[Boolean] = None) = {
      val root = customersApi
        .create(
            CreateCustomerPayload(email = email,
                                  name = name,
                                  password = secret,
                                  isGuest = isGuest))
        .as[Root]

      val created = Users.findOneByAccountId(root.id).gimme.value
      created.id must === (root.id)
      created.name must === (root.name)
    }

    val payload = LoginPayload(email, secret.get, org)

    "should create customer and get logged in" in {
      publicApi.doLogin(payload).mustFailWith400(LoginFailed)
      createCustomer("test".some)
      publicApi.doLogin(payload).mustBeOk()
    }

    "should create guest with same email" in {
      createCustomer("test".some)
      publicApi.doLogin(payload).mustBeOk()
      createCustomer("guest".some, true.some)
      publicApi.doLogin(payload).mustBeOk()
    }

//    "try use seed factory?" in new Customer_Seed {
//      Factories
//        .createCustomer(user = customer, isGuest = true, scopeId = 2, password = secret)
//        .gimme
//
//      println("after guest")
//      publicApi.doLogin(LoginPayload(email, secret.get, org)).mustBeOk()
//    }
  }

}
