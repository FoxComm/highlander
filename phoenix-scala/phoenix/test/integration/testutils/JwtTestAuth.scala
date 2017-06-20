package testutils

import akka.http.scaladsl.model.headers.Cookie
import cats.implicits._
import core.db._
import org.scalatest._
import phoenix.models.account.{Accounts, Users}
import phoenix.models.auth.UserToken
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.payloads.LoginPayload
import phoenix.payloads.StoreAdminPayloads.CreateStoreAdminPayload
import phoenix.responses.users.{CustomerResponse, StoreAdminResponse}
import phoenix.services.StoreAdminManager
import phoenix.services.account.AccountManager
import phoenix.utils.aliases.{SF, SL}
import phoenix.utils.seeds.generators.GeneratorUtils.randomString
import testutils.apis._
import utils.MockedApis

case class TestLoginData(email: String, password: String)
object TestLoginData {

  def apply(email: String): TestLoginData =
    TestLoginData(email = email, password = randomPassword)

  def random: TestLoginData =
    TestLoginData(email = randomEmail, password = randomPassword)

  private def randomEmail: String    = faker.Internet.email
  private def randomPassword: String = randomString(8)
}

case class TestAdminAuth(jwtCookie: Cookie, adminId: Int, loginData: TestLoginData)

case class TestCustomerAuth(jwtCookie: Cookie, customerId: Int, loginData: TestLoginData)

trait DefaultJwtAdminAuth extends JwtTestAuth { self: FoxSuite ⇒
  implicit def defaultAdminAuthImplicit: TestAdminAuth = defaultAdminAuth
}

trait JwtTestAuth
    extends DbTestSupport
    with GimmeSupport
    with PhoenixPublicApi
    with PhoenixAdminApi
    with AppendedClues
    with MockedApis
    with TestActivityContext.AdminAC { self: FoxSuite ⇒

  val defaultAdminLoginData: TestLoginData = TestLoginData("default@admin.com")

  def defaultAdmin: StoreAdminResponse =
    Users.findByEmail(defaultAdminLoginData.email).one.gimme match {
      case Some(admin) ⇒
        StoreAdminManager.getById(admin.accountId).gimme
      case _ ⇒
        StoreAdminManager
          .create(
            CreateStoreAdminPayload(org = "tenant",
                                    name = faker.Name.name,
                                    email = defaultAdminLoginData.email,
                                    password = defaultAdminLoginData.password.some,
                                    roles = List("admin")),
            author = None
          )
          .gimme
    }

  protected def defaultAdminAuth: TestAdminAuth =
    TestAdminAuth(jwtCookie = bakeJwtCookie(defaultAdmin.id),
                  adminId = defaultAdmin.id,
                  loginData = defaultAdminLoginData)

  def withRandomAdminAuth[Out](testCode: TestAdminAuth ⇒ Out)(implicit sl: SL, sf: SF): Out =
    withNewAdminAuth(TestLoginData.random)(testCode)

  def withNewAdminAuth[Out](loginData: TestLoginData)(testCode: TestAdminAuth ⇒ Out)(implicit sl: SL,
                                                                                     sf: SF): Out = {
    val adminId = storeAdminsApi
      .create(
        CreateStoreAdminPayload(org = "tenant",
                                email = loginData.email,
                                password = loginData.password.some,
                                name = faker.Name.name,
                                roles = List("admin")))(defaultAdminAuth)
      .as[StoreAdminResponse]
      .id
    withAdminAuth(loginData, adminId)(testCode)
  }

  def withAdminAuth[Out](loginData: TestLoginData, adminId: Int)(
      testCode: TestAdminAuth ⇒ Out)(implicit sl: SL, sf: SF): Out = {
    storeAdminsApi(adminId).get()(defaultAdminAuth).mustBeOk()

    val jwtCookie = bakeJwtCookie(adminId)

    val userId = publicApi
      .login(LoginPayload(org = "tenant", email = loginData.email, password = loginData.password), jwtCookie)
      .as[UserToken]
      .id
    testCode(TestAdminAuth(adminId = userId, jwtCookie = jwtCookie, loginData = loginData))
  } withClue originalSourceClue

  def withRandomCustomerAuth[Out](testCode: TestCustomerAuth ⇒ Out)(implicit sl: SL, sf: SF): Out =
    withNewCustomerAuth(TestLoginData.random)(testCode)

  def withNewCustomerAuth[Out](loginData: TestLoginData)(testCode: TestCustomerAuth ⇒ Out)(implicit sl: SL,
                                                                                           sf: SF): Out = {
    val customerId = customersApi
      .create(
        CreateCustomerPayload(email = loginData.email,
                              password = loginData.password.some,
                              name = faker.Name.name.some))(defaultAdminAuth)
      .as[CustomerResponse]
      .id

    withCustomerAuth(loginData, customerId)(testCode)
  }

  def withCustomerAuth[Out](loginData: TestLoginData, customerId: Int)(
      testCode: TestCustomerAuth ⇒ Out)(implicit sl: SL, sf: SF): Out = {
    customersApi(customerId).get()(defaultAdminAuth).mustBeOk()

    val jwtCookie = bakeJwtCookie(customerId)

    val userId = publicApi
      .login(LoginPayload(org = "merchant", email = loginData.email, password = loginData.password),
             jwtCookie)
      .as[UserToken]
      .id
    testCode(TestCustomerAuth(customerId = userId, jwtCookie = jwtCookie, loginData = loginData))
  } withClue originalSourceClue

  private def bakeJwtCookie(accountId: Int)(implicit sl: SL, sf: SF): Cookie = {
    val userToken = for {
      user    ← * <~ Users.mustFindByAccountId(accountId)
      account ← * <~ Accounts.mustFindById400(accountId)
      claims  ← * <~ AccountManager.getClaims(accountId, 1)
    } yield UserToken.fromUserAccount(user, account, claims)

    Cookie("JWT", userToken.gimme.encode.rightVal)
  } withClue originalSourceClue
}
