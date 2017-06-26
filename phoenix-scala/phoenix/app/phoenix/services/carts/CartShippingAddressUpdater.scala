package phoenix.services.carts

import phoenix.models.account._
import phoenix.models.location._
import phoenix.payloads.AddressPayloads._
import phoenix.responses.AddressResponse._
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.{CartValidator, LogActivity}
import phoenix.utils.aliases._
import core.db._
import phoenix.models.location.Address._
import cats.implicits._

object CartShippingAddressUpdater {

  def createShippingAddressFromAddressId(originator: User, addressId: Int, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart      ← * <~ getCartByOriginator(originator, refNum)
      addAndReg ← * <~ mustFindByAddressId(addressId)
      (address, region) = addAndReg
      _         ← * <~ address.mustBelongToAccount(cart.accountId)
      _         ← * <~ address.bindToCart(cart.referenceNumber)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressAdded(originator, response, build(address, region))
    } yield TheResponse.validated(response, validated)

  def createShippingAddressFromPayload(originator: User,
                                       payload: CreateAddressPayload,
                                       refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart       ← * <~ getCartByOriginator(originator, refNum)
      newAddress ← * <~ Addresses.create(Address.fromPayload(payload, cart.accountId))
      _          ← * <~ newAddress.bindToCart(cart.refNum)
      region     ← * <~ Regions.mustFindById404(payload.regionId)
      validated  ← * <~ CartValidator(cart).validate()
      response   ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressAdded(originator, response, build(newAddress, region))
    } yield TheResponse.validated(response, validated)

  def updateShippingAddressFromPayload(originator: User,
                                       payload: UpdateAddressPayload,
                                       refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart      ← * <~ getCartByOriginator(originator, refNum)
      addAndReg ← * <~ mustFindByCordRef(cart.referenceNumber)
      (address, region) = addAndReg
      _         ← * <~ Addresses.update(address, Address.fromPatchPayload(address, payload))
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressUpdated(originator, response, build(address, region))
    } yield TheResponse.validated(response, validated)

  def removeShippingAddress(originator: User, refNum: Option[String] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart      ← * <~ getCartByOriginator(originator, refNum)
      addAndReg ← * <~ mustFindByCordRef(cart.referenceNumber)
      (address, region) = addAndReg
      _         ← * <~ address.mustBelongToAccount(cart.accountId)
      _         ← * <~ address.unbindFromCart()
      validated ← * <~ CartValidator(cart).validate()
      fullOrder ← * <~ CartResponse.buildRefreshed(cart)
      _ ← * <~ LogActivity()
           .orderShippingAddressDeleted(originator, fullOrder, build(address, region))
    } yield TheResponse.validated(fullOrder, validated)
}
