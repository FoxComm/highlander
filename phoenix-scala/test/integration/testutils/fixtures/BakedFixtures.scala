package testutils.fixtures

import testutils.TestSeeds

/**
  * Cake-inspired class names galore.
  * Baked fixtures are raw fixtures combined. (and baked) (yum). Fully baked fixture has all dependencies satisfied.
  * You can mix different raw and baked stuff to create a fixture convenient for testing without crafting models
  * manually. Resulting fixture has at least _something_ for the stuff you don't care about, and it allows you
  * to plug in a custom impl for your test subject. For example, you could create a fixture that uses predefined
  * customer, cart and addresses, but requires call site to define payments.
  * Order matters! Inject traits in order of initialization. If you suddenly start getting NullPointers, you probably
  * screwed that up.
  * The cake is a lie tho.
  */
trait BakedFixtures extends TestSeeds with RawFixtures {

  trait Reason_Baked extends StoreAdmin_Seed with Reason_Raw

  trait EmptyCustomerCart_Baked extends StoreAdmin_Seed with Customer_Seed with EmptyCart_Raw

  trait CustomerAddress_Baked extends StoreAdmin_Seed with Customer_Seed with CustomerAddress_Raw

  trait EmptyCartWithShipAddress_Baked
      extends StoreAdmin_Seed
      with EmptyCustomerCart_Baked
      with CustomerAddress_Baked
      with CartWithShipAddress_Raw

  trait Order_Baked extends EmptyCartWithShipAddress_Baked with Order_Raw {
    override implicit lazy val au = customerAuthData
  }

  trait ProductAndSkus_Baked
      extends StoreAdmin_Seed
      with Sku_Raw
      with Product_Raw
      with Schemas_Seed

  trait ProductAndVariants_Baked
      extends StoreAdmin_Seed
      with Product_Raw
      with ProductWithVariants_Raw
}
