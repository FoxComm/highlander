package responses

import models.product.{Sku, Skus}
import models.{SaveForLater, SaveForLaters}
import services.NotFoundFailure404
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}
import utils.Slick.implicits._

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

  def forSkuId(skuId: Int)(implicit ec: ExecutionContext, db: Database): DbResultT[Root] = for {
    sku ← * <~ Skus.mustFindById404(skuId)
    sfl ← * <~ SaveForLaters.filter(_.skuId === skuId).one
                 .mustFindOr(NotFoundFailure404(s"Save for later entry for sku with id=$skuId not found"))
  } yield build(sfl, sku)

  def build(sfl: SaveForLater, sku: Sku): Root =
    Root(
      id = sfl.id,
      name = sku.name,
      sku = sku.sku,
      price = sku.price,
      createdAt = sfl.createdAt
    )

}
