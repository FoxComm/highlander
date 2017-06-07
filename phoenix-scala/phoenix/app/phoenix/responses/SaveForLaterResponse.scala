package phoenix.responses

import java.time.Instant

import core.db._
import core.failures.NotFoundFailure404
import objectframework.FormShadowGet
import objectframework.models._
import phoenix.models.inventory.{Sku, Skus}
import phoenix.models.{SaveForLater, SaveForLaters}
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._

object SaveForLaterResponse {

  case class Root(
      id: Int,
      name: Option[String],
      sku: String,
      price: Long,
      createdAt: Instant,
      imageUrl: String =
        "https://s-media-cache-ak0.pinimg.com/originals/37/0b/05/370b05c49ec83cae065c36fa0b3e5ada.jpg",
      favorite: Boolean = false
  )

  def forSkuId(skuId: Int, contextId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      sfl ← * <~ SaveForLaters
             .filter(_.skuId === skuId)
             .mustFindOneOr(NotFoundFailure404(s"Save for later entry for sku with id=$skuId not found"))
      sku    ← * <~ Skus.mustFindById404(skuId)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
    } yield build(sfl, sku, form, shadow)

  def build(sfl: SaveForLater, sku: Sku, form: ObjectForm, shadow: ObjectShadow): Root =
    Root(
      id = sfl.id,
      name = FormShadowGet.title(form, shadow),
      sku = sku.code,
      price = FormShadowGet.priceAsLong(form, shadow),
      createdAt = sfl.createdAt
    )
}
