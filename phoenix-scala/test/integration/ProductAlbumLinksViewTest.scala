import responses.ProductResponses.ProductResponse
import testutils.PayloadHelpers._
import testutils._
import testutils.fixtures.api.products.InvariantProductPayloadBuilder

case class ImageAsViewed(src: String,
                         baseUrl: Option[String],
                         title: Option[String],
                         alt: Option[String])
case class ProductAlbumAsViewed(name: String, images: Seq[ImageAsViewed])

case class ProductAlbumLinksViewResult(productId: Int, albums: Seq[ProductAlbumAsViewed])

class ProductAlbumLinksViewTest extends SearchViewTestBase {

  type SearchViewResult = ProductAlbumLinksViewResult
  val searchViewName: String = "product_album_links_view"
  val searchKeyName: String  = "product_id"

  "Product album links view must be updated when" - {

    "a new product with album is created" in {
      val createPayload = InvariantProductPayloadBuilder(albums = someAlbums).createPayload

      val product = productsApi.create(createPayload).as[ProductResponse.Root]
      product.albums must not be empty

      val productAlbum = viewOne(product.id).albums.onlyElement
      productAlbum.name must === ("Default")
      productAlbum.images.onlyElement.src must === (imageSrc)
    }

    "product's albums are updated" in new ProductVariant_ApiFixture {
      productsApi(product.id).update(payloadBuilder.updatePayload(albums = someAlbums)).mustBeOk()

      val productAlbum = viewOne(product.id).albums.onlyElement
      productAlbum.name must === ("Default")
      productAlbum.images.onlyElement.src must === (imageSrc)
    }
  }
}
