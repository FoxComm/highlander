package responses

import models.product.{Product, Products, ProductShadow, ProductShadows, Sku, Skus, SkuShadow, SkuShadows, Mvp}
import models.{SaveForLater, SaveForLaters}
import services.NotFoundFailure404
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}
import utils.Slick.implicits._
import utils.Money.Currency

import java.time.Instant
import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._

object SaveForLaterResponse {

  final case class Root(
    id: Int,
    name: Option[String],
    sku: String,
    price: Int,
    createdAt: Instant,
    imageUrl: String = "https://s-media-cache-ak0.pinimg.com/originals/37/0b/05/370b05c49ec83cae065c36fa0b3e5ada.jpg",
    favorite: Boolean = false
  )

  def forSkuId(skuId: Int, contextId: Int)(implicit ec: ExecutionContext, db: Database): DbResultT[Root] = for {
    sku ← * <~ Skus.mustFindById404(skuId)
    product ← * <~ Products.mustFindById404(sku.productId)
    productShadow ← * <~ ProductShadows.filter(_.productId === sku.productId).filter(_.productContextId === contextId).one
      .mustFindOr(NotFoundFailure404(s"Unable to find product with id ${sku.productId} for context $contextId"))
    skuShadow  ← * <~ SkuShadows.filter(_.skuId === sku.id).filter(_.productContextId === contextId).one
      .mustFindOr(NotFoundFailure404(s"Unable to find sku with id ${sku.id} for context $contextId"))
    sfl ← * <~ SaveForLaters.filter(_.skuId === skuId).one
      .mustFindOr(NotFoundFailure404(s"Save for later entry for sku with id=$skuId not found"))
  } yield build(sfl, sku, skuShadow, product, productShadow)

  def build(sfl: SaveForLater, sku: Sku, skuShadow: SkuShadow, product: Product, productShadow: ProductShadow): Root = { 

    val price = Mvp.price(sku, skuShadow).getOrElse((0, Currency.USD))
    val name = Mvp.name(product, productShadow)

    Root(
      id = sfl.id,
      name = name,
      sku = sku.sku,
      price = price._1,
      createdAt = sfl.createdAt
    )
  }

}
