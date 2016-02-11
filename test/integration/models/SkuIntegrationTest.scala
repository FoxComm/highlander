package models

import models.product.Skus

import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.seeds.Seeds
import Seeds.Factories

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table

class SkuIntegrationTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "Skus" - {
    "a Postgres trigger creates a `order_line_item_skus` record after `skus` insert" in {
      val (sku, liSku) = (for {
        sku ← * <~ Skus.create(Factories.skus.head).run().futureValue.rightVal
        liSku ← * <~ OrderLineItemSkus.safeFindBySkuId(sku.id).toXor
      } yield (sku, liSku)).runTxn().futureValue.rightVal

      sku.id must === (liSku.skuId)
    }
  }
}

