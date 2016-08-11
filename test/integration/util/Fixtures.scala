package util

import scala.concurrent.ExecutionContext

import models.StoreAdmins
import models.cord._
import models.customer.{Customer, Customers}
import models.location.Addresses
import utils.aliases.{DB, EC}
import utils.seeds.Seeds.Factories

object Fixtures extends GimmeSupport {

  trait ImplicitBase {
    implicit val ec: EC = ExecutionContext.global
    implicit val db: DB = DbTestSupport.database
  }

  trait AddressFixture extends CustomerFixture {
    val address = Addresses
      .create(Factories.address.copy(customerId = customer.id, isDefaultShipping = true))
      .gimme
  }

  trait CustomerFixture extends ImplicitBase {
    def createCustomer: Customer = Customers.create(Factories.customer).gimme
    val customer = createCustomer
  }

  trait EmptyCustomerCartFixture extends CustomerFixture {
    def buildCarts: Seq[Cart] = Seq(Cart(customerId = customer.id, referenceNumber = "ABC-123"))
    val carts = Carts.createAllReturningModels(buildCarts).gimme
    val cart  = carts.head
  }

  trait StoreAdminFixture extends ImplicitBase {
    val storeAdmin = StoreAdmins.create(Factories.storeAdmin).gimme
  }

  /*  trait CartShippingAddresses extends ImplicitBase {
    def cart: Cart

    // replace with service call when we update it
    val shippingAddress: Address = (for {
      address ← * <~ Addresses.create(Factories.address)
      _       ← * <~ Carts.update(cart, cart.copy(shippingAddressId = address.id.some))
    } yield address).gimme
  }

  trait FullOrder extends EmptyCustomerCart with CartShippingAddresses {
    implicit val apis: Apis
    implicit val ctx: OC
    implicit val ac: AC

    val order: Order = (for {
      _     ← * <~ Checkout.fromCart(cart)
      order ← * <~ Orders.mustFindByRefNum(cart.refNum)
    } yield order).gimme
  }*/
}
