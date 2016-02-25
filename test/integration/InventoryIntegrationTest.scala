import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import models.inventory.{InventorySummaries ⇒ Summaries, InventorySummary ⇒ Summary, _}
import models.product.{ProductContexts, Mvp, SimpleContext}
import responses.InventoryResponses._
import services.NotFoundFailure404
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds.Factories

class InventoryIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET /v1/inventory/skus/:code/:warehouseId" - {
    "returns SKU summary" in new Fixture {
      val response = GET(s"v1/inventory/skus/${sku.code}/${warehouse.id}")
      response.status must ===(StatusCodes.OK)
      val skuResponse = response.as[Seq[SkuDetailsResponse.Root]]

      skuResponse must have size 4
      skuResponse must contain allOf(
        // sellable
        SkuDetailsResponse.Root(Sellable, SkuCounts(sellable.onHand, sellable.onHold, sellable.reserved, sellable
          .safetyStock, sellable.availableForSale, sellable.availableForSale * Mvp.priceAsInt(sku,skuShadow))),
        // backorder
        SkuDetailsResponse.Root(Backorder, SkuCounts(backorder.onHand, backorder.onHold, backorder.reserved, backorder
          .safetyStock, backorder.availableForSale, backorder.availableForSale * Mvp.priceAsInt(sku, skuShadow))),
        // preorder
        SkuDetailsResponse.Root(Preorder, SkuCounts(preorder.onHand, preorder.onHold, preorder.reserved, preorder
          .safetyStock, preorder.availableForSale, preorder.availableForSale * Mvp.priceAsInt(sku, skuShadow))),
        // nonsellable
        SkuDetailsResponse.Root(NonSellable, SkuCounts(nonsellable.onHand, nonsellable.onHold, nonsellable.reserved,
          nonsellable.safetyStock, nonsellable.availableForSale, nonsellable.availableForSale * Mvp.priceAsInt(sku, skuShadow)))
        )
    }

    "errors on wrong SKU code" in {
      val response = GET("v1/inventory/skus/NOPE/1")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Sku, "NOPE").description)
    }

    "errors on wrong warehouse id" in {
      val productContext = ProductContexts.mustFindById404(SimpleContext.id).run().futureValue.rightVal
      val product = Mvp.insertProduct(productContext.id, Factories.products.head).run().futureValue.rightVal
      val response = GET(s"v1/inventory/skus/${product.code}/666")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Warehouse, 666).description)
    }
  }

  "GET /v1/inventory/skus/:skuCode/summary" - {
    "returns SKU summary across multiple warehouses" in new SummaryFixture {
      val response = GET(s"v1/inventory/skus/${sku.code}/summary")
      response.status must ===(StatusCodes.OK)
      val summaries = response.as[Seq[SkuSummaryResponse.Root]]
      summaries must have size 2
      summaries must contain allOf(
        SkuSummaryResponse.Root(warehouse, SkuCounts(sellable.onHand, sellable.onHold, sellable.reserved, sellable
          .safetyStock, sellable.availableForSale, sellable.availableForSale * Mvp.priceAsInt(sku, skuShadow))),
        SkuSummaryResponse.Root(warehouse2, SkuCounts(sellable2.onHand, sellable2.onHold, sellable2.reserved, sellable2
          .safetyStock, sellable2.availableForSale, sellable2.availableForSale * Mvp.priceAsInt(sku, skuShadow)))
        )
    }

    "errors on wrong SKU code" in {
      val response = GET("v1/inventory/skus/NOPE/summary")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Sku, "NOPE").description)
    }
  }

  trait Fixture {
    val (productContext, product, sku, skuShadow, warehouse, sellable, backorder, preorder, nonsellable) = (for {
      productContext ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
      product     ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      sku         ← * <~ Skus.mustFindById404(product.skuId)
      skuShadow   ← * <~ SkuShadows.mustFindById404(product.skuShadowId)
      warehouse   ← * <~ Warehouses.create(Warehouse(name = "first"))
      sellable    ← * <~ Summaries.create(Summary.build(warehouse.id, sku.id, 100, 30, 20, 10.some, Sellable))
      backorder   ← * <~ Summaries.create(Summary.build(warehouse.id, sku.id, 101, 31, 21, None, Backorder))
      preorder    ← * <~ Summaries.create(Summary.build(warehouse.id, sku.id, 102, 32, 22, None, Preorder))
      nonsellable ← * <~ Summaries.create(Summary.build(warehouse.id, sku.id, 103, 33, 23, None, NonSellable))
    } yield (productContext, product, sku, skuShadow, warehouse, sellable, backorder, preorder, nonsellable)).run().futureValue.rightVal
  }

  trait SummaryFixture extends Fixture {
    val (warehouse2, sellable2) = (for {
      warehouse2 ← * <~ Warehouses.create(Warehouse(name = "second"))
      sellable2  ← * <~ Summaries.create(Summary.build(warehouse2.id, sku.id, 1100, 130, 120, 110.some, Sellable))
      _          ← * <~ Summaries.create(Summary.build(warehouse2.id, sku.id, 1101, 131, 121, None, Backorder))
      _          ← * <~ Summaries.create(Summary.build(warehouse2.id, sku.id, 1102, 132, 122, None, Preorder))
      _          ← * <~ Summaries.create(Summary.build(warehouse2.id, sku.id, 1103, 133, 123, None, NonSellable))
      _          ← * <~ Warehouses.create(Warehouse(name = "empty"))
    } yield (warehouse2, sellable2)).run().futureValue.rightVal
  }
}
