package plugins

import scala.concurrent.Future

import models.cord.{Cart, Order}
import models.cord.lineitems.CartLineItems._
import models.location.{Address, Country, Region}
import models.plugins.Plugins
import org.json4s.JsonAST._
import services._
import utils.aliases._
import utils.apis._
import utils.JsonFormatters

case class AvalaraPluginSettings(url: String = "",
                                 account: String = "",
                                 license: String = "",
                                 profile: String = "",
                                 isDisabled: Boolean = true,
                                 loggingEnabled: Boolean = false,
                                 commitEnabled: Boolean = false)
    extends PluginSettings

object AvalaraPluginSettings {
  val url            = "avatax_service_url"
  val account        = "avatax_account_number"
  val license        = "avatax_license_key"
  val profile        = "avatax_company_code"
  val loggingEnabled = "avatax_log_transactions"
  val commitEnabled  = "avatax_commit_documents"

  implicit val jsonFormats = JsonFormatters.phoenixFormats

  def fromJson(settigns: Map[String, JValue], isDisabled: Boolean): AvalaraPluginSettings = {
    val urlValue            = settigns.get(url).getOrElse(JString("")).extract[String]
    val accountValue        = settigns.get(account).getOrElse(JString("")).extract[String]
    val licenseValue        = settigns.get(license).getOrElse(JString("")).extract[String]
    val profileValue        = settigns.get(profile).getOrElse(JString("")).extract[String]
    val loggingEnabledValue = settigns.get(loggingEnabled).getOrElse(JBool(false)).extract[Boolean]
    val commitEnabledValue  = settigns.get(commitEnabled).getOrElse(JBool(false)).extract[Boolean]

    AvalaraPluginSettings(
        url = urlValue,
        account = accountValue,
        license = licenseValue,
        profile = profileValue,
        loggingEnabled = loggingEnabledValue,
        commitEnabled = commitEnabledValue,
        isDisabled = isDisabled
    )
  }
}

object AvalaraPlugin extends Plugin {
  val identifier = "Avalara AvaTax"
  var settings = AvalaraPluginSettings()
  val defaultTaxValue = 0

  def initialize()(implicit db: DB, ec: EC): Unit = {
    Plugins.findByName(identifier).run().map {
      case Some(plugin) ⇒ {
        settings = AvalaraPluginSettings.fromJson(plugin.settings, plugin.isDisabled)
        register()
      }
      case _ ⇒ {
        println(s"Cannot find settings for $identifier plugin")
        register()
      }
    }
  }

  override def receiveSettings(isDisabled: Boolean, newSettings: Map[String, JValue]): Unit = {
    settings = AvalaraPluginSettings.fromJson(newSettings, isDisabled)
    println(s"$identifier plugin received new settings.")
  }

  def receiveSettings(newSettings: AvalaraPluginSettings): Unit = {
    settings = newSettings
    println(s"$identifier plugin received new settings.")
  }

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
