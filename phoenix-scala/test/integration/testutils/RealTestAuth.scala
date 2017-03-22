package testutils

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Cookie
import cats.implicits._
import models.account.{Accounts, Users}
import models.auth.UserToken
import org.scalatest._
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LoginPayload
import payloads.StoreAdminPayloads.CreateStoreAdminPayload
import responses.StoreAdminResponse.Root
import responses.{CustomerResponse, StoreAdminResponse}
import services.StoreAdminManager
import services.account.AccountManager
import testutils.apis.{PhoenixAdminApi, PhoenixPublicApi}
import utils.aliases.{SF, SL}
import utils.db._
import utils.seeds.generators.GeneratorUtils.randomString

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

trait DefaultAdminAuth extends RealTestAuth { self: FoxSuite ⇒
  implicit def defaultAdminAuthImplicit: TestAdminAuth = defaultStoreAdminAuth
}

trait RealTestAuth
    extends DbTestSupport
    with GimmeSupport
    with PhoenixPublicApi
    with PhoenixAdminApi
    with AppendedClues
    with TestActivityContext.AdminAC {
  self: FoxSuite ⇒

  val defaultStoreAdminLoginData: TestLoginData = TestLoginData("default@admin.com")

  def defaultStoreAdmin: Root =
    Users.findByEmail(defaultStoreAdminLoginData.email).one.gimme match {
      case Some(admin) ⇒
        StoreAdminManager.getById(admin.id).gimme
      case _ ⇒
        StoreAdminManager
          .create(CreateStoreAdminPayload(org = "tenant",
                                          name = faker.Name.name,
                                          email = defaultStoreAdminLoginData.email,
                                          password = defaultStoreAdminLoginData.password.some,
                                          roles = List("admin")),
                  author = None)
          .gimme
    }

  protected def defaultStoreAdminAuth: TestAdminAuth =
    TestAdminAuth(jwtCookie = bakeJwtCookie(defaultStoreAdmin.id),
                  adminId = defaultStoreAdmin.id,
                  loginData = defaultStoreAdminLoginData)

  def withAdminAuth[Out](testCode: TestAdminAuth ⇒ Out)(implicit sl: SL, sf: SF): Out =
    withAdminAuth(TestLoginData.random)(testCode)

  def withAdminAuth[Out](loginData: TestLoginData, userIdIfExists: Option[Int] = None)(
      testCode: TestAdminAuth ⇒ Out)(implicit sl: SL, sf: SF): Out = {
    val adminId = userIdIfExists match {
      case Some(id) ⇒
        storeAdminsApi(id).get()(defaultStoreAdminAuth).mustBeOk()
        id
      case _ ⇒
        storeAdminsApi
          .create(
              CreateStoreAdminPayload(org = "tenant",
                                      email = loginData.email,
                                      password = loginData.password.some,
                                      name = faker.Name.name,
                                      roles = List("admin")))(defaultStoreAdminAuth)
          .as[StoreAdminResponse.Root]
          .id
    }

    val jwtCookie = bakeJwtCookie(adminId)

    val userId = publicApi
      .login(LoginPayload(org = "tenant", email = loginData.email, password = loginData.password),
             jwtCookie)
      .as[UserToken]
      .id
    testCode(TestAdminAuth(adminId = userId, jwtCookie = jwtCookie, loginData = loginData))
  } withClue originalSourceClue

  def withCustomerAuth[Out](testCode: TestCustomerAuth ⇒ Out)(implicit sl: SL, sf: SF): Out =
    withCustomerAuth(TestLoginData.random)(testCode)

  def withCustomerAuth[Out](loginData: TestLoginData, userIdIfExists: Option[Int] = None)(
      testCode: TestCustomerAuth ⇒ Out)(implicit sl: SL, sf: SF): Out = {
    val customerId = userIdIfExists match {
      case Some(id) ⇒
        customersApi(id).get()(defaultStoreAdminAuth).mustBeOk()
        id
      case _ ⇒
        customersApi
          .create(
              CreateCustomerPayload(email = loginData.email,
                                    password = loginData.password.some,
                                    name = faker.Name.name.some))(defaultStoreAdminAuth)
          .as[CustomerResponse.Root]
          .id
    }

    val jwtCookie = bakeJwtCookie(customerId)

    val userId = publicApi
      .login(
          LoginPayload(org = "merchant", email = loginData.email, password = loginData.password),
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
