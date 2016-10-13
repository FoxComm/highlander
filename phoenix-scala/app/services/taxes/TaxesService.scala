package services.taxes

import models.cord._
import models.cord.lineitems._
import CartLineItems.scope._
import failures.NotFoundFailure400
import models.location._
import plugins.AvalaraPlugin
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.Apis
import utils.db._

object TaxesService {

  val plugin = AvalaraPlugin

  def getTaxRate(cart: Cart)(implicit ec: EC, db: DB, apis: Apis): DbResultT[Int] =
    for {
      maybeAddress ← * <~ OrderShippingAddresses.findByOrderRefWithRegions(cart.refNum).one
      result ← * <~ maybeAddress.map {
                case (address, region) ⇒
                  fetchTax(cart, address, region)
              }.getOrElse(DbResultT.good(0))
    } yield result

  private def fetchTax(cart: Cart,
                       address: OrderShippingAddress,
                       region: Region)(implicit ec: EC, db: DB, apis: Apis): DbResultT[Int] =
    for {
      li       ← * <~ CartLineItems.byCordRef(cart.refNum).lineItems.result
      country  ← * <~ Countries.mustFindById400(region.countryId)
      discount ← * <~ CartTotaler.adjustmentsTotal(cart)
      result ← * <~ plugin.getTaxForCart(cart,
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
      (address, _) = addressTuple
      region   ← * <~ Regions.mustFindById404(address.regionId)
      country  ← * <~ Countries.mustFindById404(region.countryId)
      discount ← * <~ CartTotaler.adjustmentsTotal(cart)
      _ ← * <~ plugin.getTaxForOrder(cart,
                                     lineItems,
                                     Address.fromOrderShippingAddress(address),
                                     region,
                                     country,
                                     discount)
    } yield {}

  def cancelTaxes(cord: Order)(implicit ec: EC, apis: Apis): DbResultT[Unit] =
    for {
      _ ← * <~ plugin.cancelTax(cord)
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
      _       ← * <~ plugin.validateAddress(address, region, country)
    } yield {}

}
