import cats.implicits._
import com.github.tminglei.slickpg.LTree
import objectframework.ObjectUtils
import org.json4s._
import org.json4s.jackson.JsonMethods._
import phoenix.models.account.Scope
import phoenix.models.image._
import phoenix.models.objects._
import phoenix.models.product._
import phoenix.payloads.ImagePayloads._
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.services.image.ImageManager
import phoenix.utils.aliases.Json
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.utils.Money.Currency
import core.db._

object ProductsCatalogViewIntegrationTest {

  case class ViewImage(alt: Option[String], src: String, title: Option[String]) {
    def this(payload: ImagePayload) = this(payload.alt, payload.src, payload.title)
  }
  case class ViewAlbum(name: Option[String], images: Option[Seq[ViewImage]]) {
    def this(payload: AlbumPayload) =
      this(payload.name, payload.images.map(images ⇒ images.map(new ViewImage(_))))
  }

  case class ProductCatalogViewResult(
      id: Int,
      productId: Int,
      context: String,
      title: String,
      description: String,
      salePrice: String,
      currency: String,
      tags: String,
      albums: Json,
      scope: LTree,
      skus: Json,
      slug: String,
      retailPrice: String,
      taxonomies: Json
  )

}

class ProductsCatalogViewIntegrationTest
    extends SearchViewTestBase
    with IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures
    with SkuOps {

  import ProductsCatalogViewIntegrationTest._

  type SearchViewResult = ProductCatalogViewResult
  val searchViewName: String = "products_catalog_view"
  val searchKeyName: String  = "product_id"

  "Products with no active SKUs are not visible" - {
    def skuCode(skuAttrs: Json): String = (skuAttrs \ "code" \ "v").extractOpt[String].value

    "after archival" in go(sku ⇒ skusApi(skuCode(sku.attributes)).archive().mustBeOk)
    "after deactivation" in go(sku ⇒ deactivateSku(skuCode(sku.attributes)))

    def go(deactivate: SkuResponse ⇒ Unit): Unit = {
      val product   = ProductSku_ApiFixture().product
      val currentDb = implicitly[DB]
      val psv = new ProductsSearchViewIntegrationTest {
        override def dbOverride(): Option[DB] = Some(currentDb)
      }
      psv.viewOne(product.id).archivedAt mustBe 'empty
      findOne(product.id) mustBe 'defined
      product.skus.foreach(deactivate) // FIXME: why does it take 0.4 seconds? :P @michalrus
      findOne(product.id) mustBe 'empty
      psv.viewOne(product.id).archivedAt mustBe 'empty
    }
  }
}
