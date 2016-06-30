import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.ProductFailures.SkuNotFoundForContext
import failures.{AlreadySavedForLater, NotFoundFailure404}
import models.customer.{Customer, Customers}
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.{SaveForLater, SaveForLaters}
import responses.SaveForLaterResponse
import services.SaveForLaterManager.SavedForLater
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class SaveForLaterIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET v1/save-for-later/:customerId" - {
    "shows all save for later items for customer" in new Fixture {
      val emptyResponse = GET(s"v1/save-for-later/${customer.id}")
      emptyResponse.status must === (StatusCodes.OK)
      emptyResponse.as[SavedForLater].result mustBe empty

      SaveForLaters.create(SaveForLater(customerId = customer.id, skuId = product.skuId)).gimme
      val notEmptyResponse = GET(s"v1/save-for-later/${customer.id}")
      notEmptyResponse.status must === (StatusCodes.OK)
      notEmptyResponse.as[SavedForLater].result must === (roots)
    }

    "404 if customer is not found" in {
      val response = GET(s"v1/save-for-later/666")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 666).description)
    }
  }

  "POST v1/save-for-later/:customerId/:sku" - {
    "adds sku to customer's save for later list" in new Fixture {
      val response = POST(s"v1/save-for-later/${customer.id}/${product.code}")
      response.status must === (StatusCodes.OK)
      response.as[SavedForLater].result must === (roots)

      val get = GET(s"v1/save-for-later/${customer.id}")
      get.status must === (StatusCodes.OK)
      get.as[SavedForLater].result must === (roots)
    }

    "does not create duplicate records" in new Fixture {
      val create = POST(s"v1/save-for-later/${customer.id}/${product.code}")
      create.status must === (StatusCodes.OK)
      val result = create.as[SavedForLater].result
      result must === (roots)

      val duplicate = POST(s"v1/save-for-later/${customer.id}/${product.code}")
      duplicate.status must === (StatusCodes.BadRequest)
      duplicate.error must === (AlreadySavedForLater(customer.id, product.skuId).description)

      SaveForLaters.gimme must have size 1
    }

    "404 if customer is not found" in new Fixture {
      val response = POST(s"v1/save-for-later/666/${product.skuId}")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(Customer, 666).description)
    }

    "404 if sku is not found" in new Fixture {
      val response = POST(s"v1/save-for-later/${customer.id}/NOPE")
      response.status must === (StatusCodes.NotFound)
      response.error must === (SkuNotFoundForContext("NOPE", SimpleContext.id).description)
    }
  }

  "DELETE v1/save-for-later/:id" - {
    "deletes save for later" in new Fixture {
      val sflId =
        POST(s"v1/save-for-later/${customer.id}/${product.code}").as[SavedForLater].result.head.id

      val response = DELETE(s"v1/save-for-later/$sflId")
      response.status must === (StatusCodes.NoContent)
    }

    "404 if save for later is not found" in {
      val response = DELETE("v1/save-for-later/666")
      response.status must === (StatusCodes.NotFound)
      response.error must === (NotFoundFailure404(SaveForLater, 666).description)
    }
  }

  trait Fixture {
    val (customer, product, productContext) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customer       ← * <~ Customers.create(Factories.customer)
      product        ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
    } yield (customer, product, productContext)).gimme

    def roots = Seq(SaveForLaterResponse.forSkuId(product.skuId, productContext.id).gimme)
  }
}
