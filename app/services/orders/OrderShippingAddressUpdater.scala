package services.orders

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import models._
import payloads.{CreateAddressPayload, UpdateAddressPayload}
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}

object OrderShippingAddressUpdater {

  def createShippingAddressFromAddressId(addressId: Int, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {

    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      (for {
        address ← Addresses.findOneById(addressId)
        region ← address.map { a ⇒ Regions.findOneById(a.regionId) }.getOrElse(DBIO.successful(None))
        _ ← address match {
          case Some(a) ⇒
            for {
              _ ← OrderShippingAddresses.findByOrderId(order.id).delete
              shipAddress ← OrderShippingAddresses.copyFromAddress(a, order.id)
            } yield Some(shipAddress)

          case None ⇒
            DBIO.successful(None)
        }
      } yield (address, region)).flatMap {
        case (Some(address), Some(region)) ⇒
          DbResult.fromDbio(fullOrder(finder))
        case _ ⇒
          DbResult.failure(NotFoundFailure404(Address, addressId))
      }
    }
  }

  def createShippingAddressFromPayload(payload: CreateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      val address = Address.fromPayload(payload)
      address.validate match {
        case Valid(_) ⇒
          (for {
            newAddress ← Addresses.saveNew(address.copy(customerId = order.customerId))
            region ← Regions.findOneById(newAddress.regionId)
            _ ← OrderShippingAddresses.findByOrderId(order.id).delete
            _ ← OrderShippingAddresses.copyFromAddress(newAddress, order.id)
          } yield region).flatMap {
            case Some(region) ⇒ DbResult.fromDbio(fullOrder(finder))
            case None ⇒ DbResult.failure(NotFoundFailure404(Region, address.regionId))
          }
        case Invalid(errors) ⇒ DbResult.failures(errors)
      }
    }
  }

  def updateShippingAddressFromPayload(payload: UpdateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      val actions = for {
        oldAddress ← OrderShippingAddresses.findByOrderId(order.id).one

        rowsAffected ← oldAddress.map { osa ⇒
          OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(a = osa, p = payload))
        }.getOrElse(lift(0))

        newAddress ← OrderShippingAddresses.findByOrderId(order.id).one

        region ← newAddress.map { address ⇒
          Regions.findOneById(address.regionId)
        }.getOrElse(lift(None))
      } yield (rowsAffected, newAddress, region)

      actions.flatMap {
        case (_, None, _) ⇒
          DbResult.failure(NotFoundFailure404(OrderShippingAddress, order.id))
        case (0, _, _) ⇒
          DbResult.failure(GeneralFailure("Unable to update address"))
        case (_, Some(address), None) ⇒
          DbResult.failure(NotFoundFailure404(Region, address.regionId))
        case (_, Some(address), Some(region)) ⇒
          DbResult.fromDbio(fullOrder(finder))
      }
    }
  }

  def removeShippingAddress(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOne({ order ⇒
      OrderShippingAddresses.findByOrderId(order.id).deleteAll(
        onSuccess = DbResult.fromDbio(fullOrder(finder)),
        onFailure = DbResult.failure(NotFoundFailure400(
          s"Shipping Address for order with reference number $refNum not found")))
    }, checks = finder.checks + finder.mustBeCart)
  }

}
