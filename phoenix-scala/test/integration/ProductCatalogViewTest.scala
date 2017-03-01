import cats.implicits._
import testutils.PayloadHelpers._
import testutils._
import testutils.fixtures.api.products.buildVariantPayload
import utils.Money.Currency

case class ProductCatalogViewResult(id: Int,
                                    productId: Int,
                                    slug: String,
                                    context: String,
                                    title: String,
                                    description: String,
                                    salePrice: String,
                                    retailPrice: String,
                                    currency: String,
                                    tags: Seq[String],
                                    albums: Seq[ProductAlbumAsViewed],
                                    scope: String,
                                    skus: Seq[String])

class ProductCatalogViewTest extends SearchViewTestBase {

  type SearchViewResult = ProductCatalogViewResult
  val searchViewName: String = "products_catalog_view"
  val searchKeyName: String  = "product_id"

  "Product search view must be updated" - {

    "when product is created" in new ProductVariant_ApiFixture {
      val productAsViewed = viewOne(product.id)

      {
        import productAsViewed.{tags ⇒ productTags, _}

        productId must === (product.id)
        slug must === (product.slug)
        context must === ("default")
        title must === (product.attributes.getString("title"))
        description must === (product.attributes.getString("description"))
        salePrice must === (productVariant.attributes.salePrice.toString)
        retailPrice must === (productVariant.attributes.retailPrice.toString)
        currency must === (Currency.USD.getCode)
        productTags mustBe empty
        albums mustBe empty
        scope must not be empty
        skus.onlyElement must === (productVariantCode)
      }
    }

    "when product is updated" in new ProductVariant_ApiFixture {
      private val newSlug = "new-slug"

      productsApi(product.id)
        .update(payloadBuilder.updatePayload(albums = someAlbums, slug = newSlug.some))
        .mustBeOk()

      private val updated = viewOne(product.id)
      updated.slug must === (newSlug)
      val productAlbum = updated.albums.onlyElement
      productAlbum.name must === ("Default")
      productAlbum.images.onlyElement.src must === (imageSrc)
    }

    "when variant is updated" in new ProductVariant_ApiFixture {
      private val newPrice = 123

      productVariantsApi(productVariant.id)
        .update(buildVariantPayload(code = productVariantCode, price = newPrice))
        .mustBeOk()

      private val updated = viewOne(product.id)
      updated.salePrice must === (newPrice.toString)
      updated.retailPrice must === (newPrice.toString)
    }

    "when product is archived" in new ProductVariant_ApiFixture {
      productsApi(product.id).archive().mustBeOk()

      existsInView(product.id) mustBe false
    }
  }
}
