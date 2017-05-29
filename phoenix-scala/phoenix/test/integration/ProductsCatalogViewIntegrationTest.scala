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
  val searchKeyName: String  = "id"

  "Products with no active SKUs are not visible" - {
    def skuCode(skuAttrs: Json): String = (skuAttrs \ "code" \ "v").extractOpt[String].value

    "after archival" in go(sku ⇒ skusApi(skuCode(sku.attributes)).archive().mustBeOk)
    "after deactivation" in go(sku ⇒ deactivateSku(skuCode(sku.attributes)))

    def go(deactivate: SkuResponse.Root ⇒ Unit): Unit = {
      val product = new ProductSku_ApiFixture {}.product
      val psv     = new ProductsSearchViewIntegrationTest
      psv.viewOne(product.id).archivedAt mustBe 'empty
      findOne(product.id) mustBe 'defined
      product.skus.foreach(deactivate) // FIXME: why does it take 0.4 seconds? :P @michalrus
      findOne(product.id) mustBe 'empty
      psv.viewOne(product.id).archivedAt mustBe 'empty
    }
  }

  // FIXME: dead code? @michalrus
  case class ProductAlbumsFromDatabase(product: Product) {
    private def parseJsonColumnValues(query: SQLActionBuilder): List[ViewAlbum] = {
      val columnValues = query.as[String].gimme
      val result       = columnValues.map(value ⇒ parse(value).extract[List[ViewAlbum]])
      result.size must === (1)
      result.head
    }

    def usingProductCatalogView: List[ViewAlbum] =
      parseJsonColumnValues(
          sql"select albums from products_catalog_view where product_id = ${product.formId}")

    def usingProductAlbumLinksView: List[ViewAlbum] =
      parseJsonColumnValues(
          sql"select albums from product_album_links_view where product_id = ${product.id}")

    def usingAlbumSearchView: List[ViewAlbum] = {
      val columnValues =
        sql"select name, images from album_search_view where album_id in (select right_id from product_album_links where left_id = ${product.id})"
          .as[(String, Option[String])]
          .gimme
      columnValues.map {
        case (name, images) ⇒
          val parsedImages: Option[List[ViewImage]] = images.map(parse(_).extract[List[ViewImage]])
          ViewAlbum(name = name.some, images = parsedImages)
      }.toList
    }

    def getAndCompareAllViews: List[ViewAlbum] = {
      val productCatalogVersion    = usingProductCatalogView
      val productAlbumLinksVersion = usingProductAlbumLinksView
      val albumSearchViewVersion   = usingAlbumSearchView

      productCatalogVersion must === (productAlbumLinksVersion)
      productCatalogVersion must === (albumSearchViewVersion)
      productCatalogVersion
    }
  }

  // FIXME: dead code? @michalrus
  trait Fixture extends StoreAdmin_Seed {
    val imagePayload = ImagePayload(None, "http://lorem.png", "lorem.png".some, "Lorem Ipsum".some)
    val defaultAlbumPayload =
      AlbumPayload(None, None, "Sample Album".some, Some(Seq(imagePayload)))
    val scope = Scope.current

    val (album, albumImages, product, sku) = (for {
      fullAlbum ← * <~ ObjectUtils.insertFullObject(defaultAlbumPayload.formAndShadow,
                                                    ins ⇒
                                                      Albums.create(
                                                          Album(scope = Scope.current,
                                                                contextId = ctx.id,
                                                                shadowId = ins.shadow.id,
                                                                formId = ins.form.id,
                                                                commitId = ins.commit.id)))
      albumImages ← * <~ ImageManager.createImagesForAlbum(fullAlbum.model, Seq(imagePayload), ctx)
      sku ← * <~ Mvp.insertSku(
               scope,
               ctx.id,
               SimpleSku("SKU-TEST", "Test SKU", 9999, Currency.USD, active = true))

      product ← * <~ Mvp.insertProductWithExistingSkus(scope,
                                                       ctx.id,
                                                       SimpleProduct(title = "Test Product",
                                                                     active = true,
                                                                     description =
                                                                       "Test product description"),
                                                       Seq(sku))

      _ ← * <~ ProductAlbumLinks.create(
             ProductAlbumLink(leftId = product.id, rightId = fullAlbum.model.id))

    } yield (fullAlbum.model, albumImages, product, sku)).gimmeTxn
  }
}
