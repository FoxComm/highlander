package plugins

import models.cord.{Cart, Order}
import models.cord.lineitems.CartLineItems._
import models.location.{Address, Country, Region}
import services._
import utils.aliases._
import utils.apis._

case class AvalaraPluginSettings(url: String = "",
                                 account: String = "",
                                 license: String = "",
                                 profile: String = "",
                                 isDisabled: Boolean = true,
                                 loggingEnabled: Boolean = false,
                                 commitEnabled: Boolean = false)
    extends PluginSettings

object AvalaraPlugin extends Plugin {

  val identifier = "Avalara AvaTax"

  var settings = AvalaraPluginSettings()

  val defaultTaxValue = 0

  def validateAddress(address: Address, region: Region, country: Country)(
      implicit ec: EC,
      apis: Apis): Result[Unit] = {
    if (settings.isDisabled)
      apis.avalara.validateAddress(address, region, country)
    else
      Result.unit
  }

  def getTaxForCart(cart: Cart,
                    lineItems: Seq[FindLineItemResult],
                    address: Address,
                    region: Region,
                    country: Country,
                    discount: Int)(implicit ec: EC, apis: Apis): Result[Int] = {
    if (settings.isDisabled) {
      apis.avalara.getTaxForCart(cart, lineItems, address, region, country, discount)
    } else {
      Result.good(defaultTaxValue)
    }
  }

  def getTaxForOrder(cart: Cart,
                     lineItems: Seq[FindLineItemResult],
                     address: Address,
                     region: Region,
                     country: Country,
                     discount: Int)(implicit ec: EC, apis: Apis): Result[Int] = {
    if (settings.isDisabled) {
      if (settings.commitEnabled) {
        apis.avalara.getTaxForOrder(cart, lineItems, address, region, country, discount)
      } else {
        apis.avalara.getTaxForCart(cart, lineItems, address, region, country, discount)
      }
    } else {
      Result.good(defaultTaxValue)
    }
  }

  def cancelTax(order: Order)(implicit ec: EC, apis: Apis): Result[Unit] = {
    if (!settings.isDisabled)
      apis.avalara.cancelTax(order)
    else
      Result.unit
  }

}
