package services.carts

import failures.CartFailures.NoShipAddress
import failures.NotFoundFailure404
import models.cord._
import models.location.Addresses.scope._
import models.location._
import models.traits.Originator
import payloads.AddressPayloads._
import responses.Addresses.buildOneShipping
import responses.TheResponse
import responses.cart.FullCart
import services.{CartValidator, LogActivity}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartShippingAddressUpdater {

  def mustFindAddressWithRegion(id: Int)(implicit ec: EC): DbResultT[(Address, Region)] =
    Addresses.findById(id).extract.withRegions.mustFindOneOr(NotFoundFailure404(Address, id))

  def mustFindShipAddressForCart(cart: Cart)(implicit ec: EC): DbResultT[OrderShippingAddress] =
    OrderShippingAddresses.findByOrderRef(cart.refNum).mustFindOneOr(NoShipAddress(cart.refNum))

  def createShippingAddressFromAddressId(originator: Originator,
                                         addressId: Int,
                                         refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart      ← * <~ getCartByOriginator(originator, refNum)
      addAndReg ← * <~ mustFindAddressWithRegion(addressId)
      _         ← * <~ OrderShippingAddresses.findByOrderRef(cart.refNum).delete
      (address, _) = addAndReg
      _           ← * <~ address.mustBelongToCustomer(cart.customerId)
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(address, cart.refNum)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      validated   ← * <~ CartValidator(cart).validate()
      response    ← * <~ FullCart.buildRefreshed(cart)
      _ ← * <~ LogActivity
           .orderShippingAddressAdded(originator, response, buildOneShipping(shipAddress, region))
    } yield TheResponse.validated(response, validated)

  def createShippingAddressFromPayload(originator: Originator,
                                       payload: CreateAddressPayload,
                                       refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart ← * <~ getCartByOriginator(originator, refNum)
      newAddress ← * <~ Addresses.create(
                      Address.fromPayload(payload).copy(customerId = cart.customerId))
      _           ← * <~ OrderShippingAddresses.findByOrderRef(cart.refNum).delete
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, cart.refNum)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      validated   ← * <~ CartValidator(cart).validate()
      response    ← * <~ FullCart.buildRefreshed(cart)
      _ ← * <~ LogActivity
           .orderShippingAddressAdded(originator, response, buildOneShipping(shipAddress, region))
    } yield TheResponse.validated(response, validated)

  def updateShippingAddressFromPayload(originator: Originator,
                                       payload: UpdateAddressPayload,
                                       refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, refNum)
      shipAddress ← * <~ mustFindShipAddressForCart(cart)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      patch = OrderShippingAddress.fromPatchPayload(shipAddress, payload)
      _         ← * <~ OrderShippingAddresses.update(shipAddress, patch)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ FullCart.buildRefreshed(cart)
      _ ← * <~ LogActivity.orderShippingAddressUpdated(originator,
                                                       response,
                                                       buildOneShipping(shipAddress, region))
    } yield TheResponse.validated(response, validated)

  def removeShippingAddress(originator: Originator, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, refNum)
      shipAddress ← * <~ mustFindShipAddressForCart(cart)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      _           ← * <~ OrderShippingAddresses.findById(shipAddress.id).delete
      validated   ← * <~ CartValidator(cart).validate()
      fullOrder   ← * <~ FullCart.buildRefreshed(cart)
      _ ← * <~ LogActivity.orderShippingAddressDeleted(originator,
                                                       fullOrder,
                                                       buildOneShipping(shipAddress, region))
    } yield TheResponse.validated(fullOrder, validated)
}
