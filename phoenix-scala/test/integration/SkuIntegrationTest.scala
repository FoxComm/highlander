import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.ObjectFailures.ObjectContextNotFound
import failures.ProductFailures.SkuNotFoundForContext
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonAST.JNothing
import org.json4s.JsonDSL._
import payloads.SkuPayloads.SkuPayload
import responses.SkuResponses.SkuResponse
import util.{IntegrationTestBase, PhoenixAdminApi}
import util.fixtures.BakedFixtures
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
      val priceJson  = ("t" → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)
      val payload    = makeSkuPayload("SKU-NEW-TEST", attrMap)

      val response = skusApi.create(payload)
      response.status must === (StatusCodes.OK)
    }

    "Tries to create a SKU with no code" in new Fixture {
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t" → "price") ~ ("v" → priceValue)
      val attrMap    = Map("price" → priceJson)
      val payload    = SkuPayload(attrMap)

      val response = skusApi.create(payload)
      response.status must === (StatusCodes.BadRequest)
    }
  }

  "GET v1/skus/:context/:code" - {
    "Get a created SKU successfully" in new Fixture {
      val response = skusApi(sku.code).get()
      response.status must === (StatusCodes.OK)

      val skuResponse = response.as[SkuResponse.Root]
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
      val updatePayload =
        SkuPayload(attributes = Map("name" → (("t" → "string") ~ ("v" → "Test"))))

      val response = skusApi(sku.code).update(updatePayload)
      response.status must === (StatusCodes.OK)

      val skuResponse = response.as[SkuResponse.Root]
      val code        = skuResponse.attributes \ "code" \ "v"
      code.extract[String] must === (sku.code)

      val name = skuResponse.attributes \ "name" \ "v"
      name.extract[String] must === ("Test")
      val salePrice = skuResponse.attributes \ "salePrice" \ "v" \ "value"
      salePrice.extract[Int] must === (9999)
    }

    "Updates the SKU's code" in new Fixture {
      val updatePayload =
        SkuPayload(attributes = Map("code" → (("t" → "string") ~ ("v" → "UPCODE"))))

      val response = skusApi(sku.code).update(updatePayload)
      response.status must === (StatusCodes.OK)

      val response2 = skusApi("upcode").get()
      response2.status must === (StatusCodes.OK)

      val skuResponse = response2.as[SkuResponse.Root]
      val code        = skuResponse.attributes \ "code" \ "v"
      code.extract[String] must === ("UPCODE")

      val salePrice = skuResponse.attributes \ "salePrice" \ "v" \ "value"
      salePrice.extract[Int] must === (9999)
    }
  }

  "DELETE v1/products/:context/:id" - {
    "Archives SKU successfully" in new Fixture {
      val response = skusApi(sku.code).archive()

      response.status must === (StatusCodes.OK)

      val result = response.as[SkuResponse.Root]
      withClue(result.archivedAt.value → Instant.now) {
        result.archivedAt.value.isBeforeNow === true
      }
    }

    "SKU Albums must be unlinked" in new Fixture {
      val response = skusApi(sku.code).archive()

      response.status must === (StatusCodes.OK)
      val result = response.as[SkuResponse.Root]
      result.albums mustBe empty
    }

    "Responds with NOT FOUND when SKU is requested with wrong code" in new Fixture {
      val response = skusApi("666").archive()

      response.status must === (StatusCodes.NotFound)
      response.error must === (SkuNotFoundForContext("666", ctx.id).description)
    }

    "Responds with NOT FOUND when SKU is requested with wrong context" in new Fixture {
      implicit val donkeyContext = ObjectContext(name = "donkeyContext", attributes = JNothing)
      val response               = skusApi(sku.code)(donkeyContext).archive()

      response.status must === (StatusCodes.NotFound)
      response.error must === (ObjectContextNotFound("donkeyContext").description)
    }
  }

  trait Fixture extends StoreAdmin_Seed {
    def makeSkuPayload(code: String, attrMap: Map[String, Json]) = {
      val codeJson = ("t" → "string") ~ ("v" → code)
      SkuPayload(attrMap + ("code" → codeJson))
    }

    val (sku, skuForm, skuShadow) = (for {
      simpleSku       ← * <~ SimpleSku("SKU-TEST", "Test SKU", 9999, Currency.USD)
      skuForm         ← * <~ ObjectForms.create(simpleSku.create)
      simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow       ← * <~ ObjectShadows.create(simpleSkuShadow.create.copy(formId = skuForm.id))
      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
      sku ← * <~ Skus.create(
               Sku(contextId = ctx.id,
                   code = simpleSku.code,
                   formId = skuForm.id,
                   shadowId = skuShadow.id,
                   commitId = skuCommit.id))
    } yield (sku, skuForm, skuShadow)).gimme
  }
}
