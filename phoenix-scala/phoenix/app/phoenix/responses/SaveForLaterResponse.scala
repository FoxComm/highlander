package phoenix.responses

import java.time.Instant

import failures.NotFoundFailure404
import models.objects._
import phoenix.models.inventory.{Sku, Skus}
import phoenix.models.product.Mvp
import phoenix.models.{SaveForLater, SaveForLaters}
import slick.jdbc.PostgresProfile.api._
import utils.db._

object SaveForLaterResponse {

  case class Root(
      id: Int,
      name: Option[String],
      sku: String,
      price: Int,
      createdAt: Instant,
      imageUrl: String =
        "https://s-media-cache-ak0.pinimg.com/originals/37/0b/05/370b05c49ec83cae065c36fa0b3e5ada.jpg",
      favorite: Boolean = false
  )

  def forSkuId(skuId: Int, contextId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      sfl ← * <~ SaveForLaters
             .filter(_.skuId === skuId)
             .mustFindOneOr(
                 NotFoundFailure404(s"Save for later entry for sku with id=$skuId not found"))
      sku    ← * <~ Skus.mustFindById404(skuId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield build(sfl, sku, form, shadow)

  def build(sfl: SaveForLater, sku: Sku, form: ObjectForm, shadow: ObjectShadow): Root = {

    val price = Mvp.priceAsInt(form, shadow)
    val name  = Mvp.title(form, shadow)

    Root(
        id = sfl.id,
        name = name,
        sku = sku.code,
        price = price,
        createdAt = sfl.createdAt
    )
  }
}
