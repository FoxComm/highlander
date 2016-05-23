import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ImageFailures._
import failures.ObjectFailures._
import failures.ProductFailures._
import models.Aliases.Json
import models.image._
import models.inventory._
import models.objects._
import models.product._
import models.StoreAdmins
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import payloads.ImagePayloads._
import responses.ImageResponses.AlbumResponse
import util.IntegrationTestBase
import util.SlickSupport.implicits._
import utils.IlluminateAlgorithm
import utils.Money.Currency
import utils._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._
import utils.db.ExPostgresDriver.api._
import utils.db.ExPostgresDriver.jsonMethods._

class ImageIntegrationTest
  extends IntegrationTestBase with HttpSupport with AutomaticAuth {
  "Album Tests" - {
    "GET v1/albums/:context/:id" - {
      "Searching for a valid album returns the album" in new Fixture {
        val response = GET(s"v1/albums/${context.name}/${album.formId}")
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)

        val image :: Nil = albumResponse.images
        val src = image.get("src")
      }

    }
    "POST v1/albums/:context" - {
      "Create an album with no images" in new Fixture {
        val payload = AlbumPayload(name = "Empty album")
        val response = POST(s"v1/albums/${context.name}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (0)
      }

      "Create an album with one image" in new Fixture {
        val payload = AlbumPayload(name = "Non-empty album",
          images = Some(Seq(ImagePayload(src = "http://test.com/test.jpg"))))
        val response = POST(s"v1/albums/${context.name}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)
      }
    }

    "PATCH v1/albums/:context/:id" - {
      "Update the album to have another image" in new Fixture {
        val payload = AlbumPayload(name = "Sample album", images = Some(Seq(
          imageJson.extract[ImagePayload],
          ImagePayload(src = "http://test.it/test.png"))))

        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (2)
      }

      "Update the album to be empty" in new Fixture {
        val payload = AlbumPayload(name = "now-empty album", images = Some(Seq.empty))
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.name must === ("now-empty album")
        albumResponse.images.length must === (0)
      }

      "Request the album after updating" in new Fixture {
        val payload = AlbumPayload(name = "Name 2.0", images = Some(Seq.empty))
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val response2 = GET(s"v1/albums/${context.name}/${album.formId}")
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[AlbumResponse.Root]
        albumResponse.name must === ("Name 2.0")
      }
    }

    "POST v1/products/:context/:id/albums" - {
      "Creates a new album on an existing product" in new ProductFixture {
        val payload = AlbumPayload(name = "Simple Album",
          images = Some(Seq(ImagePayload(src = "http://test.com/test.jpg"))))
        val response = POST(s"v1/products/${context.name}/${prodForm.id}/albums", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Simple Album")
        albumResponse.images.head.src must === ("http://test.com/test.jpg")
      }
    }

    "GET v1/products/:context/:id/albums" - {
      "Retrieves all the albums associated with a product" in new ProductFixture {
        val response = GET(s"v1/products/${context.name}/${prodForm.id}/albums")
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sample Album")
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val payload = AlbumPayload(name = "Name 2.0", images = Some(Seq.empty))
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val response2 = GET(s"v1/products/${context.name}/${prodForm.id}/albums")
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[AlbumResponse.Root]
        albumResponse.name must === ("Name 2.0")
      }
    }

    "POST v1/skus/:context/:id/albums" - {
      "Creates a new album on an existing SKU" in new ProductFixture {
        val payload = AlbumPayload(name = "Sku Album",
          images = Some(Seq(ImagePayload(src = "http://test.com/test.jpg"))))
        val response = POST(s"v1/skus/${context.name}/${sku.code}/albums", payload)
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sku Album")
        albumResponse.images.head.src must === ("http://test.com/test.jpg")
      }
    }

    "GET v1/skus/:context/:id/albums" - {
      "Retrieves all the albums associated with a SKU" in new ProductFixture {
        val response = GET(s"v1/skus/${context.name}/${sku.code}/albums")
        response.status must === (StatusCodes.OK)

        val albumResponse = response.as[AlbumResponse.Root]
        albumResponse.images.length must === (1)

        albumResponse.name must === ("Sample Album")
        albumResponse.images.head.src must === ("http://lorem.png")
      }

      "Retrieves a correct version of an album after an update" in new ProductFixture {
        val payload = AlbumPayload(name = "Name 3.0", images = Some(Seq.empty))
        val response = PATCH(s"v1/albums/${context.name}/${album.formId}", payload)
        response.status must === (StatusCodes.OK)

        val response2 = GET(s"v1/skus/${context.name}/${sku.code}/albums")
        response2.status must === (StatusCodes.OK)

        val albumResponse = response2.as[AlbumResponse.Root]
        albumResponse.name must === ("Name 3.0")
      }
    }
  }

  trait Fixture {
    def createShadowAttr(key: String, attrType: String) =
      (key -> ("type" -> attrType) ~ ("ref" -> key))

    val imageJson = ("src" -> "http://lorem.png") ~ ("title" -> "lorem.png") ~ ("alt" -> "Lorem Ipsum")

    val albumFormAttrs = ("name" -> "Sample Album") ~ ("images" -> Seq(imageJson))
    val albumShadowAttrs = createShadowAttr("name", "string") ~ createShadowAttr("images", "images")

    val form = ObjectForm(kind = Album.kind, attributes = albumFormAttrs)
    val shadow = ObjectShadow(attributes = albumShadowAttrs)

    val (context, album) = (for {
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
      context    ← * <~ ObjectContexts.filterByName(SimpleContext.default).one.
        mustFindOr(ObjectContextNotFound(SimpleContext.default))
      ins        ← * <~ ObjectUtils.insert(form, shadow)
      album  ← * <~ Albums.create(Album(contextId = context.id,
        shadowId = ins.shadow.id, formId = ins.form.id, commitId = ins.commit.id))
    } yield (context, album)).run().futureValue.rightVal
  }

  trait ProductFixture extends Fixture {
    val (product, prodForm, prodShadow, sku, skuForm, skuShadow) = (for {
      simpleSku   ← * <~ SimpleSku("SKU-TEST", "Test SKU", "http://poop/", 9999, Currency.USD)
      skuForm     ← * <~ ObjectForms.create(simpleSku.create)
      sSkuShadow  ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow   ← * <~ ObjectShadows.create(sSkuShadow.create.copy(formId = skuForm.id))
      skuCommit   ← * <~ ObjectCommits.create(ObjectCommit(formId = skuForm.id,
                          shadowId = skuShadow.id))
      sku         ← * <~ Skus.create(Sku(contextId = context.id, formId = skuForm.id,
                          shadowId = skuShadow.id, commitId = skuCommit.id, code = "SKU-TEST"))
      _           ← * <~ ObjectLinks.create(ObjectLink(leftId = sku.shadowId,
                          rightId = album.shadowId, linkType = ObjectLink.SkuAlbum))

      simpleProd  ← * <~ SimpleProduct(title = "Test Product",
                          description = "Test product description", image = "image.png",
                          code = simpleSku.code)
      prodForm    ← * <~ ObjectForms.create(simpleProd.create)
      sProdShadow ← * <~ SimpleProductShadow(simpleProd)
      prodShadow  ← * <~ ObjectShadows.create(sProdShadow.create.copy(formId = prodForm.id))
      prodCommit  ← * <~ ObjectCommits.create(ObjectCommit(formId = prodForm.id,
                          shadowId = prodShadow.id))
      product     ← * <~ Products.create(Product(contextId = context.id, formId = prodForm.id,
                          shadowId = prodShadow.id, commitId = prodCommit.id))

      _           ← * <~ ObjectLinks.create(ObjectLink(leftId = product.shadowId,
                          rightId = album.shadowId, linkType = ObjectLink.ProductAlbum))
    } yield (product, prodForm, prodShadow, sku, skuForm, skuShadow)).runTxn().futureValue.rightVal
  }
}
