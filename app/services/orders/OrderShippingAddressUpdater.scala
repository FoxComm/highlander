package services.orders

import scala.concurrent.ExecutionContext

import models._
import payloads.{CreateAddressPayload, UpdateAddressPayload}
import responses.{TheResponse, FullOrder}
import services.CartFailures.NoShipAddress
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}
import utils.DbResultT._
import utils.DbResultT.implicits._
import orders.Helpers._
import Addresses.scope._

object OrderShippingAddressUpdater {

  def mustFindAddressWithRegion(id: Int)
    (implicit ec: ExecutionContext): DbResult[(Address, Region)] = {
    val justOne = Addresses.findById(id).extract.withRegions.one
    justOne.mustFindOr(NotFoundFailure404(Address, id))
  }

  def mustFindShipAddressForOrder(order: Order)(implicit ec: ExecutionContext): DbResult[OrderShippingAddress] =
    OrderShippingAddresses.findByOrderId(order.id).one.mustFindOr(NoShipAddress(order.refNum))

  def createShippingAddressFromAddressId(addressId: Int, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {

    order         ← * <~ mustFindOrderByRefNum(refNum)
    _             ← * <~ order.mustBeCart
    addAndReg     ← * <~ mustFindAddressWithRegion(addressId)
    _             ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    (address, _)  = addAndReg
    shipAddress   ← * <~ OrderShippingAddresses.copyFromAddress(address, order.id)
    validated     ← * <~ CartValidator(order).validate
    response      ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def createShippingAddressFromPayload(payload: CreateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ mustFindOrderByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    newAddress  ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = order.customerId))
    _           ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    _           ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, order.id)
    validated   ← * <~ CartValidator(order).validate
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def updateShippingAddressFromPayload(payload: UpdateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {

    order       ← * <~ mustFindOrderByRefNum(refNum)
    _           ← * <~ order.mustBeCart
    shipAddress ← * <~ mustFindShipAddressForOrder(order)
    _           ← * <~ OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(shipAddress, payload))
    validated   ← * <~ CartValidator(order).validate
    response    ← * <~ FullOrder.refreshAndFullOrder(order).toXor
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()

  def removeShippingAddress(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[FullOrder.Root]] = (for {

    order     ← * <~ mustFindOrderByRefNum(refNum)
    _         ← * <~ order.mustBeCart
    response  ← * <~ OrderShippingAddresses.findByOrderId(order.id).deleteAll(
                        onSuccess = FullOrder.refreshAndFullOrder(order).toXor,
                        onFailure = DbResult.failure(NoShipAddress(order.refNum)))
    validated ← * <~ CartValidator(order).validate
  } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).runT()
}
