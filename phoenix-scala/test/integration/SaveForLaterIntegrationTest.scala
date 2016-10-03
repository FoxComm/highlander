import akka.http.scaladsl.model.StatusCodes

import util.Extensions._
import failures.ProductFailures.SkuNotFoundForContext
import failures.{AlreadySavedForLater, NotFoundFailure404}
import models.customer.Customer
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.{SaveForLater, SaveForLaters}
import responses.SaveForLaterResponse
import services.SaveForLaterManager.SavedForLater
import util._
import util.apis.PhoenixAdminApi
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class SaveForLaterIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "GET v1/save-for-later/:customerId" - {
    "shows all save for later items for customer" in new Fixture {
      saveForLaterApi(customer.id).get().as[SavedForLater].result mustBe empty

      SaveForLaters.create(SaveForLater(customerId = customer.id, skuId = product.skuId)).gimme
      saveForLaterApi(customer.id).get().as[SavedForLater].result must === (roots)
    }

    "404 if customer is not found" in {
      saveForLaterApi(666).get().mustFailWith404(NotFoundFailure404(Customer, 666))
    }
  }

  "POST v1/save-for-later/:customerId/:sku" - {
    "adds sku to customer's save for later list" in new Fixture {
      saveForLaterApi(customer.id).create(product.code).as[SavedForLater].result must === (roots)

      saveForLaterApi(customer.id).get().as[SavedForLater].result must === (roots)
    }

    "does not create duplicate records" in new Fixture {
      val result = saveForLaterApi(customer.id).create(product.code).as[SavedForLater].result
      result must === (roots)

      saveForLaterApi(customer.id)
        .create(product.code)
        .mustFailWith400(AlreadySavedForLater(customer.id, product.skuId))

      SaveForLaters.gimme must have size 1
    }

    "404 if customer is not found" in new Fixture {
      saveForLaterApi(666).create(product.code).mustFailWith404(NotFoundFailure404(Customer, 666))
    }

    "404 if sku is not found" in new Fixture {
      saveForLaterApi(customer.id)
        .create("NOPE")
        .mustFailWith404(SkuNotFoundForContext("NOPE", SimpleContext.id))
    }
  }

  "DELETE v1/save-for-later/:id" - {
    "deletes save for later" in new Fixture {
      val saveForLaterId =
        saveForLaterApi(customer.id).create(product.code).as[SavedForLater].result.head.id

      saveForLaterApi.delete(saveForLaterId).mustBeEmpty()
    }

    "404 if save for later is not found" in {
      saveForLaterApi.delete(666).mustFailWith404(NotFoundFailure404(SaveForLater, 666))
    }
  }

  trait Fixture extends Customer_Seed {
    val (product, productContext) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      product        ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
    } yield (product, productContext)).gimme

    def roots = Seq(SaveForLaterResponse.forSkuId(product.skuId, productContext.id).gimme)
  }
}
