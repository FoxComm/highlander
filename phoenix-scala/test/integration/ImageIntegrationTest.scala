import java.nio.file.Paths
import java.time.Instant

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.ArchiveFailures.AddImagesToArchivedAlbumFailure
import failures.ImageFailures._
import failures.ObjectFailures._
import models.account.Scope
import models.image._
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import payloads.ImagePayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.ProductResponses._
import responses.SkuResponses._
import services.image.ImageManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.Money.Currency
import utils._
import utils.db._
import utils.time.RichInstant

class ImageIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "Album Tests" - {
    "GET v1/albums/:context/:id" - {
      "Searching for a valid album returns the album" in new Fixture {
        val albumResponse = albumsApi(album.formId).get().as[AlbumRoot]

        albumResponse.images.length must === (1)
        albumResponse.images.head.src must not be empty
      }

      "404 if wrong context name" in new Fixture {
        implicit val nopeCtx = ObjectContext(name = "NOPE", attributes = JNothing)
        albumsApi(album.formId)(nopeCtx).get().mustFailWith404(ObjectContextNotFound("NOPE"))
      }

      "404 if wrong album form id" in new Fixture {
        albumsApi(666).get().mustFailWith404(AlbumNotFoundForContext(666, ctx.id))
      }

      "Retrieves a correct version of an album after an update" in new Fixture {
        albumsApi(album.formId)
          .update(AlbumPayload(name = "Name 2.0".some))
          .as[AlbumRoot]
          .name must === ("Name 2.0")

        albumsApi(album.formId).get().as[AlbumRoot].name must === ("Name 2.0")
      }

      "Retrieves multiple images in correct order" in new Fixture {
        val imageSources = Seq("1", "2")

        albumsApi(album.formId)
          .update(AlbumPayload(name = "Name 2.0".some,
                               images = imageSources.map(u ⇒ ImagePayload(src = u)).some))
          .mustBeOk()

        albumsApi(album.formId).get().as[AlbumRoot].images.map(_.src) must === (imageSources)
      }
    }

    "POST v1/albums/:context" - {
      "Creates an album with" - {
        "no images" in new Fixture {
          albumsApi
            .create(AlbumPayload(name = Some("Empty album"), position = Some(1)))
            .as[AlbumRoot]
            .images mustBe empty
        }

        "one image" in new Fixture {
          albumsApi
            .create(AlbumPayload(name = Some("Non-empty album"),
                                 images = Seq(ImagePayload(src = "url")).some))
            .as[AlbumRoot]
            .images
            .length must === (1)
        }

        "multiple images" in new Fixture {
          val sources = Seq("url", "url2")
          val payload = AlbumPayload(name = Some("Non-empty album"),
                                     images = sources.map(s ⇒ ImagePayload(src = s)).some)

          val ordered = albumsApi.create(payload).as[AlbumRoot]
          ordered.images.map(_.src) must === (sources)

          val reversed =
            albumsApi.create(payload.copy(images = payload.images.map(_.reverse))).as[AlbumRoot]
          reversed.images.map(_.src) must === (sources.reverse)
        }
      }
    }

    "DELETE v1/albums/:context/:id" - {
      "Archives album successfully" in new Fixture {
        val result = albumsApi(album.formId).delete().as[AlbumRoot]
        withClue(result.archivedAt.value → Instant.now) {
          result.archivedAt.value.isBeforeNow must === (true)
        }
      }

      "Responds with NOT FOUND when wrong album is requested" in new Fixture {
        albumsApi(666).delete().mustFailWith404(AlbumNotFoundForContext(666, ctx.id))
      }

      "Responds with NOT FOUND when wrong context is requested" in new Fixture {
        implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
        albumsApi(album.formId)(donkeyContext)
          .delete()
          .mustFailWith404(ObjectContextNotFound("donkeyContext"))
      }
    }

    "PATCH v1/albums/:context/:id" - {
      "Update the album to have another image" in new Fixture {
        val payload = AlbumPayload(images = Seq(testPayload, ImagePayload(src = "foo")).some)

        val albumResponse = albumsApi(album.formId).update(payload).as[AlbumRoot]

        albumResponse.images.length must === (2)
        albumResponse.images.map(_.id).toSet.size must === (2)
      }

      "Saves image order" in new Fixture {
        val newImageSrc = "http://test.it/test.png"
        val moreImages  = Seq(testPayload, ImagePayload(src = newImageSrc))
        val payload     = AlbumPayload(images = moreImages.some)

        val ordered = albumsApi(album.formId).update(payload).as[AlbumRoot]
        ordered.images.map(_.src) must === (Seq(testPayload.src, newImageSrc))

        val reversed = albumsApi(album.formId)
          .update(payload.copy(images = payload.images.map(_.reverse)))
          .as[AlbumRoot]
        reversed.images.map(_.src) must === (Seq(newImageSrc, testPayload.src))
      }

      "Update the album fails on image id duplications" in new Fixture {
        val moreImages = Seq(testPayload, ImagePayload(src = "foo")).map(_.copy(id = Some(1)))
        albumsApi(album.formId)
          .update(AlbumPayload(images = moreImages.some))
          .mustFailWithMessage("Image ID is duplicated 1")
      }

      "Request the album after updating" in new Fixture {
        def checkAlbum(album: AlbumRoot): Unit = {
          album.name must === ("Name 2.0")
          album.images must have size 1
          val image = album.images.head
          image.src must === ("http://lorem.png")
          image.alt.value must === ("Lorem Ipsum")
          image.title.value must === ("lorem.png")
        }

        val payload = AlbumPayload(name = "Name 2.0".some, images = Seq(testPayload).some)
        checkAlbum(albumsApi(album.formId).update(payload).as[AlbumRoot])
        checkAlbum(albumsApi(album.formId).get().as[AlbumRoot])
      }
    }

    "POST v1/products/:context/:id/albums" - {
      "Creates a new album on an existing product" in new ProductFixture {
        val payload =
          AlbumPayload(name = Some(testAlbumName), images = Seq(ImagePayload(src = "url")).some)
        val albumResponse = productsApi(prodForm.id).albums.create(payload).as[AlbumRoot]

        albumResponse.name must === (testAlbumName)
        albumResponse.images.length must === (1)
        albumResponse.images.head.src must === ("url")
      }

      "Puts new album at the end" in new ProductFixture {
        val payload = AlbumPayload(name = Some("0"), images = Seq(ImagePayload(src = "url")).some)
        productsApi(prodForm.id).albums.create(payload.copy(name = Some("1"))).mustBeOk()
        productsApi(prodForm.id).albums.create(payload.copy(name = Some("2"))).mustBeOk()

        val albums = productsApi(prodForm.id).albums.get().as[Seq[AlbumRoot]]
        albums.map(_.name) must === (Seq(testAlbumName, "1", "2"))
      }
    }

    "POST products/:context/:productId/albums/position" - {
      "updates album position" in new ProductFixture {
        val _albumsApi = productsApi(prodForm.id).albums

        val imagePayload = Seq(ImagePayload(src = "url")).some
        Seq("1", "2").foreach { name ⇒
          val payload = AlbumPayload(name = Some(name), images = imagePayload)
          _albumsApi.create(payload).mustBeOk()
        }

        val originalOrder = Seq(testAlbumName, "1", "2")
        val expectedOrder = originalOrder.reverse

        val productAlbums = _albumsApi.get().as[Seq[AlbumRoot]]
        productAlbums.map(_.name) must === (originalOrder)
        val albumId = productAlbums.last.id

        val positionPayload = UpdateAlbumPositionPayload(albumId, 0)
        val repositioned    = _albumsApi.updatePosition(positionPayload).as[Seq[AlbumRoot]]
        repositioned.map(_.name) must === (expectedOrder)

        val updated = _albumsApi.get().as[Seq[AlbumRoot]]
        updated.map(_.name) must === (expectedOrder)
      }
    }

    "GET v1/products/:context/:id/albums" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val response = productsApi(prodForm.id).albums.get().as[Seq[AlbumRoot]].headOption.value

        response.name must === (testAlbumName)
        response.images.length must === (1)
        response.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        albumsApi(album.formId).update(AlbumPayload(name = "Name 2.0".some)).mustBeOk()

        val response = productsApi(prodForm.id).albums.get().as[Seq[AlbumRoot]]
        response.headOption.value.name must === ("Name 2.0")
      }

      "Archived albums are not present in list" in new ProductFixture {
        albumsApi(album.formId).delete().mustBeOk()

        val albumResponse = productsApi(prodForm.id).albums.get().as[Seq[AlbumRoot]]
        albumResponse.length must === (0)
      }
    }

    "GET v1/products/:context/:id" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val productResponse = productsApi(prodForm.id).get().as[ProductResponse.Root]
        productResponse.albums.length must === (1)

        val headAlbum = productResponse.albums.head
        headAlbum.images.length must === (1)

        headAlbum.name must === ("Sample Album")
        headAlbum.images.head.src must === ("http://lorem.png")
      }

      "Retrieves the albums associated with product's SKUs" in new ProductFixture {
        val productResponse = productsApi(prodForm.id).get().as[ProductResponse.Root]

        productResponse.skus.headOption.value.albums.length must === (1)
      }

      "Archived albums are not present in list" in new ProductFixture {
        albumsApi(album.formId).delete().mustBeOk()

        val productResponse = productsApi(prodForm.id).get().as[ProductResponse.Root]
        productResponse.skus.headOption.value.albums.length must === (0)
      }
    }

    "GET v1/skus/:context/:code" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val skuResponse = skusApi(sku.code).get().as[SkuResponse.Root]
        skuResponse.albums.length must === (1)

        val headAlbum = skuResponse.albums.head
        headAlbum.images.length must === (1)

        headAlbum.name must === ("Sample Album")
        headAlbum.images.head.src must === ("http://lorem.png")
      }

      "Archived albums are not present in list" in new ProductFixture {
        albumsApi(album.formId).delete().mustBeOk()

        val skuResponse = skusApi(sku.code).get().as[SkuResponse.Root]
        skuResponse.albums.length must === (0)
      }
    }

    "POST v1/skus/:context/:id/albums" - {
      "Creates a new album on an existing SKU" in new ProductFixture {
        val payload =
          AlbumPayload(name = Some("ProductVariant Album"), images = Seq(ImagePayload(src = "url")).some)
        val albumResponse = skusApi(sku.code).albums.create(payload).as[AlbumRoot]

        albumResponse.name must === ("ProductVariant Album")
        albumResponse.images.length must === (1)
        albumResponse.images.head.src must === ("url")
      }
    }

    "GET v1/skus/:context/:id/albums" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val albumResponse = skusApi(sku.code).albums.get().as[Seq[AlbumRoot]].headOption.value
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sample Album")
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val response = albumsApi(album.formId).update(AlbumPayload(name = "Name 2.0".some))
        response.as[AlbumRoot].name must === ("Name 2.0")

        val response2 = skusApi(sku.code).albums.get()
        response2.as[Seq[AlbumRoot]].headOption.value.name must === ("Name 2.0")
      }

      "Archived albums are not present in list" in new ProductFixture {
        albumsApi(album.formId).delete().mustBeOk()

        val albumResponse = skusApi(sku.code).albums.get().as[Seq[AlbumRoot]]
        albumResponse.length must === (0)
      }
    }

    "POST /v1/albums/:context/images" - {

      "uploads image" in new Fixture {
        val updatedAlbumImages = ImageManager
          .createOrUpdateImagesForAlbum(album, Seq(testPayload, testPayload), ctx)
          .gimme

        val responseAlbum = uploadImage(album).as[AlbumRoot]
        responseAlbum.images.size must === (updatedAlbumImages.size + 1)

        val uploadedImage = responseAlbum.images.last

        uploadedImage.src must === ("amazon-image-url")
        uploadedImage.title must === ("foxy.jpg".some)
        uploadedImage.alt must === ("foxy.jpg".some)
      }

      "uploads multiple images" in new Fixture {

        val response = POST(s"/v1/albums/ru/${ctx.name}/images")

        val updatedAlbumImages = ImageManager
          .createOrUpdateImagesForAlbum(album, Seq(testPayload, testPayload), ctx)
          .gimme

        val responseAlbum = uploadImage(album, 2).as[AlbumRoot]
        responseAlbum.images.size must === (updatedAlbumImages.size + 2)

        val uploadedImage = responseAlbum.images.last

        uploadedImage.src must === ("amazon-image-url")
        uploadedImage.title must === ("foxy.jpg".some)
        uploadedImage.alt must === ("foxy.jpg".some)
      }

      "fails if uploading no images" in new Fixture {

        val response = POST(s"/v1/albums/ru/${ctx.name}/images")

        val updatedAlbumImages = ImageManager
          .createOrUpdateImagesForAlbum(album, Seq(testPayload, testPayload), ctx)
          .gimme

        val responseAlbum = uploadImage(album, 0)
        responseAlbum.error must === (ImageNotFoundInPayload.description)
      }

      "fail when uploading to archived album" in new ArchivedAlbumFixture {
        uploadImage(archivedAlbum).mustFailWith400(
            AddImagesToArchivedAlbumFailure(archivedAlbum.id))
      }

      def uploadImage(album: Album, count: Int = 1): HttpResponse = {
        val image = Paths.get("test/resources/foxy.jpg")
        image.toFile.exists mustBe true

        val entity = if (count == 0) {
          Marshal(Multipart.FormData(Multipart.FormData.BodyPart.apply("test", HttpEntity.Empty)))
            .to[RequestEntity]
            .futureValue
        } else {
          val bodyParts = 1 to count map { _ ⇒
            Multipart.FormData.BodyPart.fromPath(name = "upload-file",
                                                 contentType =
                                                   MediaTypes.`application/octet-stream`,
                                                 file = image)
          }
          val formData = Multipart.FormData(bodyParts.toList: _*)
          Marshal(formData).to[RequestEntity].futureValue
        }

        val uri     = pathToAbsoluteUrl(s"v1/albums/${ctx.name}/${album.id}/images")
        val request = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)
        dispatchRequest(request)
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    def createShadowAttr(key: String, attrType: String) =
      key → (("type" → attrType) ~ ("ref" → key))

    private val imageJson =
      ("src" → "http://lorem.png") ~ ("title" → "lorem.png") ~ ("alt" → "Lorem Ipsum")
    val testPayload = imageJson.extract[ImagePayload]

    val testAlbumName    = "Sample Album"
    val albumFormAttrs   = "name" → testAlbumName
    val albumShadowAttrs = createShadowAttr("name", "string")

    val form   = ObjectForm(kind = Album.kind, attributes = albumFormAttrs)
    val shadow = ObjectShadow(attributes = albumShadowAttrs)

    val (album, albumImages) = (for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      album ← * <~ Albums.create(
                 Album(scope = Scope.current,
                       contextId = ctx.id,
                       shadowId = ins.shadow.id,
                       formId = ins.form.id,
                       commitId = ins.commit.id))
      albumImages ← * <~ ImageManager.createImagesForAlbum(album, Seq(testPayload), ctx)
    } yield (album, albumImages)).gimme
  }

  trait ProductFixture extends Fixture {
    val (product, prodForm, prodShadow, sku, skuForm, skuShadow) = (for {
      simpleSku  ← * <~ SimpleSku("SKU-TEST", "Test SKU", 9999, Currency.USD)
      skuForm    ← * <~ ObjectForms.create(simpleSku.create)
      sSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow  ← * <~ ObjectShadows.create(sSkuShadow.create.copy(formId = skuForm.id))
      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
      sku ← * <~ ProductVariants.create(
               ProductVariant(scope = Scope.current,
                   contextId = ctx.id,
                   formId = skuForm.id,
                   shadowId = skuShadow.id,
                   commitId = skuCommit.id,
                   code = "SKU-TEST"))
      _ ← * <~ SkuAlbumLinks.create(SkuAlbumLink(leftId = sku.id, rightId = album.id))

      simpleProd ← * <~ SimpleProduct(title = "Test Product",
                                      description = "Test product description")
      prodForm    ← * <~ ObjectForms.create(simpleProd.create)
      sProdShadow ← * <~ SimpleProductShadow(simpleProd)
      prodShadow  ← * <~ ObjectShadows.create(sProdShadow.create.copy(formId = prodForm.id))
      prodCommit ← * <~ ObjectCommits.create(
                      ObjectCommit(formId = prodForm.id, shadowId = prodShadow.id))
      product ← * <~ Products.create(
                   Product(scope = Scope.current,
                           contextId = ctx.id,
                           formId = prodForm.id,
                           shadowId = prodShadow.id,
                           commitId = prodCommit.id))

      _ ← * <~ ProductAlbumLinks.create(ProductAlbumLink(leftId = product.id, rightId = album.id))
      _ ← * <~ ProductSkuLinks.create(ProductSkuLink(leftId = product.id, rightId = sku.id))
    } yield (product, prodForm, prodShadow, sku, skuForm, skuShadow)).gimme
  }

  trait ArchivedAlbumFixture extends Fixture {
    val archivedAlbum = Albums.update(album, album.copy(archivedAt = Some(Instant.now))).gimme
  }
}
