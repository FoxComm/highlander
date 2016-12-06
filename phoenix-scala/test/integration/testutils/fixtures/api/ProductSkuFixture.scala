package testutils.fixtures.api

import scala.util.Random
import faker.Lorem
import org.json4s.JsonDSL._
import org.scalatest.SuiteMixin
import payloads.ProductPayloads.CreateProductPayload
import payloads.SkuPayloads.SkuPayload
import services.inventory.SkuManager
import testutils.PayloadHelpers.tv
import testutils._
import testutils.apis.PhoenixAdminApi

trait ApiFixtures extends SuiteMixin with HttpSupport with PhoenixAdminApi { self: FoxSuite ⇒

  trait ProductSku_ApiFixture {
    private val productCode = s"testprod_${Lorem.numerify("####")}"
    private val skuCode     = s"$productCode-sku_${Lorem.letterify("????").toUpperCase}"
    private val skuPrice    = Random.nextInt(20000) + 100

    private val skuPayload = SkuPayload(
        Map("code"        → tv(skuCode),
            "title"       → tv(skuCode.capitalize),
            "salePrice"   → tv(("currency" → "USD") ~ ("value" → skuPrice), "price"),
            "retailPrice" → tv(("currency" → "USD") ~ ("value" → skuPrice), "price")))

    private val productPayload =
      CreateProductPayload(
          attributes =
            Map("name" → tv(productCode.capitalize), "title" → tv(productCode.capitalize)),
          skus = Seq(skuPayload),
          variants = None)

    productsApi.create(productPayload).mustBeOk()

    val sku = SkuManager.mustFindSkuByContextAndCode(ctx.id, skuCode).gimme
  }

}
