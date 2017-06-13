package phoenix.services.carts

import phoenix.failures.CartFailures.NoShipAddress
import core.failures.NotFoundFailure404
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.models.location.Addresses.scope._
import phoenix.models.location._
import phoenix.payloads.AddressPayloads._
import phoenix.responses.AddressResponse.buildFromOrder
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.{CartValidator, LogActivity}
import slick.jdbc.PostgresProfile.api._
import phoenix.utils.aliases._
import core.db._

object CartShippingAddressUpdater {

  def mustFindAddressWithRegion(id: Int)(implicit ec: EC): DbResultT[(Address, Region)] =
    Addresses.findById(id).extract.withRegions.mustFindOneOr(NotFoundFailure404(Address, id))

  def mustFindShipAddressForCart(cart: Cart)(implicit ec: EC): DbResultT[OrderShippingAddress] =
    OrderShippingAddresses.findByOrderRef(cart.refNum).mustFindOneOr(NoShipAddress(cart.refNum))

  def createShippingAddressFromAddressId(originator: User, addressId: Int, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart      ← * <~ getCartByOriginator(originator, refNum)
      addAndReg ← * <~ mustFindAddressWithRegion(addressId)
      _         ← * <~ OrderShippingAddresses.findByOrderRef(cart.refNum).delete
      (address, _) = addAndReg
      _           ← * <~ address.mustBelongToAccount(cart.accountId)
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(address, cart.refNum)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      validated   ← * <~ CartValidator(cart).validate()
      response    ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressAdded(originator, response, buildFromOrder(shipAddress, region))
    } yield TheResponse.validated(response, validated)

  def createShippingAddressFromPayload(originator: User,
                                       payload: CreateAddressPayload,
                                       refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, refNum)
      newAddress  ← * <~ Addresses.create(Address.fromPayload(payload, cart.accountId))
      _           ← * <~ OrderShippingAddresses.findByOrderRef(cart.refNum).delete
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, cart.refNum)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      validated   ← * <~ CartValidator(cart).validate()
      response    ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressAdded(originator, response, buildFromOrder(shipAddress, region))
    } yield TheResponse.validated(response, validated)

  def updateShippingAddressFromPayload(originator: User,
                                       payload: UpdateAddressPayload,
                                       refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, refNum)
      shipAddress ← * <~ mustFindShipAddressForCart(cart)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      patch = OrderShippingAddress.fromPatchPayload(shipAddress, payload)
      _         ← * <~ OrderShippingAddresses.update(shipAddress, patch)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressUpdated(originator, response, buildFromOrder(shipAddress, region))
    } yield TheResponse.validated(response, validated)

  def removeShippingAddress(originator: User, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, refNum)
      shipAddress ← * <~ mustFindShipAddressForCart(cart)
      region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
      _           ← * <~ OrderShippingAddresses.findById(shipAddress.id).delete
      validated   ← * <~ CartValidator(cart).validate()
      fullOrder   ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressDeleted(originator, fullOrder, buildFromOrder(shipAddress, region))
    } yield TheResponse.validated(fullOrder, validated)
}
