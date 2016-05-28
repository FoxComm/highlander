package models

import models.objects._
import models.order.lineitems.OrderLineItemSkus
import models.product.{Mvp, SimpleContext}
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.seeds.Seeds.Factories

class SkuModelIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Skus" - {
    "a Postgres trigger creates a `order_line_item_skus` record after `skus` insert" in {
      val (product, liSku) = (for {
        context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
        product ← * <~ Mvp.insertProduct(context.id, Factories.products.head)
        liSku ← * <~ OrderLineItemSkus.safeFindBySkuId(product.skuId).toXor
      } yield (product, liSku)).runTxn().futureValue.rightVal

      product.skuId must ===(liSku.skuId)
    }
  }
}
