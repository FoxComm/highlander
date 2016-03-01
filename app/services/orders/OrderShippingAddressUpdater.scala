package services.orders

import models.location._
import Addresses.scope._
import models.order._
import models.StoreAdmin
import models.traits.Originator
import payloads.{CreateAddressPayload, UpdateAddressPayload}
import responses.TheResponse
import responses.Addresses.buildOneShipping
import responses.order.FullOrder
import services.CartFailures.NoShipAddress
import services.{LogActivity, CartValidator, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.aliases._

import models.activity.ActivityContext

object OrderShippingAddressUpdater {

  def mustFindAddressWithRegion(id: Int)(implicit ec: EC): DbResult[(Address, Region)] = {
    val justOne = Addresses.findById(id).extract.withRegions.one
    justOne.mustFindOr(NotFoundFailure404(Address, id))
  }

  def mustFindShipAddressForOrder(order: Order)(implicit ec: EC): DbResult[OrderShippingAddress] =
    OrderShippingAddresses.findByOrderId(order.id).one.mustFindOr(NoShipAddress(order.refNum))

  def createShippingAddressFromAddressId(originator: Originator, addressId: Int, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order         ← * <~ getCartByOriginator(originator, refNum)
    _             ← * <~ order.mustBeCart
    addAndReg     ← * <~ mustFindAddressWithRegion(addressId)
    _             ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    (address, _)  = addAndReg
    _             ← * <~ address.mustBelongToCustomer(order.customerId)
    shipAddress   ← * <~ OrderShippingAddresses.copyFromAddress(address, order.id)
    region        ← * <~ Regions.mustFindById404(shipAddress.regionId)
    validated     ← * <~ CartValidator(order).validate()
    response      ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _             ← * <~ LogActivity.orderShippingAddressAdded(originator, response, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def createShippingAddressFromPayload(originator: Originator, payload: CreateAddressPayload,
    refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: ActivityContext):
    Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ getCartByOriginator(originator, refNum)
    _           ← * <~ order.mustBeCart
    newAddress  ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = order.customerId))
    _           ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, order.id)
    region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
    validated   ← * <~ CartValidator(order).validate()
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressAdded(originator, response, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def updateShippingAddressFromPayload(originator: Originator, payload: UpdateAddressPayload,
    refNum: Option[String] = None)(implicit ec: EC, db: DB, ac: ActivityContext):
    Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ getCartByOriginator(originator, refNum)
    _           ← * <~ order.mustBeCart
    shipAddress ← * <~ mustFindShipAddressForOrder(order)
    region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
    patch       =      OrderShippingAddress.fromPatchPayload(shipAddress, payload)
    _           ← * <~ OrderShippingAddresses.update(shipAddress, patch)
    validated   ← * <~ CartValidator(order).validate()
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressUpdated(originator, response, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def removeShippingAddress(originator: Originator, refNum: Option[String] = None)
    (implicit ec: EC, db: DB, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ getCartByOriginator(originator, refNum)
    _           ← * <~ order.mustBeCart
    shipAddress ← * <~ mustFindShipAddressForOrder(order)
    region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
    _           ← * <~ OrderShippingAddresses.findById(shipAddress.id).delete
    validated   ← * <~ CartValidator(order).validate()
    fullOrder   ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressDeleted(originator, fullOrder, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(fullOrder, alerts = validated.alerts, warnings = validated.warnings)).runTxn()
}
