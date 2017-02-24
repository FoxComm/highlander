import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.util.ByteString
import failures.AuthFailures.LoginFailed
import payloads.CustomerPayloads.{CreateCustomerPayload, UpdateCustomerPayload}
import payloads.LoginPayload
import testutils.{AutomaticAuth, IntegrationTestBase}
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures
import testutils._
import cats.implicits._
import org.json4s.jackson.Serialization.write

import scala.collection.immutable
import scala.util.Random

class UserAuthIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with PhoenixAdminApi
    with PhoenixMyApi
    with ApiFixtures
    with BakedFixtures
    with JWTAuth {

  "smoke test auth" - {
    val userEmail    = "guest@guest.com"
    val loginPayload = LoginPayload(userEmail, "123", "merchant")
    val customerPayload = CreateCustomerPayload(email = userEmail,
                                                name = Some("guest"),
                                                password = Some(loginPayload.password))
    val updateCustomerPayload = UpdateCustomerPayload(email = "guest_updated@guest.com".some)

    "should login for a new user account created" in {
      publicApi.login(loginPayload).mustFailWith400(LoginFailed)
      publicApi.register(customerPayload).mustBeOk()
      publicApi.login(loginPayload).mustBeOk()
    }

    "should be working with jwt tokens" in new CustomerAddress_Baked {
      pending
      publicApi.register(customerPayload).mustBeOk()

      val login = publicApi.login(loginPayload)
      login.mustBeOk()

      myApi.myAccount().mustBeOk()
      myApi.patchAccount(UpdateCustomerPayload(email = loginPayload.email.some)).mustBeOk()
    }

    "should create customer and get logged in" in new Customer_Seed {
      pending
      private val register = publicApi.register(customerPayload)
      register.mustBeOk()

      val myAccount = myApi.myAccount()
      myAccount.mustBeOk()
      info(myAccount.entity.toString)

      val patchAccount = myApi.patchAccount(updateCustomerPayload)
      patchAccount.mustBeOk()
      info(patchAccount.entity.toString)
    }

    "run requests in series" in {
      runRequests(
          Seq(
              buildRequest(HttpMethods.POST, "v1/public/registrations/new", customerPayload.some),
              buildRequest(HttpMethods.GET, "v1/my/account"),
              buildRequest(HttpMethods.PATCH, "v1/my/account", updateCustomerPayload.some)
          ))
    }
  }
}
