package util

import models._
import models.customer._
import models.payment.giftcard._
import slick.driver.PostgresDriver.api._
import util.fixtures.TestFixtureBase
import utils.seeds.Seeds.Factories

/**
  * Seeds are simple values that can be created without any external dependencies.
  */
trait TestSeeds extends TestFixtureBase {

  trait StoreAdmin_Seed {
    def storeAdmin: StoreAdmin = _storeAdmin
    private val _storeAdmin =
      StoreAdmins.result.headOption.findOrCreate(StoreAdmins.create(Factories.storeAdmin)).gimme
  }

  trait Customer_Seed {
    def customer: Customer = _customer

    private val _customer =
      Customers.result.headOption.findOrCreate(Customers.create(Factories.customer)).gimme
  }

  trait GiftCardSubtype_Seed {
    def giftCardSubtype: GiftCardSubtype = _giftCardSubtype

    private val _giftCardSubtype = GiftCardSubtypes.result.headOption
      .findOrCreate(GiftCardSubtypes.create(Factories.giftCardSubTypes.head))
      .gimme
  }
}
