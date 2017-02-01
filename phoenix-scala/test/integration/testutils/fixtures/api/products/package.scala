package testutils.fixtures.api

import cats.implicits._
import faker.Lorem
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductOptionPayloads.{ProductOptionPayload, ProductOptionValuePayload}
import payloads.ProductVariantPayloads.ProductVariantPayload
import testutils.PayloadHelpers._
import utils.aliases.Json

package object products {

  def randomProductName: String =
    s"prod_${Lorem.numerify("####")}"

  def randomVariantCode: String =
    Lorem.letterify("????").toUpperCase

  def productAttrs(name: String = randomProductName,
                   title: String = Lorem.sentence(1),
                   description: String = Lorem.sentence(3)): Map[String, Json] =
    Map("name" → tv(name), "title" → tv(title), "description" → tv(description))

  def variantAttrs(code: String,
                   price: Int,
                   title: String = Lorem.sentence(1)): Map[String, Json] =
    Map("code"        → tv(code),
        "title"       → tv(title),
        "salePrice"   → usdPrice(price),
        "retailPrice" → usdPrice(price))

  def optionAttrs(name: String): Map[String, Json] =
    Map("name" → tv(name))

  def buildVariantPayload(code: String = randomVariantCode,
                          price: Int = 10000,
                          title: String = Lorem.sentence(1),
                          albums: Option[Seq[AlbumPayload]] = None): ProductVariantPayload =
    ProductVariantPayload(attributes = variantAttrs(code, price, title), albums = albums)

  def buildVariantsPayload(variantCodes: Seq[String], price: Int): Seq[ProductVariantPayload] =
    variantCodes.map(code ⇒ buildVariantPayload(code, price))

  def buildOptionPayload(optionCfg: ProductOptionCfg, variantCodes: Seq[String]) = {
    val optionValues = optionCfg.values.map { optionValue ⇒
      ProductOptionValuePayload(name = optionValue.some,
                                skuCodes = variantCodes.filter(_.contains(optionValue)),
                                swatch = None)
    }
    ProductOptionPayload(attributes = optionAttrs(optionCfg.name), values = optionValues.some)
  }

  // Example: name: "Fabric", values: Seq("cotton", "silk")
  case class ProductOptionCfg(name: String, values: Seq[String])

  // Configurations for generating variants from option values
  sealed trait OneOptionVariantCfg
  sealed trait TwoOptionsVariantCfg

  // Generate variant codes for option values' × (aka cross, aka cartesian) product
  case object AllVariantsCfg extends OneOptionVariantCfg with TwoOptionsVariantCfg

  // Generate no variant codes, because reasons
  case object NoneVariantsCfg extends OneOptionVariantCfg with TwoOptionsVariantCfg

  // Example: Seq("gold", "silver"), omitting "bronze"
  case class OneOptionVariantsCfg(optionValues: Seq[String]) extends OneOptionVariantCfg

  // Tuples of option values that you want variants to be created for
  // Example: Seq(("blonde", "short"), ("long", "black"))
  // Yes, we're now selling wigs here
  // Order of tuple elements should not matter
  case class TwoOptionsVariantsCfg(optionValues: Seq[(String, String)])
      extends TwoOptionsVariantCfg
}
