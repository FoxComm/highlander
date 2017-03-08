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

  "GET v1/variants/:context/:code" - {
    "Get a created variant successfully" in new Fixture {
      val variantResponse =
        productVariantsApi(variantForm.id).get().as[ProductVariantResponse.Root]

      variantResponse.attributes.code must === (variant.code)
      variantResponse.attributes.salePrice must === (9999)
    }

    "Throws a 404 if given an invalid code" in new Fixture {
      productVariantsApi(99).get().mustFailWith404(ProductVariantNotFoundForContext("99", ctx.id))
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
      variantResponse.attributes.salePrice must === (9999)
    }

    "Updates variant's code" in new Fixture {
      val payload = ProductVariantPayload(attributes =
                                            Map("code" → (("t" → "string") ~ ("v" → "UPCODE"))),
                                          albums = None)
      productVariantsApi(variantForm.id).update(payload).mustBeOk()

      val variantResponse =
        productVariantsApi(variantForm.id).get().as[ProductVariantResponse.Root]
      variantResponse.attributes.code must === ("UPCODE")
      variantResponse.attributes.salePrice must === (9999)
    }
  }

  "DELETE v1/products/:context/:id" - {
    "Archives variant successfully" in new Fixture {
      val result = productVariantsApi(variantForm.id).archive().as[ProductVariantResponse.Root]

      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow mustBe true
      }
    }

    "Successfully archives variant which is linked to a product" in new Fixture {
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

    "Returns error if variant is present in carts" in new Fixture {
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

    private val simpleProd = SimpleProductData(title = "Test Product",
                                               code = "TEST",
                                               description = "Test product description",
                                               image = "image.png",
                                               price = 5999)

    val (product, variant, variantForm, variantShadow) = (for {
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

      product ← * <~ Mvp.insertProductWithExistingVariants(LTree(au.token.scope),
                                                           ctx.id,
                                                           simpleProd,
                                                           Seq(variant))
    } yield (product, variant, variantForm, variantShadow)).gimme
  }
}
