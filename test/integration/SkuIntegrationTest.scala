import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ObjectFailures.ObjectContextNotFound
import models.StoreAdmins
import models.inventory.{Sku, Skus}
import models.objects.{ObjectCommit, ObjectCommits, ObjectContexts, ObjectForms, ObjectShadows}
import models.product.{SimpleContext, SimpleSku, SimpleSkuShadow}
import responses.SkuResponses.FullSkuResponse
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import org.json4s.jackson.JsonMethods._

class SkuIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {
  "GET v1/skus/full/:context/:code" - {
    "returns a full SKU successfully" in new Fixture {
      val response = GET(s"v1/skus/full/${context.name}/${sku.code}")
      response.status must === (StatusCodes.OK)

      val skuResponse = response.as[FullSkuResponse.Root]
      skuResponse.form.attributes must === (skuForm.attributes)
      skuResponse.shadow.attributes must === (skuShadow.attributes)
    }
  }

  trait Fixture {
    val (context, sku, skuForm, skuShadow) = (for {
      storeAdmin      ← * <~ StoreAdmins.create(authedStoreAdmin).run().futureValue.rightVal
      context         ← * <~ ObjectContexts.filterByName(SimpleContext.default).one.
                                mustFindOr(ObjectContextNotFound(SimpleContext.default))
      simpleSku       ← * <~ SimpleSku("SKU-TEST", "Test SKU", "http://poop/", 9999, Currency.USD)
      skuForm         ← * <~ ObjectForms.create(simpleSku.create)
      simpleSkuShadow ← * <~ SimpleSkuShadow(simpleSku)
      skuShadow       ← * <~ ObjectShadows.create(simpleSkuShadow.create.copy(formId = skuForm.id))
      skuCommit       ← * <~ ObjectCommits.create(ObjectCommit(formId = skuForm.id, shadowId = skuShadow.id))
      sku             ← * <~ Skus.create(Sku(contextId = context.id, code = simpleSku.code, formId = skuForm.id,
                             shadowId = skuShadow.id, commitId = skuCommit.id))
    } yield (context, sku, skuForm, skuShadow)).runTxn().futureValue.rightVal
  }
}
