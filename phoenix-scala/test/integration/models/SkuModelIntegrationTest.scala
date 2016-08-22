package models

import concurrent.ExecutionContext.Implicits.global

import models.cord.lineitems.OrderLineItemSkus
import models.product.Mvp
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class SkuModelIntegrationTest extends IntegrationTestBase with TestObjectContext {

  "Skus" - {
    "a Postgres trigger creates a `order_line_item_skus` record after `skus` insert" in {
      val (product, liSku) = (for {
        product ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
        liSku   ← * <~ OrderLineItemSkus.safeFindBySkuId(product.skuId)
      } yield (product, liSku)).gimme

      product.skuId must === (liSku.skuId)
    }
  }
}
