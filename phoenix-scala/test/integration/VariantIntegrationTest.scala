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
import payloads.CartPayloads.CreateCart
import payloads.ImagePayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
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

class VariantIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "POST v1/variants/:context" - {
    "Creates a variant successfully" in new Fixture {
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t"        → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)

      productVariantsApi.create(makeVariantPayload("SKU-NEW-TEST", attrMap, None)).mustBeOk()
    }

    "Tries to create a variant with no code" in new Fixture {
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t"        → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)

      productVariantsApi
        .create(ProductVariantPayload(attributes = attrMap, albums = None))
        .mustFailWithMessage("SKU code not found in payload")
    }

    "Creates a variant with an album" in new Fixture {
      val code       = "SKU-NEW-TEST"
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t" → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)

      val src          = "http://lorempixel/test.png"
      val imagePayload = ImagePayload(src = src)
      val albumPayload = AlbumPayload(name = "Default".some, images = Seq(imagePayload).some)

      val resp =
        productVariantsApi.create(makeVariantPayload(code, attrMap, Seq(albumPayload).some))
      resp.mustBeOk()
      val createResponse = resp.as[ProductVariantResponse.Root]

      val getResponse = productVariantsApi(createResponse.id).get().as[ProductVariantResponse.Root]
      getResponse.albums.length must === (1)
      getResponse.albums.head.images.length must === (1)
      getResponse.albums.head.images.head.src must === (src)
    }
  }

  "GET v1/variants/:context/:code" - {
    "Get a created variant successfully" in new Fixture {
      val variantResponse =
        productVariantsApi(variantForm.id).get().as[ProductVariantResponse.Root]
      variantResponse.attributes.code must === (variant.code)

      val salePrice = variantResponse.attributes \ "salePrice" \ "v" \ "value"
      salePrice.extract[Int] must === (9999)
    }

    "Throws a 404 if given an invalid code" in new Fixture {
      val response = productVariantsApi(99).get()
      response.status must === (StatusCodes.NotFound)
    }
  }

  "PATCH v1/variants/:context/:code" - {
    "Adds a new attribute to the variant" in new Fixture {
      val payload = ProductVariantPayload(attributes =
                                            Map("name" → (("t" → "string") ~ ("v" → "Test"))),
                                          albums = None)

      val variantResponse =
        productVariantsApi(variantForm.id).update(payload).as[ProductVariantResponse.Root]

      variantResponse.attributes.code must === (variant.code)
      variantResponse.attributes.getString("name") must === ("Test")
      (variantResponse.attributes \ "salePrice" \ "v" \ "value").extract[Int] must === (9999)
    }

    "Updates variant's code" in new Fixture {
      val payload = ProductVariantPayload(attributes =
                                            Map("code" → (("t" → "string") ~ ("v" → "UPCODE"))),
                                          albums = None)
      productVariantsApi(variantForm.id).update(payload).mustBeOk()

      val variantResponse =
        productVariantsApi(variantForm.id).get().as[ProductVariantResponse.Root]
      variantResponse.attributes.code must === ("UPCODE")
      (variantResponse.attributes \ "salePrice" \ "v" \ "value").extract[Int] must === (9999)
    }
  }

  "DELETE v1/products/:context/:id" - {
    "Archives variant successfully" in new Fixture {
      val result = productVariantsApi(variantForm.id).archive().as[ProductVariantResponse.Root]

      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "Successfully archives variant which is linked to a product" in new FixtureWithProduct {
      private val updateProductPayload: UpdateProductPayload =
        UpdateProductPayload(attributes = Map(),
                             variants = Some(List(makeVariantPayload(variant.code, Map()))),
                             options = None)
      productsApi(product.formId).update(updateProductPayload).mustBeOk

      val result = productVariantsApi(variantForm.id).archive().as[ProductVariantResponse.Root]

      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "Variant albums must be unlinked" in new Fixture {
      productVariantsApi(variantForm.id)
        .archive()
        .as[ProductVariantResponse.Root]
        .albums mustBe empty
    }

    // FIXME: we actually moved to form id...
    "Responds with NOT FOUND when variant is requested with wrong code" in new Fixture {
      productVariantsApi(666)
        .archive()
        .mustFailWith404(ProductVariantNotFoundForContext("666", ctx.id))
    }

    "Responds with NOT FOUND when variant is requested with wrong context" in new Fixture {
      productVariantsApi(variantForm.id)(
          ObjectContext(name = "donkeyContext", attributes = JNothing))
        .archive()
        .mustFailWith404(ObjectContextNotFound("donkeyContext"))
    }

    "Returns error if variant is present in carts" in new FixtureWithProduct {
      val cart = cartsApi.create(CreateCart(email = "yax@yax.com".some)).as[CartResponse]

      cartsApi(cart.referenceNumber).lineItems
        .add(Seq(UpdateLineItemsPayload(variant.formId, 1)))
        .mustBeOk()

      productVariantsApi(variantForm.id)
        .archive()
        .mustFailWith400(VariantIsPresentInCarts(variant.code))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    def makeVariantPayload(code: String,
                           attrMap: Map[String, Json],
                           albums: Option[Seq[AlbumPayload]] = None) = {
      val codeJson   = ("t"              → "string") ~ ("v" → code)
      val attributes = attrMap + ("code" → codeJson)
      ProductVariantPayload(attributes = attributes, albums = albums)
    }

    val (variant, variantForm, variantShadow) = (for {
      simpleVariant       ← * <~ SimpleVariant("SKU-TEST", "Test SKU", 9999, Currency.USD)
      variantForm         ← * <~ ObjectForms.create(simpleVariant.create)
      simpleVariantShadow ← * <~ SimpleVariantShadow(simpleVariant)
      variantShadow ← * <~ ObjectShadows.create(
                         simpleVariantShadow.create.copy(formId = variantForm.id))
      variantCommit ← * <~ ObjectCommits.create(
                         ObjectCommit(formId = variantForm.id, shadowId = variantShadow.id))
      variant ← * <~ ProductVariants.create(
                   ProductVariant(scope = Scope.current,
                                  contextId = ctx.id,
                                  code = simpleVariant.code,
                                  formId = variantForm.id,
                                  shadowId = variantShadow.id,
                                  commitId = variantCommit.id))
      _ ← * <~ ProductVariantSkus.create(
             ProductVariantSku(variantFormId = variantForm.id,
                               skuId = variantForm.id,
                               skuCode = simpleVariant.code))
    } yield (variant, variantForm, variantShadow)).gimme
  }

  trait FixtureWithProduct extends Fixture {
    private val simpleProd = SimpleProductData(title = "Test Product",
                                               code = "TEST",
                                               description = "Test product description",
                                               image = "image.png",
                                               price = 5999)

    val product = Mvp
      .insertProductWithExistingVariants(LTree(au.token.scope), ctx.id, simpleProd, Seq(variant))
      .gimme
  }
}
