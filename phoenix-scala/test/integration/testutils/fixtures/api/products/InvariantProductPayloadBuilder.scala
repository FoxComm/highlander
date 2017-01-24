package testutils.fixtures.api.products

import cats.implicits._
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductOptionPayloads.ProductOptionPayload
import payloads.ProductPayloads.{CreateProductPayload, UpdateProductPayload}
import payloads.ProductVariantPayloads.ProductVariantPayload
import utils.aliases.Json

case class InvariantProductPayloadBuilder(price: Int = 20000,
                                          slug: Option[String] = None,
                                          albums: Option[Seq[AlbumPayload]] = None) {

  val productName: String        = randomProductName
  val productVariantCode: String = randomVariantCode

  val variantPayload: ProductVariantPayload = buildVariantPayload(productVariantCode, price)

  val createPayload: CreateProductPayload = CreateProductPayload(
      attributes = productAttrs(productName),
      variants = Seq(variantPayload),
      slug = slug.getOrElse(productName),
      options = None,
      albums = albums)

  def updatePayload(
      attributes: Option[Map[String, Json]] = None,
      slug: Option[String] = createPayload.slug.some,
      albums: Option[Seq[AlbumPayload]] = createPayload.albums,
      variants: Option[Seq[ProductVariantPayload]] = createPayload.variants.some,
      options: Option[Seq[ProductOptionPayload]] = createPayload.options): UpdateProductPayload =
    UpdateProductPayload(attributes = attributes.getOrElse(createPayload.attributes),
                         slug = slug,
                         albums = albums,
                         variants = variants,
                         options = options)
}
