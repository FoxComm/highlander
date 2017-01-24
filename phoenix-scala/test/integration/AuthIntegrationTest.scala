import cats.implicits._
import failures.AuthFailures.LoginFailed
import models.account.{User, Users}
import models.auth.UserToken
import payloads.CustomerPayloads.{CreateCustomerPayload, UpdateCustomerPayload}
import payloads.LoginPayload
import responses.CustomerResponse.Root
import services.Authenticator.AuthData
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures._
import testutils.{IntegrationTestBase, TestActivityContext, _}
import utils.seeds.Seeds.Factories

class AuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixMyApi
    with PhoenixPublicApi
    with TestActivityContext.AdminAC
    with BakedFixtures {

  "POST /v1/public/login" - {
    // todo get org from seeds or context?
    val payload = LoginPayload("test@example.com", "letmein".some.get, "merchant")

    def createCustomer(name: Option[String], isGuest: Option[Boolean] = None) = {
      val root = customersApi
        .create(
            CreateCustomerPayload(email = payload.email,
                                  name = name,
                                  password = payload.password.some,
                                  isGuest = isGuest))
        .as[Root]

      val created = Users.findOneByAccountId(root.id).gimme.value
      created.id must === (root.id)
      created.name must === (root.name)
    }

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

    "try my api" in new Guest_Seed {
      Factories.createCustomer(user = customer, isGuest = true, scopeId = 2).gimme

      info("my cart")
      myApi.myCart().mustBeOk()

      info("my account")
      val myAccount = myApi.myAccount()
      myAccount.mustBeOk()
      info(myAccount.entity.toString)

      myApi.myCart().mustBeOk()

      info("patched account")
      val patchAccount =
        myApi.patchAccount(UpdateCustomerPayload(email = "test@example.com".some))
      patchAccount.mustBeOk()
      info(patchAccount.entity.toString)
    }

    trait Guest_Seed extends Customer_Seed {
      override def customerAuthData: AuthData[User] =
        AuthData[User](token = UserToken.fromUserAccount(customer, account, customerClaims),
                       model = customer,
                       account = account,
                       isGuest = true)
    }

  }

}
