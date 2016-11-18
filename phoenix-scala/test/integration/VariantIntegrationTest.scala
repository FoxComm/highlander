import java.time.Instant

import com.github.tminglei.slickpg.LTree
import failures.ArchiveFailures.LinkArchivedSkuFailure
import failures.ProductFailures.VariantNotFoundForContext
import models.inventory.Skus
import models.product._
import org.json4s.JsonDSL._
import payloads.VariantPayloads._
import responses.VariantResponses.IlluminatedVariantResponse.{Root ⇒ VariantRoot}
import responses.VariantValueResponses.IlluminatedVariantValueResponse.{Root ⇒ ValueRoot}
import services.product.ProductManager
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.MockedApis
import utils.Money.Currency
import utils.db._

class VariantIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with MockedApis
    with BakedFixtures {

  "POST v1/variants/:context" - {
    "Creates a variant successfully" in new Fixture {
      val variantResponse = variantsApi.create(createVariantPayload).as[VariantRoot]
      variantResponse.values.length must === (0)

      (variantResponse.attributes \ "name" \ "v").extract[String] must === ("Color")
    }

    "Creates a variant with a value successfully" in new Fixture {
      val payload         = createVariantPayload.copy(values = Some(Seq(createVariantValuePayload)))
      val variantResponse = variantsApi.create(payload).as[VariantRoot]
      variantResponse.values.length must === (1)
      private val value = variantResponse.values.head
      value.name must === ("Red")
      value.swatch must === (Some("ff0000"))
      value.skuCodes must === (Seq(skus.head.code))

      (variantResponse.attributes \ "name" \ "v").extract[String] must === ("Color")
    }

    "Fails when trying to create variant with archived sku as value" in new ArchivedSkusFixture {
      variantsApi
        .create(archivedSkuVariantPayload)
        .mustFailWith400(LinkArchivedSkuFailure(Variant, 10, archivedSkuCode))
    }
  }

  "GET v1/variants/:context/:id" - {
    "Gets a created variant successfully" in new VariantFixture {
      val variantResponse = variantsApi(variant.variant.variantFormId).get().as[VariantRoot]
      variantResponse.values.length must === (2)

      (variantResponse.attributes \ "name" \ "v").extract[String] must === ("Size")

      variantResponse.values.map(_.name).toSet must === (Set("Small", "Large"))

      val valueSkus = variantResponse.values.map(_.skuCodes).toSet
      valueSkus must contain theSameElementsAs skus.map(s ⇒ Seq(s.code))
    }

    "Throws a 404 if given an invalid id" in new Fixture {
      variantsApi(123).get().mustFailWith404(VariantNotFoundForContext(123, ctx.id))
    }
  }

  "PATCH v1/variants/:context/:id" - {
    "Updates the name of the variant successfully" in new VariantFixture {
      val payload = VariantPayload(values = None,
                                   attributes =
                                     Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))))
      val response = variantsApi(variant.variant.variantFormId).update(payload).as[VariantRoot]
      response.values.length must === (2)

      (response.attributes \ "name" \ "v").extract[String] must === ("New Size")

      response.values.map(_.name).toSet must === (Set("Small", "Large"))
    }

    "Fails when trying to attach archived SKU to the variant" in new ArchivedSkusFixture {
      var payload = VariantPayload(values = Some(Seq(archivedSkuVariantValuePayload)),
                                   attributes =
                                     Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))))

      variantsApi(variant.variant.variantFormId)
        .update(payload)
        .mustFailWith400(
            LinkArchivedSkuFailure(Variant, variant.variant.variantFormId, archivedSkuCode))
    }
  }

  "POST v1/variants/:context/:id/values" - {
    "Creates a variant value successfully" in new Fixture {
      val variantResponse = variantsApi.create(createVariantPayload).as[VariantRoot]

      val valueResponse =
        variantsApi(variantResponse.id).createValues(createVariantValuePayload).as[ValueRoot]

      valueResponse.swatch must === (Some("ff0000"))
      valueResponse.skuCodes must === (Seq(skus.head.code))
    }

    "Fails when attaching archived SKU to variant as variant value" in new ArchivedSkusFixture {
      val variantResponse = variantsApi.create(createVariantPayload).as[VariantRoot]

      variantsApi(variantResponse.id)
        .createValues(archivedSkuVariantValuePayload)
        .mustFailWith400(LinkArchivedSkuFailure(Variant, variantResponse.id, archivedSkuCode))
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    val scope = LTree(au.token.scope)

    val createVariantPayload = VariantPayload(attributes =
                                                Map("name" → (("t" → "string") ~ ("v" → "Color"))),
                                              values = None)

    val testSkus = Seq(SimpleSku("SKU-TST", "SKU test", 1000, Currency.USD, active = true),
                       SimpleSku("SKU-TS2", "SKU test 2", 1000, Currency.USD, active = true))

    val skus = Mvp.insertSkus(scope, ctx.id, testSkus).gimme

    val createVariantValuePayload = VariantValuePayload(name = Some("Red"),
                                                        swatch = Some("ff0000"),
                                                        skuCodes = Seq(skus.head.code))
  }

  trait VariantFixture extends Fixture {
    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val simpleSizeVariant = SimpleCompleteVariant(
        variant = SimpleVariant("Size"),
        variantValues = Seq(SimpleVariantValue("Small", "", Seq(skus.head.code)),
                            SimpleVariantValue("Large", "", Seq(skus(1).code))))

    val (product, variant) = (for {
      productData ← * <~ Mvp.insertProduct(ctx.id, simpleProd)
      product     ← * <~ Products.mustFindById404(productData.productId)
      variant     ← * <~ Mvp.insertVariantWithValues(scope, ctx.id, product, simpleSizeVariant)
    } yield (product, variant)).gimme
  }

  trait ArchivedSkusFixture extends VariantFixture {

    val archivedSkus = (for {
      archivedSkus ← * <~ skus.map { sku ⇒
                      Skus.update(sku, sku.copy(archivedAt = Some(Instant.now)))
                    }
    } yield archivedSkus).gimme

    val archivedSku     = archivedSkus.head
    val archivedSkuCode = archivedSku.code
    val archivedSkuVariantValuePayload =
      createVariantValuePayload.copy(skuCodes = Seq(archivedSkuCode))
    val archivedSkuVariantPayload =
      createVariantPayload.copy(values = Some(Seq(archivedSkuVariantValuePayload)))
  }
}
