package util

import scala.concurrent.ExecutionContext.Implicits.global

import models._
import models.customer._
import utils.seeds.Seeds.Factories

/**
  * Seeds are simple values that can be created without any external dependensies.
  */
trait TestSeeds extends RawFixtures with GimmeSupport {

  trait StoreAdmin_Seed {
    val storeAdmin: StoreAdmin = StoreAdmins.create(Factories.storeAdmin).gimme
  }

  trait Customer_Seed {
    val customer: Customer = Customers.create(Factories.customer).gimme
  }
}
