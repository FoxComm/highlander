package testutils

import cats.implicits._
import failures.GeneralFailure
import failures.UserFailures._
import models.account._
import models.admin._
import models.auth._
import models.customer._
import models.payment.giftcard._
import services.Authenticator.AuthData
import services.account.AccountManager
import slick.driver.PostgresDriver.api._
import testutils.fixtures.TestFixtureBase
import utils.aliases.AU
import utils.db._
import utils.seeds.Seeds.Factories

/**
  * Seeds are simple values that can be created without any external dependencies.
  */
trait TestSeeds extends TestFixtureBase {

  val TENANT = "tenant"

  trait StoreAdmin_Seed {
    def storeAdminAccount: Account         = _storeAdminAccount
    def storeAdmin: User                   = _storeAdmin
    def storeAdminUser: AdminData          = _storeAdminUser
    def storeAdminClaims: Account.ClaimSet = _storeAdminClaims

    def storeAdminAuthData: AuthData[User] =
      AuthData[User](token =
                       UserToken.fromUserAccount(storeAdmin, storeAdminAccount, storeAdminClaims),
                     model = storeAdmin,
                     account = storeAdminAccount)
    implicit lazy val au: AU = storeAdminAuthData

    private val (_storeAdminAccount, _storeAdmin, _storeAdminUser, _storeAdminClaims) = (for {
      maybeAdmin ← * <~ Users
                    .findByEmail(Factories.storeAdminTemplate.email.getOrElse(""))
                    .result
                    .headOption

      ad ← * <~ (maybeAdmin match {
                case Some(admin) ⇒ DbResultT.pure(admin)
                case None ⇒
                  Factories.createStoreAdmin(user = Factories.storeAdminTemplate,
                                             password = "password",
                                             state = AdminData.Active,
                                             org = TENANT,
                                             roles = List("admin"),
                                             author = None)
              })
      adu ← * <~ AdminsData.mustFindByAccountId(ad.accountId)
      ac  ← * <~ Accounts.mustFindById404(ad.accountId)
      organization ← * <~ Organizations
                      .findByName(TENANT)
                      .mustFindOr(OrganizationNotFoundByName(TENANT))
      claims ← * <~ AccountManager.getClaims(ac.id, organization.scopeId)
    } yield (ac, ad, adu, claims)).gimme

  }

  trait Customer_Seed extends StoreAdmin_Seed {
    def account: Account                  = _account
    def customer: User                    = _customer
    def customerData: CustomerData        = _customerData
    def accessMethod: AccountAccessMethod = _accessMethod

    private val (_account, _customer, _customerData, _accessMethod) = (for {
      c ← * <~ Factories.createCustomer(user = Factories.customer,
                                        isGuest = false,
                                        scopeId = 2,
                                        password = "password".some)
      a  ← * <~ Accounts.mustFindById404(c.accountId)
      cu ← * <~ CustomersData.mustFindByAccountId(c.accountId)
      am ← * <~ AccountAccessMethods
            .findOneByAccountIdAndName(c.accountId, "login")
            .mustFindOr(GeneralFailure("access method not found"))
    } yield (a, c, cu, am)).gimme

  }

  trait GiftCardSubtype_Seed {
    def giftCardSubtype: GiftCardSubtype = _giftCardSubtype

    private val _giftCardSubtype = GiftCardSubtypes.result.headOption
      .findOrCreate(GiftCardSubtypes.create(Factories.giftCardSubTypes.head))
      .gimme
  }
}
