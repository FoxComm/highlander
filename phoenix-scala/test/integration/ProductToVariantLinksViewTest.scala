import testutils._

case class ProductToVariantLinksViewResult(productId: Int, skus: Seq[String])

class ProductToVariantLinksViewTest extends SearchViewTestBase {

  type SearchViewResult = ProductToVariantLinksViewResult
  val searchViewName: String = "product_to_variant_links_view"
  val searchKeyName: String  = "product_id"

  "Product to variant links view must be updated" - {

    "when new product with variant is created" in new ProductVariant_ApiFixture {
      val linkAsViewed = viewOne(product.id)

      // productId here is __NOT__ a form id!!!
      linkAsViewed.productId must === (product.id)
      linkAsViewed.skus.onlyElement must === (productVariantCode)
    }
  }
}
