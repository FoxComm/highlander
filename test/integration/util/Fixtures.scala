package util

import scala.concurrent.ExecutionContext

import models.StoreAdmins
import models.cord._
import models.customer.Customers
import models.location.{Addresses, Regions}
import utils.aliases.{DB, EC, OC}
import utils.seeds.Seeds.Factories

trait Fixtures extends GimmeSupport {
  implicit val ec: EC = ExecutionContext.global
  implicit val db: DB
  implicit val ctx: OC

  trait AddressFixture extends CustomerFixture {
    val address = Addresses
      .create(Factories.address.copy(customerId = customer.id, isDefaultShipping = true))
      .gimme
    lazy val region = Regions.findOneById(address.regionId).gimme
  }

  trait CustomerFixture {
    val customer = Customers.create(Factories.customer).gimme
  }

  trait EmptyCustomerCartFixture extends CustomerFixture {
    def buildCart = Cart(customerId = customer.id, referenceNumber = "ABC-123")
    val cart = Carts.create(buildCart).gimme
  }

  trait StoreAdminFixture {
    val storeAdmin = StoreAdmins.create(Factories.storeAdmin).gimme
  }

  trait OrderFromCartFixture extends EmptyCustomerCartFixture {
    val order = Orders.createFromCart(cart).gimme
  }
}
