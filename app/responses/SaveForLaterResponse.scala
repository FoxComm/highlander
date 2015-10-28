package responses

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{SaveForLater, SaveForLaters, Sku, Skus}
import services.{Result, NotFoundFailure404}
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._

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

  def forSkuId(skuId: Int)(implicit ec: ExecutionContext, db: Database): DbResult[Root] = {
    Skus.findOneById(skuId).zip(SaveForLaters.filter(_.skuId === skuId).one).flatMap {
      case (Some(sku), Some(sfl)) ⇒
        DbResult.good(build(sfl, sku))
      case (None, _) ⇒
        DbResult.failure(NotFoundFailure404(Sku, skuId))
      case (_, None) ⇒
        DbResult.failure(NotFoundFailure404(s"Save for later entry for sku with id=$skuId not found"))
    }
  }

  def build(sfl: SaveForLater, sku: Sku): Root =
    Root(
      id = sfl.id,
      name = sku.name,
      sku = sku.sku,
      price = sku.price,
      createdAt = sfl.createdAt
    )

}
