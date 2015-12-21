package services.orders

import scala.concurrent.ExecutionContext

import models.Addresses.scope._
import models.{Address, Addresses, Order, Orders, OrderShippingAddress, OrderShippingAddresses, Region, StoreAdmin}
import payloads.{CreateAddressPayload, UpdateAddressPayload}
import responses.{FullOrder, TheResponse}
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
    validated     ← * <~ CartValidator(order).validate
    response      ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _             ← * <~ LogActivity.orderShippingAddressAdded(admin, response, shipAddress)
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def createShippingAddressFromPayload(admin: StoreAdmin, payload: CreateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ Orders.mustFindByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    newAddress  ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = order.customerId))
    _           ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, order.id)
    validated   ← * <~ CartValidator(order).validate
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressAdded(admin, response, shipAddress)
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def updateShippingAddressFromPayload(admin: StoreAdmin, payload: UpdateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ Orders.mustFindByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    shipAddress ← * <~ mustFindShipAddressForOrder(order)
    patch       =      OrderShippingAddress.fromPatchPayload(shipAddress, payload)
    _           ← * <~ OrderShippingAddresses.update(shipAddress, patch)
    validated   ← * <~ CartValidator(order).validate
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
    _           ← * <~ LogActivity.orderShippingAddressUpdated(admin, response, shipAddress)
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def removeShippingAddress(admin: StoreAdmin, refNum: String)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order     ← * <~ Orders.mustFindByRefNum(refNum)
    _         ← * <~ order.mustBeCart
    response  ← * <~ OrderShippingAddresses.findByOrderId(order.id).deleteAll(
                        onSuccess = FullOrder.refreshAndFullOrder(order).toXor,
                        onFailure = DbResult.failure(NoShipAddress(order.refNum)))
    validated ← * <~ CartValidator(order).validate
    _         ← * <~ LogActivity.orderShippingAddressDeleted(admin, response)
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()
}
