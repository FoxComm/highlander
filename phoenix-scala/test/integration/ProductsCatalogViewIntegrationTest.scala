import cats.implicits._
import com.github.tminglei.slickpg.LTree
import models.image._
import models.objects._
import models.product._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import payloads.ImagePayloads._
import services.image.ImageManager
import slick.driver.PostgresDriver.api._
import slick.jdbc.SQLActionBuilder
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.MockedApis
import utils.Money.Currency
import utils.db._

object ProductsCatalogViewIntegrationTest {

  case class ViewImage(alt: Option[String], src: String, title: Option[String]) {
    def this(payload: ImagePayload) = this(payload.alt, payload.src, payload.title)
  }
  case class ViewAlbum(name: String, images: Option[Seq[ViewImage]]) {
    def this(payload: CreateAlbumPayload) =
      this(payload.name, payload.images.map(images ⇒ images.map(new ViewImage(_))))
  }
}

class ProductsCatalogViewIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockedApis
    with BakedFixtures {

  import ProductsCatalogViewIntegrationTest._

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
          ViewAlbum(name = name, images = parsedImages)
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

  trait Fixture extends StoreAdmin_Seed {
    val imagePayload        = ImagePayload(None, "http://lorem.png", "lorem.png".some, "Lorem Ipsum".some)
    val defaultAlbumPayload = CreateAlbumPayload("Sample Album", Some(Seq(imagePayload)))
    val scope               = LTree(au.token.scope)

    val (album, albumImages, product, sku) = (for {
      fullAlbum ← * <~ ObjectUtils.insertFullObject(defaultAlbumPayload.formAndShadow,
                                                    ins ⇒
                                                      Albums.create(
                                                          Album(scope = LTree(au.token.scope),
                                                                contextId = ctx.id,
                                                                shadowId = ins.shadow.id,
                                                                formId = ins.form.id,
                                                                commitId = ins.commit.id)))
      albumImages ← * <~ ImageManager.createImagesForAlbum(fullAlbum.model, Seq(imagePayload), ctx)
      sku ← * <~ Mvp.insertSku(scope,
                               ctx.id,
                               SimpleSku("SKU-TEST", "Test SKU", 9999, Currency.USD))

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
