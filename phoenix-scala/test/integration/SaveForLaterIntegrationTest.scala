import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ProductFailures.SkuNotFoundForContext
import failures.{AlreadySavedForLater, NotFoundFailure404}
import models.customer.Customer
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.{SaveForLater, SaveForLaters}
import responses.SaveForLaterResponse
import services.SaveForLaterManager.SavedForLater
import util._
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
      val emptyResponse = saveForLaterApi(customer.id).get()
      emptyResponse.status must === (StatusCodes.OK)
      emptyResponse.as[SavedForLater].result mustBe empty

      SaveForLaters.create(SaveForLater(customerId = customer.id, skuId = product.skuId)).gimme
      val notEmptyResponse = saveForLaterApi(customer.id).get()
      notEmptyResponse.status must === (StatusCodes.OK)
      notEmptyResponse.as[SavedForLater].result must === (roots)
    }

    "404 if customer is not found" in {
      val response = saveForLaterApi(666).get()
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 666).description)
    }
  }

  "POST v1/save-for-later/:customerId/:sku" - {
    "adds sku to customer's save for later list" in new Fixture {
      val response = saveForLaterApi(customer.id).create(product.code)
      response.status must === (StatusCodes.OK)
      response.as[SavedForLater].result must === (roots)

      val get = saveForLaterApi(customer.id).get()
      get.status must === (StatusCodes.OK)
      get.as[SavedForLater].result must === (roots)
    }

    "does not create duplicate records" in new Fixture {
      val create = saveForLaterApi(customer.id).create(product.code)
      create.status must === (StatusCodes.OK)
      val result = create.as[SavedForLater].result
      result must === (roots)

      val duplicate = saveForLaterApi(customer.id).create(product.code)
      duplicate.status must === (StatusCodes.BadRequest)
      duplicate.error must === (AlreadySavedForLater(customer.id, product.skuId).description)

      SaveForLaters.gimme must have size 1
    }

    "404 if customer is not found" in new Fixture {
      val response = saveForLaterApi(666).create(product.code)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 666).description)
    }

    "404 if sku is not found" in new Fixture {
      val response = saveForLaterApi(customer.id).create("NOPE")
      response.status must === (StatusCodes.NotFound)
      response.error must === (SkuNotFoundForContext("NOPE", SimpleContext.id).description)
    }
  }

  "DELETE v1/save-for-later/:id" - {
    "deletes save for later" in new Fixture {
      val sflId =
        saveForLaterApi(customer.id).create(product.code).as[SavedForLater].result.head.id

      val response = saveForLaterApi.delete(sflId)
      response.status must === (StatusCodes.NoContent)
    }

    "404 if save for later is not found" in {
      val response = saveForLaterApi.delete(666)
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SaveForLater, 666).description)
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
