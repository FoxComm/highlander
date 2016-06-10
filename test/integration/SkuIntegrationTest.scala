import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures.ObjectContextNotFound
import models.StoreAdmins
import models.inventory.{Sku, Skus}
import models.objects.{ObjectCommit, ObjectCommits, ObjectContexts, ObjectForms, ObjectShadows}
import models.product.{SimpleContext, SimpleSku, SimpleSkuShadow}
import org.json4s.JsonDSL._
import payloads.SkuPayloads.{CreateSkuPayload, UpdateSkuPayload}
import responses.SkuResponses.{FullSkuResponse, IlluminatedSkuResponse}
import util.IntegrationTestBase
import utils.Money.Currency
import utils.db._
import utils.db.DbResultT._

class SkuIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "POST v1/skus/:context" - {
    "Creates a SKU successfully" in new Fixture {
      val priceValue = ("currency" → "USD") ~ ("value" → 9999)
      val priceJson  = ("t" -> "price") ~ ("v" -> priceValue)
      val attrMap    = Map("price" -> priceJson)
      val payload    = CreateSkuPayload("SKU-NEW-TEST", attrMap)

      val response = POST(s"v1/skus/${context.name}", payload)
      response.status must ===(StatusCodes.OK)
    }
  }

  "GET v1/skus/:context/:code" - {
    "Get a created SKU successfully" in new Fixture {
      val response = GET(s"v1/skus/${context.name}/${sku.code}")
      response.status must ===(StatusCodes.OK)

      val skuResponse = response.as[IlluminatedSkuResponse.Root]
      skuResponse.code must ===(sku.code)

      val salePrice = skuResponse.attributes \ "salePrice" \ "v" \ "value"
      salePrice.extract[Int] must ===(9999)
    }

    "Throws a 404 if given an invalid code" in new Fixture {
      val response = GET(s"v1/skus/${context.name}/INVALID-CODE")
      response.status must ===(StatusCodes.NotFound)
    }
  }

  "PATCH v1/skus/:context/:code" - {
    "Adds a new attribute to the SKU" in new Fixture {
      val updatePayload =
        UpdateSkuPayload(attributes = Map("name" → (("t" → "string") ~ ("v" → "Test"))))

      val response = PATCH(s"v1/skus/${context.name}/${sku.code}", updatePayload)
      response.status must ===(StatusCodes.OK)

      val skuResponse = response.as[IlluminatedSkuResponse.Root]
      skuResponse.code must ===(sku.code)

      val name = skuResponse.attributes \ "name" \ "v"
      name.extract[String] must ===("Test")
      val salePrice = skuResponse.attributes \ "salePrice" \ "v" \ "value"
      salePrice.extract[Int] must ===(9999)
    }
  }

  "GET v1/skus/full/:context/:code" - {
    "returns a full SKU successfully" in new Fixture {
      val response = GET(s"v1/skus/full/${context.name}/${sku.code}")
      response.status must ===(StatusCodes.OK)

      val skuResponse = response.as[FullSkuResponse.Root]
      skuResponse.form.attributes must ===(skuForm.attributes)
      skuResponse.shadow.attributes must ===(skuShadow.attributes)
    }
  }

  trait Fixture {
    val (context, sku, skuForm, skuShadow) = (for {
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin).run().futureValue.rightVal
      context ← * <~ ObjectContexts
                 .filterByName(SimpleContext.default)
                 .mustFindOneOr(ObjectContextNotFound(SimpleContext.default))
      simpleSku ← * <~ SimpleSku("SKU-TEST",
                                 "Test SKU",
                                 "http://poop/",
                                 9999,
                                 Currency.USD)
      skuForm         ← * <~ ObjectForms.create(simpleSku.create)
      simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow       ← * <~ ObjectShadows.create(simpleSkuShadow.create.copy(formId = skuForm.id))
      skuCommit ← * <~ ObjectCommits.create(
                     ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
      sku ← * <~ Skus.create(
               Sku(contextId = context.id,
                   code = simpleSku.code,
                   formId = skuForm.id,
                   shadowId = skuShadow.id,
                   commitId = skuCommit.id))
    } yield (context, sku, skuForm, skuShadow)).runTxn().futureValue.rightVal
  }
}
