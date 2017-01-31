package testutils.fixtures.api.products

import cats.implicits._
import payloads.ProductOptionPayloads.ProductOptionPayload
import payloads.ProductPayloads.CreateProductPayload
import payloads.ProductVariantPayloads.ProductVariantPayload

case class OneOptionProductPayloadBuilder(optionCfg: ProductOptionCfg,
                                          // Which variant codes to generate
                                          variantCfg: OneOptionVariantCfg = AllVariantsCfg,
                                          // Which variant codes to attach to options
                                          optionVariantCfg: OneOptionVariantCfg = AllVariantsCfg,
                                          price: Int = 10000,
                                          productName: String = randomProductName) {

  val variantCodes: Seq[String] = variantCfg match {
    case AllVariantsCfg ⇒
      optionCfg.values.map(formatVariantCode)

    case OneOptionVariantsCfg(optionValues) ⇒
      optionValues.map(formatVariantCode)

    case NoneVariantsCfg ⇒
      Seq.empty
  }

  val optionVariantCodes: Seq[String] = optionVariantCfg match {
    case AllVariantsCfg ⇒
      variantCodes

    case OneOptionVariantsCfg(optionValues) ⇒
      optionValues.map(formatVariantCode)

    case NoneVariantsCfg ⇒
      Seq.empty
  }

  val variantsPayload: Seq[ProductVariantPayload] = buildVariantsPayload(variantCodes, price)

  val optionsPayload: Option[Seq[ProductOptionPayload]] = Seq(
      buildOptionPayload(optionCfg, optionVariantCodes)).some

  val createProductPayload: CreateProductPayload = CreateProductPayload(
      attributes = productAttrs(productName),
      variants = variantsPayload,
      options = optionsPayload,
      albums = None)

  def formatVariantCode(optionValue: String): String =
    s"$productName-$optionValue"
}
