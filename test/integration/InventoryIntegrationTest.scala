import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import Extensions._
import cats.implicits._
import failures.NotFoundFailure404
import failures.ProductFailures.SkuNotFoundForContext
import models.inventory._
import models.product.{Mvp, SimpleContext}
import models.objects._
import responses.InventoryResponses._
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.seeds.Seeds.Factories
import utils.seeds.generators._

class InventoryIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth {

  "GET /v1/inventory/skus/:code/:warehouseId" - {
    "returns SKU summary" in new Fixture {
      val response = GET(s"v1/inventory/skus/${sku.code}/${warehouse1.id}")
      response.status must ===(StatusCodes.OK)
      val skuResponse = response.as[Seq[SkuDetailsResponse.Root]]

      skuResponse must have size 4
      skuResponse must contain allOf (
          // sellable
          SkuDetailsResponse.Root(
              Sellable,
              SkuCounts(sellable.onHand,
                        sellable.onHold,
                        sellable.reserved,
                        sellable.safetyStock.some,
                        sellable.availableForSale,
                        sellable.availableForSale * Mvp.priceAsInt(
                            skuForm, skuShadow))),
          // backorder
          SkuDetailsResponse.Root(
              Backorder,
              SkuCounts(backorder.onHand,
                        backorder.onHold,
                        backorder.reserved,
                        None,
                        backorder.availableForSale,
                        backorder.availableForSale * Mvp.priceAsInt(
                            skuForm, skuShadow))),
          // preorder
          SkuDetailsResponse.Root(
              Preorder,
              SkuCounts(preorder.onHand,
                        preorder.onHold,
                        preorder.reserved,
                        None,
                        preorder.availableForSale,
                        preorder.availableForSale * Mvp.priceAsInt(
                            skuForm, skuShadow))),
          // nonsellable
          SkuDetailsResponse.Root(NonSellable,
                                  SkuCounts(nonsellable.onHand,
                                            nonsellable.onHold,
                                            nonsellable.reserved,
                                            None,
                                            nonsellable.availableForSale,
                                            nonsellable.availableForSale * Mvp
                                              .priceAsInt(skuForm, skuShadow)))
      )
    }

    "errors on wrong SKU code" in {
      val response = GET("v1/inventory/skus/NOPE/1")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(
          SkuNotFoundForContext("NOPE", SimpleContext.id).description)
    }

    "errors on wrong warehouse id" in {
      val productContext = ObjectContexts
        .mustFindById404(SimpleContext.id)
        .run()
        .futureValue
        .rightVal
      val product = Mvp
        .insertProduct(productContext.id, Factories.products.head)
        .run()
        .futureValue
        .rightVal
      val response = GET(s"v1/inventory/skus/${product.code}/666")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(NotFoundFailure404(Warehouse, 666).description)
    }
  }

  "GET /v1/inventory/skus/:skuCode/summary" - {
    "returns SKU summary across multiple warehouses" in new SummaryFixture {
      val response = GET(s"v1/inventory/skus/${sku.code}/summary")
      response.status must ===(StatusCodes.OK)
      val summaries = response.as[Seq[SellableSkuSummaryResponse.Root]]
      summaries must have size 2
      summaries must contain allOf (
          SellableSkuSummaryResponse.Root(
              warehouse1,
              SkuCounts(sellable.onHand,
                        sellable.onHold,
                        sellable.reserved,
                        sellable.safetyStock.some,
                        sellable.availableForSale,
                        sellable.availableForSale * Mvp.priceAsInt(
                            skuForm, skuShadow))),
          SellableSkuSummaryResponse.Root(
              warehouse2,
              SkuCounts(sellable2.onHand,
                        sellable2.onHold,
                        sellable2.reserved,
                        sellable2.safetyStock.some,
                        sellable2.availableForSale,
                        sellable2.availableForSale * Mvp.priceAsInt(
                            skuForm, skuShadow)))
      )
    }

    "errors on wrong SKU code" in {
      val response = GET("v1/inventory/skus/NOPE/summary")
      response.status must ===(StatusCodes.NotFound)
      response.error must ===(
          SkuNotFoundForContext("NOPE", SimpleContext.id).description)
    }
  }

  trait Fixture extends InventorySummaryGenerator {
    val (productContext,
         product,
         sku,
         skuForm,
         skuShadow,
         warehouse1,
         sellable,
         backorder,
         preorder,
         nonsellable) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      product ← * <~ Mvp.insertProduct(productContext.id,
                                       Factories.products.head)
      sku ← * <~ Skus.mustFindById404(product.skuId)
      skuForm ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      warehouse1 ← * <~ Warehouses.create(Warehouse(name = "first"))
      summaries ← * <~ generateInventory(sku.id, warehouse1.id)
      (sellable, backorder, preorder, nonsellable) = summaries
    } yield
      (productContext,
       product,
       sku,
       skuForm,
       skuShadow,
       warehouse1,
       sellable,
       backorder,
       preorder,
       nonsellable)).run().futureValue.rightVal
  }

  trait SummaryFixture extends Fixture {
    val (warehouse2, sellable2) = (for {
      warehouse2 ← * <~ Warehouses.create(Warehouse(name = "second"))
      summaries ← * <~ generateInventory(sku.id, warehouse2.id)
      _ ← * <~ Warehouses.create(Warehouse(name = "empty"))
    } yield (warehouse2, summaries._1)).run().futureValue.rightVal
  }
}
