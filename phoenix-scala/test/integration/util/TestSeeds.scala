package util

import models._
import models.account._
import models.customer._
import models.admin._
import models.payment.giftcard._
import slick.driver.PostgresDriver.api._
import util.fixtures.TestFixtureBase
import utils.seeds.Seeds.Factories
import utils.Passwords.hashPassword
import failures.GeneralFailure

/**
  * Seeds are simple values that can be created without any external dependencies.
  */
trait TestSeeds extends TestFixtureBase {

  trait StoreAdmin_Seed {
    def storeAdmin: User               = _storeAdmin
    def storeAdminUser: StoreAdminUser = _storeAdminUser

    private val _storeAdmin = Users.result.headOption
      .findOrCreate(
          Factories.createStoreAdmin(user = Factories.storeAdmin,
                                     password = "password",
                                     state = StoreAdminUser.Active,
                                     org = "tenant",
                                     roles = List("tenant_admin"),
                                     author = None))
      .gimme

    private val _storeAdminUser = StoreAdminUsers
      .findOneByAccountId(_storeAdmin.accountId)
      .mustFindOr(GeneralFailure("store admin user not created"))
      .gimme

  }

  trait Customer_Seed {
    def account: Account                  = _account
    def customer: User                    = _customer
    def customerUser: CustomerUser        = _customerUser
    def accessMethod: AccountAccessMethod = _accessMethod

    private val _account = Accounts.create(Account()).gimme
    private val _accessMethod = AccountAccessMethods
      .create(
          AccountAccessMethod(accountId = _account.id,
                              name = "login",
                              hashedPassword = hashPassword("password")))
      .gimme

    private val _customer = {
      Users.result.headOption
        .findOrCreate(Users.create(Factories.customer.copy(accountId = _account.id)))
        .gimme
    }

    private val _customerUser = CustomerUsers.result.headOption
      .findOrCreate(
          CustomerUsers.create(CustomerUser(userId = _customer.id,
                                            accountId = _customer.accountId,
                                            isGuest = false)))
      .gimme
  }

  trait GiftCardSubtype_Seed {
    def giftCardSubtype: GiftCardSubtype = _giftCardSubtype

    private val _giftCardSubtype = GiftCardSubtypes.result.headOption
      .findOrCreate(GiftCardSubtypes.create(Factories.giftCardSubTypes.head))
      .gimme
  }
}
