package testutils.fixtures.api.products

import cats.implicits._
import payloads.ProductOptionPayloads._
import payloads.ProductPayloads._
import payloads.ProductVariantPayloads._

case class TwoOptionProductPayloadBuilder(option1Cfg: ProductOptionCfg,
                                          option2Cfg: ProductOptionCfg,
                                          // Which variant codes to generate
                                          variantCfg: TwoOptionsVariantCfg = AllVariantsCfg,
                                          // Which variant codes to attach to options
                                          optionVariantCfg: TwoOptionsVariantCfg = AllVariantsCfg,
                                          price: Int = 10000,
                                          productName: String = randomProductName) {

  val variantCodes: Seq[String] = variantCfg match {
    case AllVariantsCfg ⇒
      for {
        optionValue1 ← option1Cfg.values
        optionValue2 ← option2Cfg.values
      } yield formatVariantCode(optionValue1, optionValue2)

    case TwoOptionsVariantsCfg(optionValues) ⇒
      optionValues.map {
        case (optionValue1, optionValue2) ⇒ formatVariantCode(optionValue1, optionValue2)
      }

    case NoneVariantsCfg ⇒
      Seq.empty
  }

  val optionVariantCodes: Seq[String] = optionVariantCfg match {
    case AllVariantsCfg ⇒
      variantCodes

    case TwoOptionsVariantsCfg(optionValues) ⇒
      optionValues.map {
        case (optionValue1, optionValue2) ⇒ formatVariantCode(optionValue1, optionValue2)
      }

    case NoneVariantsCfg ⇒
      Seq.empty
  }

  def filterCodes(withOptionValues: Seq[(String, String)]): Seq[String] =
    for {
      optionValues ← withOptionValues
      (value1, value2) = optionValues
      code ← variantCodes
      if code.contains(value1) && code.contains(value2)
    } yield code

  val variantsPayload: Seq[ProductVariantPayload] = buildVariantsPayload(variantCodes, price)

  val optionsPayload: Option[Seq[ProductOptionPayload]] = buildOptionsPayload(optionVariantCodes)

  val createProductPayload: CreateProductPayload = CreateProductPayload(
      attributes = productAttrs(productName),
      variants = variantsPayload,
      options = optionsPayload,
      albums = None)

  def updateToVariants(newVariantCodes: Seq[String]): UpdateProductPayload =
    UpdateProductPayload(attributes = productAttrs(productName),
                         variants = buildVariantsPayload(newVariantCodes, price).some,
                         options = buildOptionsPayload(newVariantCodes),
                         albums = None)

  def buildOptionsPayload(variantCodes: Seq[String]): Option[Seq[ProductOptionPayload]] =
    Seq(buildOptionPayload(option1Cfg, variantCodes), buildOptionPayload(option2Cfg, variantCodes)).some

  def formatVariantCode(optionValue1: String, optionValue2: String): String =
    s"$productName-$optionValue1-$optionValue2"
}
