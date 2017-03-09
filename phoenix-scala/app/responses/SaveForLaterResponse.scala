package responses

import java.time.Instant

import failures.NotFoundFailure404
import models.inventory.{ProductVariant, ProductVariants}
import models.objects._
import models.product.Mvp
import models.{SaveForLater, SaveForLaters}
import slick.driver.PostgresDriver.api._
import utils.aliases._
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

  def forVariantId(variantId: Int, contextId: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      sfl ← * <~ SaveForLaters
             .filter(_.productVariantId === variantId)
             .mustFindOneOr(NotFoundFailure404(
                     s"Save for later entry for product variant with id=$variantId not found"))
      variant ← * <~ ProductVariants.mustFindById404(variantId)
      form    ← * <~ ObjectForms.mustFindById404(variant.formId)
      shadow  ← * <~ ObjectShadows.mustFindById404(variant.shadowId)
    } yield build(sfl, variant, form, shadow)

  def build(sfl: SaveForLater,
            productVariant: ProductVariant,
            form: ObjectForm,
            shadow: ObjectShadow): Root = {

    val price = Mvp.priceAsInt(form, shadow)
    val name  = Mvp.title(form, shadow)

    Root(
        id = sfl.id,
        name = name,
        sku = productVariant.code,
        price = price,
        createdAt = sfl.createdAt
    )
  }
}
