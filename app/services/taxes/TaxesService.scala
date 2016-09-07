package services.taxes

import models.cord._
import models.cord.lineitems._
import CartLineItems.scope._
import models.location._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.Apis
import utils.db._

object TaxesService {

  def fetchTax(cart: Cart,
               address: OrderShippingAddress,
               region: Region)(implicit ec: EC, db: DB, apis: Apis): DbResultT[Unit] =
    for {
      _       ← * <~ DbResultT.good("fetch taxes")
      li      ← * <~ CartLineItems.byCordRef(cart.refNum).lineItems.result
      country ← * <~ Countries.mustFindById400(region.countryId)
      result ← * <~ apis.avalaraApi.getTaxForCart(cart,
                                                  li,
                                                  Address.fromOrderShippingAddress(address),
                                                  region,
                                                  country)
    } yield result

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
    } yield DbResultT.unit

}
