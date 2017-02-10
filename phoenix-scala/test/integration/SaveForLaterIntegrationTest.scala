import failures.ProductFailures.ProductVariantNotFoundForContext
import failures.{AlreadySavedForLater, NotFoundFailure404}
import models.account._
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.{SaveForLater, SaveForLaters}
import responses.SaveForLaterResponse
import services.SaveForLaterManager.SavedForLater
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class SaveForLaterIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

  "GET v1/save-for-later/:customerId" - {
    "shows all save for later items for customer" in new Fixture {
      saveForLaterApi(customer.accountId).get().as[SavedForLater].result mustBe empty

      SaveForLaters
        .create(SaveForLater(accountId = customer.accountId, productVariantId = product.variantId))
        .gimme
      saveForLaterApi(customer.accountId).get().as[SavedForLater].result must === (roots)
    }

    "404 if customer is not found" in {
      saveForLaterApi(666).get().mustFailWith404(NotFoundFailure404(User, 666))
    }
  }

  "POST v1/save-for-later/:customerId/:sku" - {
    "adds sku to customer's save for later list" in new Fixture {
      saveForLaterApi(customer.accountId).create(product.code).as[SavedForLater].result must === (
          roots)

      saveForLaterApi(customer.accountId).get().as[SavedForLater].result must === (roots)
    }

    "does not create duplicate records" in new Fixture {
      val result =
        saveForLaterApi(customer.accountId).create(product.code).as[SavedForLater].result
      result must === (roots)

      saveForLaterApi(customer.accountId)
        .create(product.code)
        .mustFailWith400(AlreadySavedForLater(customer.accountId, product.variantId))

      SaveForLaters.gimme must have size 1
    }

    "404 if customer is not found" in new Fixture {
      saveForLaterApi(666).create(product.code).mustFailWith404(NotFoundFailure404(User, 666))
    }

    "404 if sku is not found" in new Fixture {
      saveForLaterApi(customer.accountId)
        .create("NOPE")
        .mustFailWith404(ProductVariantNotFoundForContext("NOPE", SimpleContext.id))
    }
  }

  "DELETE v1/save-for-later/:id" - {
    "deletes save for later" in new Fixture {
      val saveForLaterId =
        saveForLaterApi(customer.accountId).create(product.code).as[SavedForLater].result.head.id

      saveForLaterApi.delete(saveForLaterId).mustBeEmpty()
    }

    "404 if save for later is not found" in {
      saveForLaterApi.delete(666).mustFailWith404(NotFoundFailure404(SaveForLater, 666))
    }
  }

  trait Fixture extends StoreAdmin_Seed with Customer_Seed {
    val (product, productContext) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      product        ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
    } yield (product, productContext)).gimme

    def roots = Seq(SaveForLaterResponse.forSkuId(product.variantId, productContext.id).gimme)
  }
}
