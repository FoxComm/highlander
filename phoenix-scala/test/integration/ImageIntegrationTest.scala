import java.nio.file.Paths
import java.time.Instant

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source

import Extensions._
import cats.implicits._
import failures.ArchiveFailures.AddImagesToArchivedAlbumFailure
import failures.ImageFailures._
import failures.ObjectFailures._
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
import util.IntegrationTestBase
import util.fixtures.BakedFixtures
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
        val response = albumsApi(album.formId).get()
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (1)

        val image :: Nil = albumResponse.images
        val src          = image.get("src")
      }

      "404 if wrong context name" in new Fixture {
        implicit val nopeCtx = ObjectContext(name = "NOPE", attributes = JNothing)
        val response         = albumsApi(album.formId)(nopeCtx).get()
        response.status must === (StatusCodes.NotFound)
        response.error must === (ObjectContextNotFound("NOPE").description)
      }

      "404 if wrong album form id" in new Fixture {
        val response = albumsApi(666).get()
        response.status must === (StatusCodes.NotFound)
        response.error must === (AlbumNotFoundForContext(666, ctx.id).description)
      }

      "Retrieves a correct version of an album after an update" in new Fixture {
        val payload  = UpdateAlbumPayload(name = "Name 2.0".some)
        val response = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.OK)
        response.as[AlbumRoot].name must === ("Name 2.0")

        val response2 = albumsApi(album.formId).get()
        response2.status must === (StatusCodes.OK)
        response2.as[AlbumRoot].name must === ("Name 2.0")
      }

      "Retrieves multiple images in correct order" in new Fixture {
        val imagePaylodSrc = Seq("1", "2")
        val imagePayload   = imagePaylodSrc.map(u ⇒ ImagePayload(src = u))
        val payload        = UpdateAlbumPayload(name = "Name 2.0".some, images = imagePayload.some)
        val response       = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.OK)

        val response2 = albumsApi(album.formId).get()
        response2.status must === (StatusCodes.OK)
        response2.as[AlbumRoot].images.map(_.src) must === (imagePaylodSrc)
      }
    }

    "POST v1/albums/:context" - {
      "Creates an album with" - {
        "no images" in new Fixture {
          val payload  = CreateAlbumPayload(name = "Empty album", position = Some(1))
          val response = albumsApi.create(payload)
          response.status must === (StatusCodes.OK)

          val albumResponse = response.as[AlbumRoot]
          albumResponse.images.length must === (0)
        }

        "one image" in new Fixture {
          val payload = CreateAlbumPayload(name = "Non-empty album",
                                           images = Seq(ImagePayload(src = "url")).some)
          val response = albumsApi.create(payload)
          response.status must === (StatusCodes.OK)

          val albumResponse = response.as[AlbumRoot]
          albumResponse.images.length must === (1)
        }

        "multiple images" in new Fixture {
          val payload =
            CreateAlbumPayload(name = "Non-empty album",
                               images =
                                 Seq(ImagePayload(src = "url"), ImagePayload(src = "url2")).some)

          val response = albumsApi.create(payload)
          response.status must === (StatusCodes.OK)
          val albumResponse = response.as[AlbumRoot]
          albumResponse.images.map(_.src) must === (payload.images.get.map(_.src))

          val response2 = albumsApi.create(payload.copy(images = payload.images.map(_.reverse)))
          response2.status must === (StatusCodes.OK)
          val albumResponse2 = response2.as[AlbumRoot]
          albumResponse2.images.map(_.src) must === (payload.images.get.map(_.src).reverse)

        }
      }

      "Fails if id  for image is specified" in new Fixture {
        val payload =
          CreateAlbumPayload(name = "Non-empty album",
                             images = Seq(ImagePayload(id = Some(1), src = "url")).some)
        val response = albumsApi.create(payload)
        response.status must === (StatusCodes.BadRequest)
      }
    }

    "DELETE v1/albums/:context/:id" - {
      "Archives album successfully" in new Fixture {
        val response = albumsApi(album.formId).delete()

        response.status must === (StatusCodes.OK)

        val result = response.as[AlbumRoot]
        withClue(result.archivedAt.value → Instant.now) {
          result.archivedAt.value.isBeforeNow must === (true)
        }
      }

      "Responds with NOT FOUND when wrong album is requested" in new Fixture {
        val response = albumsApi(666).delete()

        response.status must === (StatusCodes.NotFound)
        response.error must === (AlbumNotFoundForContext(666, ctx.id).description)
      }

      "Responds with NOT FOUND when wrong context is requested" in new Fixture {
        implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
        val response               = albumsApi(album.formId)(donkeyContext).delete()

        response.status must === (StatusCodes.NotFound)
        response.error must === (ObjectContextNotFound("donkeyContext").description)
      }
    }

    "PATCH v1/albums/:context/:id" - {
      "Update the album to have another image" in new Fixture {
        val moreImages = Seq(testPayload, ImagePayload(src = "http://test.it/test.png"))
        val payload    = UpdateAlbumPayload(images = moreImages.some)

        val response = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (2)
        albumResponse.images.map(_.id).toSet.size must === (2)
      }

      "Saves image order" in new Fixture {
        private val newImageSrc: String = "http://test.it/test.png"
        val moreImages                  = Seq(testPayload, ImagePayload(src = newImageSrc))
        val payload                     = UpdateAlbumPayload(images = moreImages.some)

        val response = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.OK)
        val albumResponse = response.as[AlbumRoot]

        albumResponse.images(0).src must === (testPayload.src)
        albumResponse.images(1).src must === (newImageSrc)

        val response2 =
          albumsApi(album.formId).update(payload.copy(images = payload.images.map(_.reverse)))
        response2.status must === (StatusCodes.OK)
        val albumResponse2 = response2.as[AlbumRoot]
        albumResponse2.images(0).src must === (newImageSrc)
        albumResponse2.images(1).src must === (testPayload.src)
      }

      "Update the album fails on image id duplications" in new Fixture {
        val moreImages =
          Seq(testPayload, ImagePayload(src = "http://test.it/test.png")).map(_.copy(id = Some(1)))
        val payload = UpdateAlbumPayload(images = moreImages.some)

        val response = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.BadRequest)
      }

      "Request the album after updating" in new Fixture {
        def checkAlbum(album: AlbumRoot) = {
          album.name must === ("Name 2.0")
          album.images must have size 1
          val image = album.images.head
          image.src must === ("http://lorem.png")
          image.alt.value must === ("Lorem Ipsum")
          image.title.value must === ("lorem.png")
        }

        val payload = UpdateAlbumPayload(name = "Name 2.0".some, images = Some(Seq(testPayload)))

        val response1 = albumsApi(album.formId).update(payload)
        response1.status must === (StatusCodes.OK)
        checkAlbum(response1.as[AlbumRoot])

        val response2 = albumsApi(album.formId).get()
        response2.status must === (StatusCodes.OK)
        checkAlbum(response2.as[AlbumRoot])
      }
    }

    "POST v1/products/:context/:id/albums" - {
      "Creates a new album on an existing product" in new ProductFixture {
        val payload =
          CreateAlbumPayload(name = testAlbumName, images = Seq(ImagePayload(src = "url")).some)
        val response = productsApi(prodForm.id).albums.create(payload)
        response.status must === (StatusCodes.OK)
        val albumResponse = response.as[AlbumRoot]

        albumResponse.images.length must === (1)

        albumResponse.name must === (testAlbumName)
        albumResponse.images.head.src must === ("url")
      }

      "Puts new album at the end" in new ProductFixture {
        val payload =
          CreateAlbumPayload(name = testAlbumName, images = Seq(ImagePayload(src = "url")).some)
        val response = productsApi(prodForm.id).albums.create(payload.copy(name = "1"))
        response.status must === (StatusCodes.OK)
        val response2 = productsApi(prodForm.id).albums.create(payload.copy(name = "2"))
        response2.status must === (StatusCodes.OK)

        val getAlbumsResponse = productsApi(prodForm.id).albums.get()
        getAlbumsResponse.status must === (StatusCodes.OK)
        val albums = getAlbumsResponse.as[Seq[AlbumRoot]]

        albums.map(_.name) must === (Seq(testAlbumName, "1", "2"))
      }
    }

    "POST products/:context/:productId/albums/position" - {
      "updates album position" in new ProductFixture {
        val payload = CreateAlbumPayload(name = "1", images = Seq(ImagePayload(src = "url")).some)

        (1 to 2).map { name ⇒
          productsApi(prodForm.id).albums.create(payload.copy(name = name.toString)).status
        } must contain only StatusCodes.OK

        val originalOrder = Seq(testAlbumName, "1", "2")
        val expectedOrder = Seq("2", "1", testAlbumName)

        val albums = productsApi(prodForm.id).albums.get().as[Seq[AlbumRoot]]

        albums.map(_.name) must === (originalOrder)

        val postPositionResponse = productsApi(prodForm.id).albums
          .updatePosition(UpdateAlbumPositionPayload(albums.last.id, 0))

        postPositionResponse.status must === (StatusCodes.OK)
        postPositionResponse.as[Seq[AlbumRoot]].map(_.name) must === (expectedOrder)

        val getAlbumsResponse2 = productsApi(prodForm.id).albums.get()
        getAlbumsResponse2.status must === (StatusCodes.OK)
        val albums2 = getAlbumsResponse2.as[Seq[AlbumRoot]]

        albums2.map(_.name) must === (expectedOrder)
      }
    }

    "GET v1/products/:context/:id/albums" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val response = productsApi(prodForm.id).albums.get()
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[Seq[AlbumRoot]].headOption.value
        albumResponse.images.length must === (1)

        albumResponse.name must === (testAlbumName)
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val payload  = UpdateAlbumPayload(name = "Name 2.0".some)
        val response = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.OK)

        val response2 = productsApi(prodForm.id).albums.get()
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[Seq[AlbumRoot]].headOption.value
        albumResponse.name must === ("Name 2.0")
      }

      "Archived albums are not present in list" in new ProductFixture {
        val response = albumsApi(album.formId).delete()
        response.status must === (StatusCodes.OK)

        val response2 = productsApi(prodForm.id).albums.get()
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[Seq[AlbumRoot]]
        albumResponse.length must === (0)
      }
    }

    "GET v1/products/:context/:id" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val response = productsApi(prodForm.id).get()
        response.status must === (StatusCodes.OK)

        val productResponse = response.as[ProductResponse.Root]
        productResponse.albums.length must === (1)

        val headAlbum = productResponse.albums.head
        headAlbum.images.length must === (1)

        headAlbum.name must === ("Sample Album")
        headAlbum.images.head.src must === ("http://lorem.png")
      }

      "Retrieves the albums associated with product's SKUs" in new ProductFixture {
        val response = productsApi(prodForm.id).get()
        response.status must === (StatusCodes.OK)

        val productResponse = response.as[ProductResponse.Root]
        val headSku         = productResponse.skus.head
        headSku.albums.length must === (1)
      }

      "Archived albums are not present in list" in new ProductFixture {
        val response = albumsApi(album.formId).delete()
        response.status must === (StatusCodes.OK)

        val response2 = productsApi(prodForm.id).get()
        response2.status must === (StatusCodes.OK)

        val productResponse = response2.as[ProductResponse.Root]
        val headSku         = productResponse.skus.head
        headSku.albums.length must === (0)
      }
    }

    "GET v1/skus/:context/:code" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val response = skusApi(sku.code).get()
        response.status must === (StatusCodes.OK)

        val skuResponse = response.as[SkuResponse.Root]
        skuResponse.albums.length must === (1)

        val headAlbum = skuResponse.albums.head
        headAlbum.images.length must === (1)

        headAlbum.name must === ("Sample Album")
        headAlbum.images.head.src must === ("http://lorem.png")
      }

      "Archived albums are not present in list" in new ProductFixture {
        val response = albumsApi(album.formId).delete()
        response.status must === (StatusCodes.OK)

        val response2 = skusApi(sku.code).get()
        response2.status must === (StatusCodes.OK)

        val skuResponse = response2.as[SkuResponse.Root]
        skuResponse.albums.length must === (0)
      }
    }

    "POST v1/skus/:context/:id/albums" - {
      "Creates a new album on an existing SKU" in new ProductFixture {
        val payload =
          CreateAlbumPayload(name = "Sku Album", images = Seq(ImagePayload(src = "url")).some)
        val response = skusApi(sku.code).albums.create(payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sku Album")
        albumResponse.images.head.src must === ("url")
      }
    }

    "GET v1/skus/:context/:id/albums" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val response = skusApi(sku.code).albums.get()
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[Seq[AlbumRoot]].headOption.value
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sample Album")
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val payload  = UpdateAlbumPayload(name = "Name 2.0".some)
        val response = albumsApi(album.formId).update(payload)
        response.status must === (StatusCodes.OK)
        response.as[AlbumRoot].name must === ("Name 2.0")

        val response2 = skusApi(sku.code).albums.get()
        response2.status must === (StatusCodes.OK)
        response2.as[Seq[AlbumRoot]].headOption.value.name must === ("Name 2.0")
      }

      "Archived albums are not present in list" in new ProductFixture {
        val response = albumsApi(album.formId).delete()
        response.status must === (StatusCodes.OK)

        val response2 = skusApi(sku.code).albums.get()
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[Seq[AlbumRoot]]
        albumResponse.length must === (0)
      }
    }

    "POST /v1/albums/:context/images" - {

      "uploads image" in new Fixture {
        val image = Paths.get("test/resources/foxy.jpg")
        image.toFile.exists mustBe true

        val updatedAlbumImages = ImageManager
          .createOrUpdateImagesForAlbum(album, Seq(testPayload, testPayload), ctx)
          .gimme

        val bodyPart =
          Multipart.FormData.BodyPart.fromPath(name = "upload-file",
                                               contentType = MediaTypes.`application/octet-stream`,
                                               file = image)
        val formData = Multipart.FormData(Source.single(bodyPart))
        val entity   = Marshal(formData).to[RequestEntity].futureValue
        val uri      = pathToAbsoluteUrl(s"v1/albums/${ctx.name}/${album.id}/images")
        val request  = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)

        val response = dispatchRequest(request)
        response.status must === (StatusCodes.OK)
        val responseAlbum = response.as[AlbumRoot]

        responseAlbum.images.size must === (updatedAlbumImages.size + 1)

        val uploadedImage = responseAlbum.images.last

        uploadedImage.src must === ("amazon-image-url")
        uploadedImage.title must === ("foxy.jpg".some)
        uploadedImage.alt must === ("foxy.jpg".some)
      }

      "fail when uploading to archived album" in new ArchivedAlbumFixture {
        val image = Paths.get("test/resources/foxy.jpg")
        image.toFile.exists mustBe true

        val updatedAlbumImages = ImageManager
          .createOrUpdateImagesForAlbum(album, Seq(testPayload, testPayload), ctx)
          .gimme

        val bodyPart =
          Multipart.FormData.BodyPart.fromPath(name = "upload-file",
                                               contentType = MediaTypes.`application/octet-stream`,
                                               file = image)
        val formData = Multipart.FormData(Source.single(bodyPart))
        val entity   = Marshal(formData).to[RequestEntity].futureValue
        val uri      = pathToAbsoluteUrl(s"v1/albums/${ctx.name}/${archivedAlbum.id}/images")
        val request  = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)

        val response = dispatchRequest(request)
        response.status must === (StatusCodes.BadRequest)
        response.error must === (AddImagesToArchivedAlbumFailure(archivedAlbum.id).description)
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
                 Album(contextId = ctx.id,
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
      sku ← * <~ Skus.create(
               Sku(contextId = ctx.id,
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
                   Product(contextId = ctx.id,
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
