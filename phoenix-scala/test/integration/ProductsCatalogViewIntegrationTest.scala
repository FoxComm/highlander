import cats.implicits._
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
    with MockedApis {

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
      val albumIds = ProductAlbumLinks.filterLeft(product).map(_.rightId).result.gimme

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

    def getAndCompareAllViews = {
      val productCatalogVersion    = usingProductCatalogView
      val productAlbumLinksVersion = usingProductAlbumLinksView
      val albumSearchViewVersion   = usingAlbumSearchView

      productCatalogVersion must === (productAlbumLinksVersion)
      productCatalogVersion must === (albumSearchViewVersion)
      productCatalogVersion
    }
  }

  "album-related views should be updated on" - {
    "album created" in new Fixture {
      val payload = CreateAlbumPayload(name = "test", images = Seq(ImagePayload(src = "url")).some)
      productsApi(product.formId).albums.create(payload).mustBeOk()

      val albums = ProductAlbumsFromDatabase(product).getAndCompareAllViews

      val expectedAlbums: Seq[ViewAlbum] =
        Seq(new ViewAlbum(defaultAlbumPayload), new ViewAlbum(payload))
      albums must contain theSameElementsAs expectedAlbums
    }

    "album updated" in new Fixture {
      val moreImages = Seq(imagePayload, ImagePayload(src = "http://test.it/test.png"))
      val payload    = UpdateAlbumPayload(images = moreImages.some)

      albumsApi(album.formId).update(payload).mustBeOk()

      val albums = ProductAlbumsFromDatabase(product).getAndCompareAllViews
      albums.size must === (1)

      val dbAlbum = albums.head
      dbAlbum.images.get.size must === (payload.images.get.size)
      dbAlbum.images.get.map(_.src) must === (payload.images.get.map(_.src))
    }

    "album archived" in new Fixture {
      albumsApi(album.formId).delete().mustBeOk()

      val albums = ProductAlbumsFromDatabase(product).getAndCompareAllViews
      albums.size must === (0)
    }
  }

  trait Fixture {
    val imagePayload        = ImagePayload(None, "http://lorem.png", "lorem.png".some, "Lorem Ipsum".some)
    val defaultAlbumPayload = CreateAlbumPayload("Sample Album", Some(Seq(imagePayload)))

    val (album, albumImages, product, sku) = (for {
      fullAlbum ← * <~ ObjectUtils.insertFullObject(defaultAlbumPayload.formAndShadow,
                                                    ins ⇒
                                                      Albums.create(
                                                          Album(contextId = ctx.id,
                                                                shadowId = ins.shadow.id,
                                                                formId = ins.form.id,
                                                                commitId = ins.commit.id)))
      albumImages ← * <~ ImageManager.createImagesForAlbum(fullAlbum.model, Seq(imagePayload), ctx)
      sku         ← * <~ Mvp.insertSku(ctx.id, SimpleSku("SKU-TEST", "Test SKU", 9999, Currency.USD))

      product ← * <~ Mvp.insertProductWithExistingSkus(ctx.id,
                                                       SimpleProduct(title = "Test Product",
                                                                     active = true,
                                                                     description =
                                                                       "Test product description"),
                                                       Seq(sku))

      _ ← * <~ ProductAlbumLinks.create(
             ProductAlbumLink(leftId = product.id, rightId = fullAlbum.model.id))
    } yield (fullAlbum.model, albumImages, product, sku)).gimme
  }
}
