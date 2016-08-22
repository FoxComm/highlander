package util

import scala.concurrent.ExecutionContext

import cats.implicits._
import models.StoreAdmin
import models.cord._
import models.customer._
import models.location._
import models.traits.Originator
import services.carts.CartShippingAddressUpdater
import utils.aliases._
import utils.seeds.Seeds.Factories

/**
  * Raw fixtures are cake-pattern definitions.
  * Each trait declares "dependencies" that must be satisfied before fixture can be created.
  */
trait RawFixtures extends GimmeSupport with TestActivityContext.AdminAC {

  implicit val ec: EC = ExecutionContext.global
  implicit val db: DB
  implicit val ctx: OC

  trait CustomerAddress_Raw {
    def customer: Customer

    def address: Address = _address

    private val _address: Address = Addresses
      .create(Factories.address.copy(customerId = customer.id, isDefaultShipping = true))
      .gimme

    lazy val region: Region = Regions.findOneById(address.regionId).safeGet.gimme
  }

  trait EmptyCustomerCart_Raw {
    def customer: Customer

    def cart: Cart = _cart

    private val _cart: Cart = Carts.create(Cart(customerId = customer.id)).gimme
  }

  trait CartWithShipAddress_Raw extends EmptyCustomerCart_Raw with CustomerAddress_Raw {
    def storeAdmin: StoreAdmin

    override def address: Address = _address

    private val _address: Address = {
      CartShippingAddressUpdater
        .createShippingAddressFromAddressId(Originator(storeAdmin),
                                            super.address.id,
                                            cart.refNum.some)
        .gimme
      super.address
    }

    val shippingAddress: OrderShippingAddress =
      OrderShippingAddresses.findByOrderRef(cart.refNum).gimme.head
  }

  trait Order_Raw {
    def cart: Cart

    val order: Order = Orders.createFromCart(cart).gimme
  }

}
