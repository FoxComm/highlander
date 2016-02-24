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
    skuShadow  ← * <~ SkuShadows.filter(_.skuId === sku.id).filter(_.productContextId === contextId).one
      .mustFindOr(NotFoundFailure404(s"Unable to find sku with id ${sku.id} for context $contextId"))
    sfl ← * <~ SaveForLaters.filter(_.skuId === skuId).one
      .mustFindOr(NotFoundFailure404(s"Save for later entry for sku with id=$skuId not found"))
  } yield build(sfl, sku, skuShadow)

  def build(sfl: SaveForLater, sku: Sku, skuShadow: SkuShadow): Root = { 

    val price = Mvp.priceAsInt(sku, skuShadow)
    val name = Mvp.name(sku, skuShadow)

    Console.err.println(s"NAME : ${name} price ${price}")

    Root(
      id = sfl.id,
      name = name,
      sku = sku.sku,
      price = price,
      createdAt = sfl.createdAt
    )
  }

}
