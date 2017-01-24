package testutils.fixtures.api

import cats.implicits._
import com.sksamuel.elastic4s.mappings.attributes
import payloads.ProductOptionPayloads._
import payloads.ProductPayloads.CreateProductPayload
import payloads.ProductVariantPayloads.ProductVariantPayload
import testutils.PayloadHelpers._
import utils.aliases.Json

object ProductPayloadBuilder {

  def build(option1: ProductOptionCfg,
            option2: ProductOptionCfg,
            price: Int,
            productName: String): CreateProductPayload = {

    val allCodes: Seq[String] = for {
      value1 ← option1.values
      value2 ← option2.values
    } yield s"$productName-$value1-$value2"

    val options: Option[Seq[ProductOptionPayload]] = {
      val option1Payload = {
        val values = option1.values.map { option1Value ⇒
          ProductOptionValuePayload(name = option1Value.some,
                                    skuCodes = allCodes.filter(_.split('-')(1) == option1Value),
                                    swatch = None)
        }.some

        ProductOptionPayload(attributes = optionAttrs(option1.name), values = values)
      }

      val option2Payload = {
        val values = option2.values.map { option2Value ⇒
          ProductOptionValuePayload(name = option2Value.some,
                                    skuCodes = allCodes.filter(_.split('-')(2) == option2Value),
                                    swatch = None)
        }.some

        ProductOptionPayload(attributes = optionAttrs(option2.name), values = values)
      }

      Seq(option1Payload, option2Payload).some
    }

    val variants: Seq[ProductVariantPayload] = allCodes.map { code ⇒
      ProductVariantPayload(attributes = variantAttrs(code, price))
    }

    CreateProductPayload(attributes = productAttrs(productName),
                         variants = variants,
                         options = options,
                         albums = None)
  }

  case class ProductOptionCfg(name: String, values: Seq[String])

  def productAttrs(productName: String): Map[String, Json] =
    Map("name" → tv(productName.capitalize), "title" → tv(productName.capitalize))

  def variantAttrs(code: String, price: Int): Map[String, Json] =
    Map("code"        → tv(code),
        "title"       → tv(code.replace('-', ' ').capitalize),
        "salePrice"   → usdPrice(price),
        "retailPrice" → usdPrice(price))

  def optionAttrs(name: String): Map[String, Json] =
    Map("name" → tv(name))

}
