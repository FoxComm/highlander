package services.orders

import scala.concurrent.ExecutionContext

import models.location.{Address, Addresses, Regions, Region}
import Addresses.scope._
import models.order._
import models.StoreAdmin
import payloads.{CreateAddressPayload, UpdateAddressPayload}
import responses.{FullOrder, TheResponse}
import responses.Addresses.buildOneShipping
import services.CartFailures.NoShipAddress
import services.{LogActivity, CartValidator, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._

import models.activity.ActivityContext

object OrderShippingAddressUpdater {

  def mustFindAddressWithRegion(id: Int)
    (implicit ec: ExecutionContext): DbResult[(Address, Region)] = {
    val justOne = Addresses.findById(id).extract.withRegions.one
    justOne.mustFindOr(NotFoundFailure404(Address, id))
  }

  def mustFindShipAddressForOrder(order: Order)(implicit ec: ExecutionContext): DbResult[OrderShippingAddress] =
    OrderShippingAddresses.findByOrderId(order.id).one.mustFindOr(NoShipAddress(order.refNum))

  def createShippingAddressFromAddressId(admin: StoreAdmin, addressId: Int, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order         ← * <~ Orders.mustFindByRefNum(refNum)
    _             ← * <~ order.mustBeCart
    addAndReg     ← * <~ mustFindAddressWithRegion(addressId)
    _             ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    (address, _)  = addAndReg
    shipAddress   ← * <~ OrderShippingAddresses.copyFromAddress(address, order.id)
    region        ← * <~ Regions.mustFindById404(shipAddress.regionId)
    validated     ← * <~ CartValidator(order).validate()
    response      ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _             ← * <~ LogActivity.orderShippingAddressAdded(admin, response, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def createShippingAddressFromPayload(admin: StoreAdmin, payload: CreateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ Orders.mustFindByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    newAddress  ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = order.customerId))
    _           ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, order.id)
    region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
    validated   ← * <~ CartValidator(order).validate()
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressAdded(admin, response, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def updateShippingAddressFromPayload(admin: StoreAdmin, payload: UpdateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ Orders.mustFindByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    shipAddress ← * <~ mustFindShipAddressForOrder(order)
    region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
    patch       =      OrderShippingAddress.fromPatchPayload(shipAddress, payload)
    _           ← * <~ OrderShippingAddresses.update(shipAddress, patch)
    validated   ← * <~ CartValidator(order).validate()
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressUpdated(admin, response, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runTxn()

  def removeShippingAddress(admin: StoreAdmin, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ Orders.mustFindByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    shipAddress ← * <~ mustFindShipAddressForOrder(order)
    region      ← * <~ Regions.mustFindById404(shipAddress.regionId)
    _           ← * <~ OrderShippingAddresses.findById(shipAddress.id).delete
    validated   ← * <~ CartValidator(order).validate()
    fullOrder   ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressDeleted(admin, fullOrder, buildOneShipping(shipAddress, region))
  } yield TheResponse.build(fullOrder, alerts = validated.alerts, warnings = validated.warnings)).runTxn()
}
