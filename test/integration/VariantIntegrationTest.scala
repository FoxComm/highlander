import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures.ObjectContextNotFound
import models.objects.ObjectContexts
import models.product._
import org.json4s.JsonDSL._
import payloads.VariantPayloads._
import responses.VariantResponses.IlluminatedVariantResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse
import services.product.ProductManager
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._

class VariantIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/variants/:context" - {
    "Creates a variant successfully" in new Fixture {
      val response = POST(s"v1/variants/${context.name}", createVariantPayload)
      response.status must ===(StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must ===(0)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("Color")
    }

    "Creates a variant with a value successfully" in new Fixture {
      val payload  = createVariantPayload.copy(values = Some(Seq(createVariantValuePayload)))
      val response = POST(s"v1/variants/${context.name}", payload)
      response.status must ===(StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must ===(1)
      variantResponse.values.head.name must ===("Red")
      variantResponse.values.head.swatch must ===(Some("ff0000"))

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("Color")
    }
  }

  "GET v1/variants/:context/:id" - {
    "Gets a created variant successfully" in new VariantFixture {
      val response = GET(s"v1/variants/${context.name}/${variant.variant.variantId}")
      response.status must ===(StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must ===(2)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("Size")

      val names = variantResponse.values.map(_.name).toSet
      names must ===(Set("Small", "Large"))
    }

    "Throws a 404 if given an invalid id" in new Fixture {
      val response = GET(s"v1/variants/${context.name}/123")
      response.status must ===(StatusCodes.NotFound)
    }
  }

  "PATCH v1/variants/:context/:id" - {
    "Updates the name of the variant successfully" in new VariantFixture {
      val payload = VariantPayload(
          attributes = Map("name" → (("t" → "wtring") ~ ("v" → "New Size"))), values = None)
      val response = PATCH(s"v1/variants/${context.name}/${variant.variant.variantId}", payload)

      response.status must ===(StatusCodes.OK)

      val variantResponse = response.as[IlluminatedVariantResponse.Root]
      variantResponse.values.length must ===(2)

      val name = variantResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("New Size")

      val names = variantResponse.values.map(_.name).toSet
      names must ===(Set("Small", "Large"))
    }
  }

  "POST v1/variants/:context/:id/values" - {
    "Creates a variant value successfully" in new Fixture {
      val response = POST(s"v1/variants/${context.name}", createVariantPayload)
      response.status must ===(StatusCodes.OK)
      val variantResponse = response.as[IlluminatedVariantResponse.Root]

      val response2 = POST(s"v1/variants/${context.name}/${variantResponse.id}/values",
                           createVariantValuePayload)
      response2.status must ===(StatusCodes.OK)
      val valueResponse = response2.as[IlluminatedVariantValueResponse.Root]
      valueResponse.swatch must ===(Some("ff0000"))
    }
  }

  trait Fixture {
    val createVariantPayload = VariantPayload(
        attributes = Map("name" → (("t" → "string") ~ ("v" → "Color"))), values = None)

    val createVariantValuePayload = VariantValuePayload(
        name = Some("Red"), swatch = Some("ff0000"))

    val context = (for {
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
    } yield context).runTxn().futureValue.rightVal
  }

  trait VariantFixture extends Fixture {
    val simpleProd = SimpleProductData(title = "Test Product",
                                       code = "TEST",
                                       description = "Test product description",
                                       image = "image.png",
                                       price = 5999)

    val simpleSizeVariant = SimpleCompleteVariant(
        variant = SimpleVariant("Size"),
        variantValues = Seq(SimpleVariantValue("Small", ""), SimpleVariantValue("Large", "")))

    val (product, variant) = (for {
      productData ← * <~ Mvp.insertProduct(context.id, simpleProd)
      product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id,
                                                                     productData.productId)
      variant ← * <~ Mvp.insertVariantWithValues(context.id, product.shadowId, simpleSizeVariant)
    } yield (product, variant)).run().futureValue.rightVal
  }
}
