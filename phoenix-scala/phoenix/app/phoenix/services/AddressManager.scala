package phoenix.services

import java.time.Instant

import cats.implicits._
import core.db._
import core.failures.NotFoundFailure404
import phoenix.models.account._
import phoenix.models.location.{Address, Addresses}
import phoenix.payloads.AddressPayloads._
import phoenix.responses.AddressResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object AddressManager {

  def findAllByAccountId(accountId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[AddressResponse]] = {
    val query = Addresses.findAllActiveByAccountIdWithRegions(accountId)
    for (records ← * <~ query.result) yield AddressResponse.buildMulti(records)
  }

  def get(originator: User, addressId: Int, accountId: Int)(implicit ec: EC,
                                                            db: DB): DbResultT[AddressResponse] =
    for {
      address  ← * <~ findByOriginator(originator, addressId, accountId)
      response ← * <~ AddressResponse.fromAddress(address)
    } yield response

  def create(originator: User,
             payload: CreateAddressPayload,
             accountId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[AddressResponse] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      address  ← * <~ Addresses.create(Address.fromPayload(payload, accountId))
      response ← * <~ AddressResponse.fromAddress(address)
      _        ← * <~ LogActivity().addressCreated(originator, customer, response)
    } yield response

  def edit(originator: User, addressId: Int, accountId: Int, payload: CreateAddressPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[AddressResponse] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      oldAddress ← * <~ Addresses
                    .findActiveByIdAndAccount(addressId, accountId)
                    .mustFindOneOr(addressNotFound(addressId))
      address     ← * <~ Address.fromPayload(payload, accountId).copy(id = addressId).validate
      _           ← * <~ Addresses.insertOrUpdate(address)
      response    ← * <~ AddressResponse.fromAddress(address)
      oldResponse ← * <~ AddressResponse.fromAddress(oldAddress)
      _           ← * <~ LogActivity().addressUpdated(originator, customer, response, oldResponse)
    } yield response

  def remove(originator: User, addressId: Int, accountId: Int)(implicit ec: EC,
                                                               db: DB,
                                                               ac: AC): DbResultT[Unit] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      address ← * <~ Addresses
                 .findActiveByIdAndAccount(addressId, accountId)
                 .mustFindOneOr(addressNotFound(addressId))
      softDelete ← * <~ address.copy(deletedAt = Instant.now.some, isDefaultShipping = false)
      updated    ← * <~ Addresses.update(address, softDelete)
      response   ← * <~ AddressResponse.fromAddress(updated)
      _          ← * <~ LogActivity().addressDeleted(originator, customer, response)
    } yield ()

  def setDefaultShippingAddress(addressId: Int, accountId: Int)(implicit ec: EC,
                                                                db: DB): DbResultT[AddressResponse] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _        ← * <~ removeDefaultShippingAddress(accountId)
      address ← * <~ Addresses
                 .findActiveByIdAndAccount(addressId, accountId)
                 .mustFindOneOr(addressNotFound(addressId))
      newAddress = address.copy(isDefaultShipping = true)
      _        ← * <~ Addresses.update(address, newAddress)
      response ← * <~ AddressResponse.fromAddress(newAddress)
    } yield response

  def removeDefaultShippingAddress(accountId: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    Addresses
      .findShippingDefaultByAccountId(accountId)
      .map(_.isDefaultShipping)
      .update(false)
      .dbresult
      .void

  private def findByOriginator(originator: User, addressId: Int, accountId: Int)(implicit ec: EC) =
    if (originator.accountId == accountId)
      Addresses
        .findActiveByIdAndAccount(addressId, accountId)
        .mustFindOneOr(addressNotFound(addressId))
    else
      Addresses.findByIdAndAccount(addressId, accountId).mustFindOneOr(addressNotFound(addressId))
  //MAXDO: Look at originator claims to see if they can get all addresses
  /*
        originator match {
    case AdminOriginator(_) ⇒
      Addresses
        .findByIdAndAccount(addressId, accountId)
        .mustFindOneOr(addressNotFound(addressId))
    case CustomerOriginator(_) ⇒
      Addresses
        .findActiveByIdAndAccount(addressId, accountId)
        .mustFindOneOr(addressNotFound(addressId))
  }
   */

  private def addressNotFound(id: Int) = NotFoundFailure404(Address, id)
}
