import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ArchiveFailures.LinkArchivedSkuFailure
import models.inventory.Skus
import models.product._
import org.json4s.JsonDSL._
import payloads.VariantPayloads._
import responses.VariantResponses.IlluminatedVariantResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse.Root
import services.product.ProductManager
import util._
import utils.MockedApis
import utils.Money.Currency
import utils.db._

class VariantIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with MockedApis {

  "POST v1/variants/:context" - {
    "Creates a variant successfully" in new Fixture {
      val response = POST(s"v1/variants/${ctx.name}", createVariantPayload)
      response.status must === (StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must === (0)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must === ("Color")
    }

    "Creates a variant with a value successfully" in new Fixture {
      val payload  = createVariantPayload.copy(values = Some(Seq(createVariantValuePayload)))
      val response = POST(s"v1/variants/${ctx.name}", payload)
      response.status must === (StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must === (1)
      private val value: Root = variantResponse.values.head
      value.name must === ("Red")
      value.swatch must === (Some("ff0000"))
      value.skuCodes must === (Seq(skus.head.code))

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must === ("Color")
    }

    "Fails when trying to create variant with archived sku as value" in new ArchivedSkusFixture {
      val response = POST(s"v1/variants/${ctx.name}", archivedSkuVariantPayload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (LinkArchivedSkuFailure(Variant, 10, archivedSkuCode).description)
    }
  }

  "GET v1/variants/:context/:id" - {
    "Gets a created variant successfully" in new VariantFixture {
      val response = GET(s"v1/variants/${ctx.name}/${variant.variant.variantFormId}")
      response.status must === (StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must === (2)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must === ("Size")

      val names = variantResponse.values.map(_.name).toSet
      names must === (Set("Small", "Large"))

      val valueSkus = variantResponse.values.map(_.skuCodes).toSet
      valueSkus must contain theSameElementsAs skus.map(s ⇒ Seq(s.code))
    }

    "Throws a 404 if given an invalid id" in new Fixture {
      val response = GET(s"v1/variants/${ctx.name}/123")
      response.status must === (StatusCodes.NotFound)
    }
  }

  "PATCH v1/variants/:context/:id" - {
    "Updates the name of the variant successfully" in new VariantFixture {
      val payload = VariantPayload(attributes =
                                     Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))),
                                   values = None)
      val response = PATCH(s"v1/variants/${ctx.name}/${variant.variant.variantFormId}", payload)

      response.status must === (StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must === (2)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must === ("New Size")

      val names = variantResponse.values.map(_.name).toSet
      names must === (Set("Small", "Large"))
    }

    "Fails when trying to attach archived SKU to the variant" in new ArchivedSkusFixture {
      var payload = VariantPayload(attributes =
                                     Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))),
                                   values = Some(Seq(archivedSkuVariantValuePayload)))
      val response = PATCH(s"v1/variants/${ctx.name}/${variant.variant.variantFormId}", payload)

      response.status must === (StatusCodes.BadRequest)
      response.error must === (LinkArchivedSkuFailure(Variant,
                                                      variant.variant.variantFormId,
                                                      archivedSkuCode).description)
    }
  }

  "POST v1/variants/:context/:id/values" - {
    "Creates a variant value successfully" in new Fixture {
      val response = POST(s"v1/variants/${ctx.name}", createVariantPayload)
      response.status must === (StatusCodes.OK)
      val variantResponse = response.as[IlluminatedVariantResponse.Root]

      val response2 =
        POST(s"v1/variants/${ctx.name}/${variantResponse.id}/values", createVariantValuePayload)
      response2.status must === (StatusCodes.OK)
      val valueResponse = response2.as[IlluminatedVariantValueResponse.Root]

      valueResponse.swatch must === (Some("ff0000"))
      valueResponse.skuCodes must === (Seq(skus.head.code))
    }

    "Fails when attaching archived SKU to variant as variant value" in new ArchivedSkusFixture {
      val response = POST(s"v1/variants/${ctx.name}", createVariantPayload)
      response.status must === (StatusCodes.OK)
      val variantResponse = response.as[IlluminatedVariantResponse.Root]

      val response2 = POST(s"v1/variants/${ctx.name}/${variantResponse.id}/values",
                           archivedSkuVariantValuePayload)

      response2.status must === (StatusCodes.BadRequest)
      response2.error must === (
          LinkArchivedSkuFailure(Variant, variantResponse.id, archivedSkuCode).description)
    }
  }

  trait Fixture {
    val createVariantPayload = VariantPayload(attributes =
                                                Map("name" → (("t" → "string") ~ ("v" → "Color"))),
                                              values = None)

    val testSkus = Seq(new SimpleSku("SKU-TST", "SKU test", 1000, Currency.USD, true),
                       new SimpleSku("SKU-TS2", "SKU test 2", 1000, Currency.USD, true))

    val skus = Mvp.insertSkus(ctx.id, testSkus).gimme

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
      product     ← * <~ ProductManager.mustFindProductByContextAndId404(ctx.id, productData.productId)
      variant     ← * <~ Mvp.insertVariantWithValues(ctx.id, product, simpleSizeVariant)
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
