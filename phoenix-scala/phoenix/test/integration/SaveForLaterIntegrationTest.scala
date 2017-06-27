import core.failures.NotFoundFailure404
import objectframework.models.ObjectContexts
import phoenix.failures.AlreadySavedForLater
import phoenix.failures.ProductFailures.SkuNotFoundForContext
import phoenix.models.account._
import phoenix.models.product.{Mvp, SimpleContext}
import phoenix.models.{SaveForLater, SaveForLaters}
import phoenix.responses.SaveForLaterResponse
import phoenix.utils.seeds.Factories
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import core.db._

class SaveForLaterIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with DefaultJwtAdminAuth
    with BakedFixtures {

  "GET v1/save-for-later/:customerId" - {
    "shows all save for later items for customer" in new Fixture {
      saveForLaterApi(customer.accountId).get().as[Seq[SaveForLaterResponse]] mustBe empty

      SaveForLaters
        .create(SaveForLater(accountId = customer.accountId, skuId = product.skuId))
        .gimme
      saveForLaterApi(customer.accountId).get().as[Seq[SaveForLaterResponse]] must === (roots)
    }

    "404 if customer is not found" in {
      saveForLaterApi(666).get().mustFailWith404(NotFoundFailure404(User, 666))
    }
  }

  "POST v1/save-for-later/:customerId/:sku" - {
    "adds sku to customer's save for later list" in new Fixture {
      saveForLaterApi(customer.accountId).create(product.code).as[Seq[SaveForLaterResponse]] must === (roots)

      saveForLaterApi(customer.accountId).get().as[Seq[SaveForLaterResponse]] must === (roots)
    }

    "does not create duplicate records" in new Fixture {
      val result = saveForLaterApi(customer.accountId).create(product.code).as[Seq[SaveForLaterResponse]]
      result must === (roots)

      saveForLaterApi(customer.accountId)
        .create(product.code)
        .mustFailWith400(AlreadySavedForLater(customer.accountId, product.skuId))

      SaveForLaters.gimme must have size 1
    }

    "404 if customer is not found" in new Fixture {
      saveForLaterApi(666).create(product.code).mustFailWith404(NotFoundFailure404(User, 666))
    }

    "404 if sku is not found" in new Fixture {
      saveForLaterApi(customer.accountId)
        .create("NOPE")
        .mustFailWith404(SkuNotFoundForContext("NOPE", SimpleContext.id))
    }
  }

  "DELETE v1/save-for-later/:id" - {
    "deletes save for later" in new Fixture {
      val saveForLaterId =
        saveForLaterApi(customer.accountId).create(product.code).as[Seq[SaveForLaterResponse]].head.id

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

    def roots = Seq(SaveForLaterResponse.forSkuId(product.skuId, productContext.id).gimme)
  }
}
