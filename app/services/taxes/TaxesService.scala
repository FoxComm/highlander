package services.taxes

import models.cord._
import models.cord.lineitems._
import CartLineItems.scope._
import failures.NotFoundFailure400
import models.location._
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.Apis
import utils.db._

object TaxesService {

  def getTaxRate(cart: Cart)(implicit ec: EC, db: DB, apis: Apis): DbResultT[Int] =
    for {
      maybeAddress ← * <~ OrderShippingAddresses.findByOrderRefWithRegions(cart.refNum).one
      result ← * <~ maybeAddress.map { addressTuple ⇒
                fetchTax(cart, addressTuple._1, addressTuple._2)
              }.getOrElse(DbResultT.good(0))
    } yield result

  private def fetchTax(cart: Cart,
                       address: OrderShippingAddress,
                       region: Region)(implicit ec: EC, db: DB, apis: Apis): DbResultT[Int] =
    for {
      li       ← * <~ CartLineItems.byCordRef(cart.refNum).lineItems.result
      country  ← * <~ Countries.mustFindById400(region.countryId)
      discount ← * <~ CartTotaler.adjustmentsTotal(cart)
      result ← * <~ apis.avalaraApi.getTaxForCart(cart,
                                                  li,
                                                  Address.fromOrderShippingAddress(address),
                                                  region,
                                                  country,
                                                  discount)
    } yield result

  def finalizeTaxes(cart: Cart)(implicit ec: EC, db: DB, apis: Apis): DbResultT[Unit] =
    for {
      lineItems ← * <~ CartLineItems.byCordRef(cart.refNum).lineItems.result
      addressTuple ← * <~ OrderShippingAddresses
                      .findByOrderRefWithRegions(cart.refNum)
                      .mustFindOneOr(NotFoundFailure400(OrderShippingAddress, cart.refNum))
      region   ← * <~ Regions.mustFindById404(addressTuple._1.regionId)
      country  ← * <~ Countries.mustFindById404(region.countryId)
      discount ← * <~ CartTotaler.adjustmentsTotal(cart)
      _ ← * <~ apis.avalaraApi.getTaxForOrder(cart,
                                              lineItems,
                                              Address.fromOrderShippingAddress(addressTuple._1),
                                              addressTuple._2,
                                              country,
                                              discount)
    } yield {}

  def saveAddressValidationDetails(
      address: OrderShippingAddress
  )(implicit ec: EC, db: DB, apis: Apis): DbResultT[Unit] =
    saveAddressValidationDetails(Address.fromOrderShippingAddress(address))

  def saveAddressValidationDetails(
      address: Address
  )(implicit ec: EC, db: DB, apis: Apis): DbResultT[Unit] =
    for {
      region  ← * <~ Regions.mustFindById400(address.regionId)
      country ← * <~ Countries.mustFindById400(region.countryId)
      _       ← * <~ apis.avalaraApi.validateAddress(address, region, country)
    } yield {}

}
