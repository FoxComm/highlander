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
      val optionResponse = variantsApi.create(createProductOptionPayload).as[OptionRoot]
      optionResponse.values.length must === (0)

      (optionResponse.attributes \ "name" \ "v").extract[String] must === ("Color")
    }

    "Creates a productOption with a value successfully" in new Fixture {
      val payload         = createProductOptionPayload.copy(values = Some(Seq(createVariantValuePayload)))
      val variantResponse = variantsApi.create(payload).as[OptionRoot]
      variantResponse.values.length must === (1)
      private val value = variantResponse.values.head
      value.name must === ("Red")
      value.swatch must === (Some("ff0000"))
      value.skuCodes.value must === (Seq(skus.head.code))

      (variantResponse.attributes \ "name" \ "v").extract[String] must === ("Color")
    }

    "Fails when trying to create productOption with archived variant as value" in new ArchivedSkusFixture {
      variantsApi
        .create(archivedSkuVariantPayload)
        .mustFailWith400(LinkArchivedVariantFailure(ProductOption, 10, archivedSkuCode))
    }
  }

  "GET v1/options/:context/:id" - {
    "Gets a created productOption successfully" in new VariantFixture {
      val variantResponse = variantsApi(variant.variant.variantFormId).get().as[OptionRoot]
      variantResponse.values.length must === (2)

      (variantResponse.attributes \ "name" \ "v").extract[String] must === ("Size")

      variantResponse.values.map(_.name).toSet must === (Set("Small", "Large"))

      val valueSkus = variantResponse.values.flatMap(_.skuCodes).toSet
      valueSkus must contain theSameElementsAs skus.map(s ⇒ Seq(s.code))
    }

    "Throws a 404 if given an invalid id" in new Fixture {
      variantsApi(123).get().mustFailWith404(ProductOptionNotFoundForContext(123, ctx.id))
    }
  }

  "PATCH v1/options/:context/:id" - {
    "Updates the name of the productOption successfully" in new VariantFixture {
      val payload = ProductOptionPayload(values = None,
                                         attributes =
                                           Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))))
      val response = variantsApi(variant.variant.variantFormId).update(payload).as[OptionRoot]
      response.values.length must === (2)

      (response.attributes \ "name" \ "v").extract[String] must === ("New Size")

      response.values.map(_.name).toSet must === (Set("Small", "Large"))
    }

    "Fails when trying to attach archived SKU to the productOption" in new ArchivedSkusFixture {
      var payload = ProductOptionPayload(values = Some(Seq(archivedSkuVariantValuePayload)),
                                         attributes =
                                           Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))))

      variantsApi(variant.variant.variantFormId)
        .update(payload)
        .mustFailWith400(LinkArchivedVariantFailure(ProductOption,
                                                    variant.variant.variantFormId,
                                                    archivedSkuCode))
    }
  }

  "POST v1/options/:context/:id/values" - {
    "Creates a productOption value successfully" in new Fixture {
      val variantResponse = variantsApi.create(createProductOptionPayload).as[OptionRoot]

      val valueResponse =
        variantsApi(variantResponse.id).createValues(createVariantValuePayload).as[ValueRoot]

      valueResponse.swatch must === (Some("ff0000"))
      valueResponse.skuCodes.value must === (Seq(skus.head.code))
    }

    "Fails when attaching archived SKU to productOption as productOption value" in new ArchivedSkusFixture {
      val variantResponse = variantsApi.create(createProductOptionPayload).as[OptionRoot]

      variantsApi(variantResponse.id)
        .createValues(archivedSkuVariantValuePayload)
        .mustFailWith400(
            LinkArchivedVariantFailure(ProductOption, variantResponse.id, archivedSkuCode))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val scope = Scope.current

    val createProductOptionPayload = ProductOptionPayload(
        attributes = Map("name" → (("t" → "string") ~ ("v" → "Color"))),
        values = None)

    val testSkus = Seq(SimpleVariant("SKU-TST", "SKU test", 1000, Currency.USD, active = true),
                       SimpleVariant("SKU-TS2", "SKU test 2", 1000, Currency.USD, active = true))

    val skus = Mvp.insertVariants(scope, ctx.id, testSkus).gimme

    val createVariantValuePayload = ProductOptionValuePayload(name = Some("Red"),
                                                              swatch = Some("ff0000"),
                                                              skuCodes = Seq(skus.head.code))
  }

  trait VariantFixture extends Fixture {
    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val simpleSizeVariant = SimpleCompleteOption(
        option = SimpleProductOption("Size"),
        productValues = Seq(SimpleProductValue("Small", "", Seq(skus.head.code)),
                            SimpleProductValue("Large", "", Seq(skus(1).code))))

    val (product, variant) = (for {
      productData ← * <~ Mvp.insertProduct(ctx.id, simpleProd)
      product     ← * <~ Products.mustFindById404(productData.productId)
      variant     ← * <~ Mvp.insertVariantWithValues(scope, ctx.id, product, simpleSizeVariant)
    } yield (product, variant)).gimme
  }

  trait ArchivedSkusFixture extends VariantFixture {

    val archivedSkus = (for {
      archivedSkus ← * <~ skus.map { sku ⇒
                      ProductVariants.update(sku, sku.copy(archivedAt = Some(Instant.now)))
                    }
    } yield archivedSkus).gimme

    val archivedSku     = archivedSkus.head
    val archivedSkuCode = archivedSku.code
    val archivedSkuVariantValuePayload =
      createVariantValuePayload.copy(skuCodes = Seq(archivedSkuCode))
    val archivedSkuVariantPayload =
      createProductOptionPayload.copy(values = Some(Seq(archivedSkuVariantValuePayload)))
  }
}
