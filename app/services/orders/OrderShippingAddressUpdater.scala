package services.orders

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import models._
import payloads.{CreateAddressPayload, UpdateAddressPayload}
import responses.FullOrder
import services.CartFailures.NoShipAddress
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}
import utils.DbResultT.*
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
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order         ← * <~ mustFindOrderByRefNum(refNum)
    addAndReg     ← * <~ mustFindAddressWithRegion(addressId)
    _             ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    (address, _)  = addAndReg
    shipAddress   ← * <~ OrderShippingAddresses.copyFromAddress(address, order.id)
    response      ← * <~ FullOrder.fromOrder(order).toXor
  } yield response).value.transactionally.run()

  def createShippingAddressFromPayload(payload: CreateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order       ← * <~ mustFindOrderByRefNum(refNum)
    newAddress  ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = order.customerId))
    _           ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    _           ← * <~ OrderShippingAddresses.copyFromAddress(newAddress, order.id)
    response    ← * <~ FullOrder.fromOrder(order).toXor
  } yield response).value.transactionally.run()

  def updateShippingAddressFromPayload(payload: UpdateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order         ← * <~ mustFindOrderByRefNum(refNum)
    shipAddress   ← * <~ mustFindShipAddressForOrder(order)
    _             ← * <~ OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(shipAddress, payload))
    response      ← * <~ FullOrder.fromOrder(order).toXor
  } yield response).value.transactionally.run()

  def removeShippingAddress(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order     ← * <~ mustFindOrderByRefNum(refNum)
    _         ← * <~ order.mustBeCart
    deleted   ← * <~ OrderShippingAddresses.findByOrderId(order.id).delete
    resp      = if (deleted > 0) FullOrder.fromOrder(order).toXor else DbResult.failure(NoShipAddress(order.refNum))
    response  ← * <~ resp
  } yield response).value.transactionally.run()
}
