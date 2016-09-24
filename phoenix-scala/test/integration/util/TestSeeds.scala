package util

import cats.implicits._

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
import utils.db._

/**
  * Seeds are simple values that can be created without any external dependencies.
  */
trait TestSeeds extends TestFixtureBase {

  trait StoreAdmin_Seed {
    def storeAdmin: User               = _storeAdmin
    def storeAdminUser: StoreAdminUser = _storeAdminUser

    private val (_storeAdmin, _storeAdminUser) = (for {
      ad ← * <~ Factories.createStoreAdmin(user = Factories.storeAdmin,
                                           password = "password",
                                           state = StoreAdminUser.Active,
                                           org = "tenant",
                                           roles = List("tenant_admin"),
                                           author = None)
      adu ← * <~ StoreAdminUsers.mustFindByAccountId(ad.accountId)
    } yield (ad, adu)).gimme

  }

  trait Customer_Seed {
    def account: Account                  = _account
    def customer: User                    = _customer
    def customerUser: CustomerUser        = _customerUser
    def accessMethod: AccountAccessMethod = _accessMethod

    private val (_account, _customer, _customerUser, _accessMethod) = (for {
      c ← * <~ Factories.createCustomer(user = Factories.customer,
                                        isGuest = false,
                                        scopeId = 2,
                                        password = "password".some)
      a  ← * <~ Accounts.mustFindById404(c.accountId)
      cu ← * <~ CustomerUsers.mustFindByAccountId(c.accountId)
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
