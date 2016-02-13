package utils.seeds

import models.inventory.{InventorySummaries, InventorySummary, Warehouse, Warehouses}
import models.product.{ProductContexts, ProductContext, Products, Product, 
  ProductShadows, ProductShadow, Sku, Skus, SkuShadows, SkuShadow,
  SimpleContext, SimpleProduct, SimpleProductShadow, SimpleSku, SimpleSkuShadow, SimpleProductData}
import utils.Money.Currency
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

trait ProductSeeds {

  type Products = (Sku, Sku, Sku, Sku, Sku, Sku, Sku)

  def insertProduct(contextId: Int, p: SimpleProductData): DbResultT[SimpleProductData] = for {
    simpleProduct ← * <~ SimpleProduct(p.title, p.description, p.image, p.sku, p.isActive)
    product ← * <~ Products.create(simpleProduct.create)
    simpleShadow ← * <~ SimpleProductShadow(contextId, product.id)
    productShadow ← * <~ ProductShadows.create(simpleShadow.create)
    simpleSku ← * <~ SimpleSku(product.id, p.sku, p.price, p.currency, p.skuType)
    sku ← * <~ Skus.create(simpleSku.create)
    simpleSkuShadow ← * <~ SimpleSkuShadow(contextId, sku.id)
    skuShadow ← * <~ SkuShadows.create(simpleSkuShadow.create)
  } yield p.copy(
    productId = product.id,
    productShadowId = productShadow.id,
    skuId = sku.id,
    skuShadowId = skuShadow.id)

  def insertProducts(ps : Seq[SimpleProductData]): DbResultT[Seq[SimpleProductData]] = for {
    context ← * <~ ProductContexts.create(SimpleContext.create)
    results ← * <~ DbResultT.sequence(ps.map { p ⇒ insertProduct(context.id, p) } )
  } yield results

  def products: Seq[SimpleProductData] = Seq(
    SimpleProductData(sku = "SKU-YAX", title = "Flonkey", description = "Best in Class Flonkey", price = 3300),
    SimpleProductData(sku = "SKU-BRO", title = "Bronkey", description = "Bronze Bronkey", price = 15300),
    SimpleProductData(sku = "SKU-ABC", title = "Shark", description = "Dangerious Shark Pets", price = 4500, skuType = Sku.Preorder),
    SimpleProductData(sku = "SKU-SHH", title = "Sharkling", description = "Smaller Shark", price = 1500, skuType = Sku.Preorder),
    SimpleProductData(sku = "SKU-ZYA", title = "Dolphin", description = "A Dog named Dolphin", price = 8800, skuType = Sku.Backorder),
    SimpleProductData(sku = "SKU-MRP", title = "Morphin", description = "Power Ranger", price = 7700),
    // Why beetle? Cuz it's probably a bug. FIXME: add validation!!!
    SimpleProductData(sku = "SKU-TRL", title = "Beetle", description = "Music Album", price = -100, skuType = Sku.NonSellable, isActive = false))

  def context: ProductContext = SimpleContext.create
}
