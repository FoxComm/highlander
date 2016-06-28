import java.io.File
import java.nio.file.Paths

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source

import Extensions._
import cats.implicits._
import failures.ImageFailures._
import failures.ObjectFailures._
import models.StoreAdmins
import models.image._
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonDSL._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import payloads.ImagePayloads._
import responses.ImageResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import services.Result
import responses.ProductResponses._
import responses.SkuResponses._
import util.IntegrationTestBase
import utils.Money.Currency
import utils._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

class ImageIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockitoSugar {

  "Album Tests" - {
    "GET v1/albums/:context/:id" - {
      "Searching for a valid album returns the album" in new Fixture {
        val response = GET(s"v1/albums/${context.name}/${album.formId}")
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (1)

        val image :: Nil = albumResponse.images
        val src          = image.get("src")
      }

      "404 if wrong context name" in new Fixture {
        val response = GET(s"v1/albums/NOPE/${album.formId}")
        response.status must === (StatusCodes.NotFound)
        response.error must === (ObjectContextNotFound("NOPE").description)
      }

      "404 if wrong album form id" in new Fixture {
        val response = GET(s"v1/albums/${context.name}/666")
        response.status must === (StatusCodes.NotFound)
        response.error must === (AlbumNotFoundForContext(666, context.id).description)
      }

      "Retrieves a correct version of an album after an update" in new Fixture {
        val payload  = UpdateAlbumPayload(name = "Name 2.0".some)
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)
        response.as[AlbumRoot].name must === ("Name 2.0")

        val response2 = GET(s"v1/albums/${context.name}/${album.formId}")
        response2.status must === (StatusCodes.OK)
        response2.as[AlbumRoot].name must === ("Name 2.0")
      }
    }

    "POST v1/albums/:context" - {
      "Create an album with no images" in new Fixture {
        val payload  = CreateAlbumPayload(name = "Empty album")
        val response = POST(s"v1/albums/${context.name}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (0)
      }

      "Create an album with one image" in new Fixture {
        val payload = CreateAlbumPayload(name = "Non-empty album",
                                         images = Seq(ImagePayload(src = "url")).some)
        val response = POST(s"v1/albums/${context.name}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (1)
      }
    }

    "PATCH v1/albums/:context/:id" - {
      "Update the album to have another image" in new Fixture {
        val moreImages = Seq(images, ImagePayload(src = "http://test.it/test.png"))
        val payload    = UpdateAlbumPayload(images = moreImages.some)

        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (2)
      }

      "Ignore empty payload params" in new Fixture {
        val payload  = UpdateAlbumPayload(name = "now-empty album".some)
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.name must === ("now-empty album")
        albumResponse.images.length must === (1)
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

        val payload = UpdateAlbumPayload(name = "Name 2.0".some)

        val response1 = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response1.status must === (StatusCodes.OK)
        checkAlbum(response1.as[AlbumRoot])

        val response2 = GET(s"v1/albums/${context.name}/${album.formId}")
        response2.status must === (StatusCodes.OK)
        checkAlbum(response2.as[AlbumRoot])
      }
    }

    "POST v1/products/:context/:id/albums" - {
      "Creates a new album on an existing product" in new ProductFixture {
        val payload =
          CreateAlbumPayload(name = "Simple Album", images = Seq(ImagePayload(src = "url")).some)
        val response = POST(s"v1/products/${context.name}/${prodForm.id}/albums", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Simple Album")
        albumResponse.images.head.src must === ("url")
      }
    }

    "GET v1/products/:context/:id/albums" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val response = GET(s"v1/products/${context.name}/${prodForm.id}/albums")
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[Seq[AlbumRoot]].headOption.value
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sample Album")
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val payload  = UpdateAlbumPayload(name = "Name 2.0".some)
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val response2 = GET(s"v1/products/${context.name}/${prodForm.id}/albums")
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[Seq[AlbumRoot]].headOption.value
        albumResponse.name must === ("Name 2.0")
      }
    }

    "GET v1/products/:context/:id" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val response = GET(s"v1/products/${context.name}/${prodForm.id}")
        response.status must === (StatusCodes.OK)

        val productResponse = response.as[ProductResponse.Root]
        productResponse.albums.length must === (1)

        val headAlbum = productResponse.albums.head
        headAlbum.images.length must === (1)

        headAlbum.name must === ("Sample Album")
        headAlbum.images.head.src must === ("http://lorem.png")
      }

      "Retrieves the albums associated with product's SKUs" in new ProductFixture {
        val response = GET(s"v1/products/${context.name}/${prodForm.id}")
        response.status must === (StatusCodes.OK)

        val productResponse = response.as[ProductResponse.Root]
        val headSku         = productResponse.skus.head
        headSku.albums.length must === (1)
      }
    }

    "GET v1/skus/:context/:code" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val response = GET(s"v1/skus/${context.name}/${sku.code}")
        response.status must === (StatusCodes.OK)

        val skuResponse = response.as[SkuResponse.Root]
        skuResponse.albums.length must === (1)

        val headAlbum = skuResponse.albums.head
        headAlbum.images.length must === (1)

        headAlbum.name must === ("Sample Album")
        headAlbum.images.head.src must === ("http://lorem.png")
      }
    }

    "POST v1/skus/:context/:id/albums" - {
      "Creates a new album on an existing SKU" in new ProductFixture {
        val payload =
          UpdateAlbumPayload(name = "Sku Album".some, images = Seq(ImagePayload(src = "url")).some)
        val response = POST(s"v1/skus/${context.name}/${sku.code}/albums", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumRoot]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sku Album")
        albumResponse.images.head.src must === ("url")
      }
    }

    "GET v1/skus/:context/:id/albums" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val response = GET(s"v1/skus/${context.name}/${sku.code}/albums")
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[Seq[AlbumRoot]].headOption.value
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sample Album")
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val payload  = UpdateAlbumPayload(name = "Name 2.0".some)
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)
        response.as[AlbumRoot].name must === ("Name 2.0")

        val response2 = GET(s"v1/skus/${context.name}/${sku.code}/albums")
        response2.status must === (StatusCodes.OK)
        response2.as[Seq[AlbumRoot]].headOption.value.name must === ("Name 2.0")
      }
    }

    "POST /v1/albums/:context/images" - {

      "uploads image" in new Fixture {
        val image = Paths.get("test/resources/foxy.jpg")
        image.toFile.exists mustBe true

        when(amazonApiMock.uploadFile(any[String], any[File])(any[EC]))
          .thenReturn(Result.good("amazon-image-url"))

        val bodyPart =
          Multipart.FormData.BodyPart.fromPath(name = "upload-file",
                                               contentType = MediaTypes.`application/octet-stream`,
                                               file = image)
        val formData = Multipart.FormData(Source.single(bodyPart))
        val entity   = Marshal(formData).to[RequestEntity].futureValue
        val uri      = pathToAbsoluteUrl(s"v1/albums/${context.name}/${album.id}/images")
        val request  = HttpRequest(method = HttpMethods.POST, uri = uri, entity = entity)

        val response = dispatchRequest(request)
        response.status must === (StatusCodes.OK)
        val responseAlbum = response.as[AlbumRoot]
        responseAlbum.images must contain(
            Image("amazon-image-url", "foxy.jpg".some, "foxy.jpg".some))
      }
    }
  }

  trait Fixture {
    def createShadowAttr(key: String, attrType: String) =
      key → (("type" → attrType) ~ ("ref" → key))

    val imageJson =
      ("src" → "http://lorem.png") ~ ("title" → "lorem.png") ~ ("alt" → "Lorem Ipsum")
    val images = imageJson.extract[ImagePayload]

    val albumFormAttrs = ("name" → "Sample Album") ~ ("images" → Seq(imageJson))
    val albumShadowAttrs =
      createShadowAttr("name", "string") ~ createShadowAttr("images", "images")

    val form   = ObjectForm(kind = Album.kind, attributes = albumFormAttrs)
    val shadow = ObjectShadow(attributes = albumShadowAttrs)

    val (context, album) = (for {
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
      ins ← * <~ ObjectUtils.insert(form, shadow)
      album ← * <~ Albums.create(
                 Album(contextId = context.id,
                       shadowId = ins.shadow.id,
                       formId = ins.form.id,
                       commitId = ins.commit.id))
    } yield (context, album)).gimme
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
               Sku(contextId = context.id,
                   formId = skuForm.id,
                   shadowId = skuShadow.id,
                   commitId = skuCommit.id,
                   code = "SKU-TEST"))
      _ ← * <~ ObjectLinks.create(
             ObjectLink(leftId = sku.shadowId,
                        rightId = album.shadowId,
                        linkType = ObjectLink.SkuAlbum))

      simpleProd ← * <~ SimpleProduct(title = "Test Product",
                                      description = "Test product description")
      prodForm    ← * <~ ObjectForms.create(simpleProd.create)
      sProdShadow ← * <~ SimpleProductShadow(simpleProd)
      prodShadow  ← * <~ ObjectShadows.create(sProdShadow.create.copy(formId = prodForm.id))
      prodCommit ← * <~ ObjectCommits.create(
                      ObjectCommit(formId = prodForm.id, shadowId = prodShadow.id))
      product ← * <~ Products.create(
                   Product(contextId = context.id,
                           formId = prodForm.id,
                           shadowId = prodShadow.id,
                           commitId = prodCommit.id))

      _ ← * <~ ObjectLinks.create(
             ObjectLink(leftId = product.shadowId,
                        rightId = album.shadowId,
                        linkType = ObjectLink.ProductAlbum))
      _ ← * <~ ProductSkuLinks.create(ProductSkuLink(leftId = product.id, rightId = sku.id))
    } yield (product, prodForm, prodShadow, sku, skuForm, skuShadow)).gimme
  }
}
