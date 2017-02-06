import java.time.Instant

import failures.ArchiveFailures.LinkArchivedVariantFailure
import failures.ProductFailures.ProductOptionNotFoundForContext
import models.account.Scope
import models.inventory.ProductVariants
import models.product._
import org.json4s.JsonDSL._
import payloads.ProductOptionPayloads._
import responses.ProductOptionResponses.ProductOptionResponse.{Root ⇒ OptionRoot}
import responses.ProductValueResponses.ProductValueResponse.{Root ⇒ ValueRoot}
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.MockedApis
import utils.Money.Currency
import utils.db._

class ProductOptionIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockedApis
    with BakedFixtures {

  "POST v1/options/:context" - {
    "Creates a product option successfully" in new Fixture {
      val optionResponse = productOptionsApi.create(createProductOptionPayload).as[OptionRoot]
      optionResponse.values mustBe empty

      optionResponse.attributes.getString("name") must === ("Color")
    }

    "Creates a productOption with a value successfully" in new Fixture {
      val payload = createProductOptionPayload.copy(values = Some(Seq(createVariantValuePayload)))
      private val productOption =
        productOptionsApi.create(payload).as[OptionRoot].values.onlyElement
      productOption.name must === ("Red")
      productOption.swatch must === (Some("ff0000"))
      productOption.skuCodes must === (Seq(variants.head.code))

      productOptionsApi.create(payload).as[OptionRoot].attributes.getString("name") must === (
          "Color")
    }

    "Fails when trying to create productOption with archived variant as value" in new ArchivedSkusFixture {
      productOptionsApi
        .create(archivedSkuVariantPayload)
        .mustFailWith400(LinkArchivedVariantFailure(ProductOption, 10, archivedSkuCode))
    }
  }

  "GET v1/options/:context/:id" - {
    "Gets a created productOption successfully" in new VariantFixture {
      val optionResponse = productOptionsApi(variant.option.optionFormId).get().as[OptionRoot]
      optionResponse.values must have size 2
      optionResponse.attributes.getString("name") must === ("Size")
      optionResponse.values.map(_.name).toSet must === (Set("Small", "Large"))

      val valueSkus = optionResponse.values.map(_.skuCodes).toSet
      valueSkus must contain theSameElementsAs variants.map(_.code)
    }

    "Throws a 404 if given an invalid id" in new Fixture {
      productOptionsApi(123).get().mustFailWith404(ProductOptionNotFoundForContext(123, ctx.id))
    }
  }

  "PATCH v1/options/:context/:id" - {
    "Updates the name of the productOption successfully" in new VariantFixture {
      val payload = ProductOptionPayload(values = None,
                                         attributes =
                                           Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))))
      val response = productOptionsApi(variant.option.optionFormId).update(payload).as[OptionRoot]
      response.values.length must === (2)

      (response.attributes \ "name" \ "v").extract[String] must === ("New Size")

      response.values.map(_.name).toSet must === (Set("Small", "Large"))
    }

    "Fails when trying to attach archived SKU to the productOption" in new ArchivedSkusFixture {
      var payload = ProductOptionPayload(values = Some(Seq(archivedSkuVariantValuePayload)),
                                         attributes =
                                           Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))))

      productOptionsApi(variant.option.optionFormId)
        .update(payload)
        .mustFailWith400(LinkArchivedVariantFailure(ProductOption,
                                                    variant.option.optionFormId,
                                                    archivedSkuCode))
    }
  }

  "POST v1/options/:context/:id/values" - {
    "Creates a productOption value successfully" in new Fixture {
      val variantResponse = productOptionsApi.create(createProductOptionPayload).as[OptionRoot]

      val valueResponse =
        productOptionsApi(variantResponse.id).createValues(createVariantValuePayload).as[ValueRoot]

      valueResponse.swatch must === (Some("ff0000"))
      valueResponse.skuCodes must === (Seq(variants.head.code))
    }

    "Fails when attaching archived SKU to productOption as productOption value" in new ArchivedSkusFixture {
      val optionResponse = productOptionsApi.create(createProductOptionPayload).as[OptionRoot]

      productOptionsApi(optionResponse.id)
        .createValues(archivedSkuVariantValuePayload)
        .mustFailWith400(
            LinkArchivedVariantFailure(ProductOption, optionResponse.id, archivedSkuCode))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val scope = Scope.current

    val createProductOptionPayload = ProductOptionPayload(
        attributes = Map("name" → (("t" → "string") ~ ("v" → "Color"))),
        values = None)

    val testSkus = Seq(SimpleVariant("SKU-TST", "SKU test", 1000, Currency.USD, active = true),
                       SimpleVariant("SKU-TS2", "SKU test 2", 1000, Currency.USD, active = true))

    val variants = Mvp.insertProductVariants(scope, ctx.id, testSkus).gimme

    val createVariantValuePayload = ProductOptionValuePayload(name = Some("Red"),
                                                              swatch = Some("ff0000"),
                                                              skus = Seq(variants.head.code))
  }

  trait VariantFixture extends Fixture {
    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val simpleSizeVariant = SimpleCompleteOption(
        option = SimpleProductOption("Size"),
        productValues = Seq(SimpleProductOptionValue("Small", "", Seq(variants.head.code)),
                            SimpleProductOptionValue("Large", "", Seq(variants(1).code))))

    val (product, variant) = (for {
      productData ← * <~ Mvp.insertProduct(ctx.id, simpleProd)
      product     ← * <~ Products.mustFindById404(productData.productId)
      variant     ← * <~ Mvp.insertProductOptionWithValues(scope, ctx.id, product, simpleSizeVariant)
    } yield (product, variant)).gimme
  }

  trait ArchivedSkusFixture extends VariantFixture {

    val archivedVariants = DbResultT
      .sequence(variants.map { variant ⇒
        ProductVariants.update(variant, variant.copy(archivedAt = Some(Instant.now)))
      })
      .gimme

    val archivedVariant = archivedVariants.head
    val archivedSkuCode = archivedVariant.code
    val archivedSkuVariantValuePayload =
      createVariantValuePayload.copy(skus = Seq(archivedSkuCode))
    val archivedSkuVariantPayload =
      createProductOptionPayload.copy(values = Some(Seq(archivedSkuVariantValuePayload)))
  }
}
