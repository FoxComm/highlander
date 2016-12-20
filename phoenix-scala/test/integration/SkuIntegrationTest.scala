import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.ArchiveFailures.VariantIsPresentInCarts
import failures.ObjectFailures.ObjectContextNotFound
import failures.ProductFailures.ProductVariantNotFoundForContext
import models.account.Scope
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import payloads.ImagePayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.OrderPayloads.CreateCart
import payloads.ProductPayloads.UpdateProductPayload
import payloads.ProductVariantPayloads.ProductVariantPayload
import responses.ProductVariantResponses.ProductVariantResponse
import responses.cord.CartResponse
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.Money.Currency
import utils.aliases._
import utils.db._
import utils.time.RichInstant

class SkuIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/skus/:context" - {
    "Creates a SKU successfully" in new Fixture {
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t"        → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)

      skusApi.create(makeSkuPayload("SKU-NEW-TEST", attrMap, None)).mustBeOk()
    }

    "Tries to create a SKU with no code" in new Fixture {
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t"        → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)

      skusApi
        .create(ProductVariantPayload(attributes = attrMap, albums = None))
        .mustFailWithMessage("SKU code not found in payload")
    }

    "Creates a SKU with an album" in new Fixture {
      val code       = "SKU-NEW-TEST"
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t" → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)

      val src          = "http://lorempixel/test.png"
      val imagePayload = ImagePayload(src = src)
      val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

      skusApi.create(makeSkuPayload(code, attrMap, Seq(albumPayload).some)).mustBeOk()

      val getResponse = skusApi(code).get().as[ProductVariantResponse.Root]
      getResponse.albums.length must === (1)
      getResponse.albums.head.images.length must === (1)
      getResponse.albums.head.images.head.src must === (src)
    }
  }

  "GET v1/skus/:context/:code" - {
    "Get a created SKU successfully" in new Fixture {
      val skuResponse = skusApi(sku.code).get().as[ProductVariantResponse.Root]
      val code        = skuResponse.attributes \ "code" \ "v"
      code.extract[String] must === (sku.code)

      val salePrice = skuResponse.attributes \ "salePrice" \ "v" \ "value"
      salePrice.extract[Int] must === (9999)
    }

    "Throws a 404 if given an invalid code" in new Fixture {
      val response = skusApi("INVALID-CODE").get()
      response.status must === (StatusCodes.NotFound)
    }
  }

  "PATCH v1/skus/:context/:code" - {
    "Adds a new attribute to the SKU" in new Fixture {
      val payload = ProductVariantPayload(attributes =
                                            Map("name" → (("t" → "string") ~ ("v" → "Test"))),
                                          albums = None)
      val skuResponse = skusApi(sku.code).update(payload).as[ProductVariantResponse.Root]

      (skuResponse.attributes \ "code" \ "v").extract[String] must === (sku.code)
      (skuResponse.attributes \ "name" \ "v").extract[String] must === ("Test")
      (skuResponse.attributes \ "salePrice" \ "v" \ "value").extract[Int] must === (9999)
    }

    "Updates the SKU's code" in new Fixture {
      val payload = ProductVariantPayload(attributes =
                                            Map("code" → (("t" → "string") ~ ("v" → "UPCODE"))),
                                          albums = None)
      skusApi(sku.code).update(payload).mustBeOk()

      val skuResponse = skusApi("upcode").get().as[ProductVariantResponse.Root]
      (skuResponse.attributes \ "code" \ "v").extract[String] must === ("UPCODE")

      (skuResponse.attributes \ "salePrice" \ "v" \ "value").extract[Int] must === (9999)
    }
  }

  "DELETE v1/products/:context/:id" - {
    "Archives SKU successfully" in new Fixture {
      val result = skusApi(sku.code).archive().as[ProductVariantResponse.Root]

      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "Successfully archives SKU which is linked to a product" in new FixtureWithProduct {
      private val updateProductPayload: UpdateProductPayload =
        UpdateProductPayload(attributes = Map(),
                             skus = Some(List(makeSkuPayload(sku.code, Map()))),
                             variants = None)
      productsApi(product.formId).update(updateProductPayload).mustBeOk

      val result = skusApi(sku.code).archive().as[ProductVariantResponse.Root]

      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "SKU Albums must be unlinked" in new Fixture {
      skusApi(sku.code).archive().as[ProductVariantResponse.Root].albums mustBe empty
    }

    "Responds with NOT FOUND when SKU is requested with wrong code" in new Fixture {
      skusApi("666").archive().mustFailWith404(ProductVariantNotFoundForContext("666", ctx.id))
    }

    "Responds with NOT FOUND when SKU is requested with wrong context" in new Fixture {
      implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)

      skusApi(sku.code)(donkeyContext)
        .archive()
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }

    "Returns error if SKU is present in carts" in new FixtureWithProduct {
      val cart = cartsApi.create(CreateCart(email = "yax@yax.com".some)).as[CartResponse]

      cartsApi(cart.referenceNumber).lineItems
        .add(Seq(UpdateLineItemsPayload(sku.code, 1)))
        .mustBeOk()

      skusApi(sku.code).archive().mustFailWith400(VariantIsPresentInCarts(sku.code))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    def makeSkuPayload(code: String,
                       attrMap: Map[String, Json],
                       albums: Option[Seq[AlbumPayload]] = None) = {
      val codeJson   = ("t"              → "string") ~ ("v" → code)
      val attributes = attrMap + ("code" → codeJson)
      ProductVariantPayload(attributes = attributes, albums = albums)
    }

    val (sku, skuForm, skuShadow) = (for {
      simpleSku       ← * <~ SimpleVariant("SKU-TEST", "Test SKU", 9999, Currency.USD)
      skuForm         ← * <~ ObjectForms.create(simpleSku.create)
      simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow       ← * <~ ObjectShadows.create(simpleSkuShadow.create.copy(formId = skuForm.id))
      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
      sku ← * <~ ProductVariants.create(
               ProductVariant(scope = Scope.current,
                              contextId = ctx.id,
                              code = simpleSku.code,
                              formId = skuForm.id,
                              shadowId = skuShadow.id,
                              commitId = skuCommit.id))
    } yield (sku, skuForm, skuShadow)).gimme
  }

  trait FixtureWithProduct extends Fixture {
    private val simpleProd = SimpleProductData(title = "Test Product",
                                               code = "TEST",
                                               description = "Test product description",
                                               image = "image.png",
                                               price = 5999)

    val product =
      Mvp.insertProductWithExistingSkus(LTree(au.token.scope), ctx.id, simpleProd, Seq(sku)).gimme
  }
}
