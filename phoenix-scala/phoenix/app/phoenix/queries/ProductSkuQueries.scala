package phoenix.queries

import cats.data.EitherT
import objectframework.models._
import phoenix.models.inventory._
import phoenix.models.objects._
import phoenix.models.product._
import phoenix.utils.aliases.OC
import slick.jdbc.PostgresProfile.api._

object ProductSkuQueries {

  def bySkuCode(code: String)(implicit ctx: OC) =
    productSkuFormsQuery.filter {}

  /**
    * Basic query that joins some tables together. It does not run any filters
    */
  val productSkuFormsQuery = {
    // Sku and product can be linked either directly or via links
    val productQ = {
      val directProductQuery = for {
        sku         ← Skus
        productLink ← ProductSkuLinks if productLink.rightId === sku.id
      } yield (sku.id, productLink.leftId)

      val withVariantProductQuery =
        for {
          sku         ← Skus
          valueLink   ← VariantValueSkuLinks if valueLink.rightId === sku.id
          variantLink ← VariantValueLinks if variantLink.rightId === valueLink.leftId
          productLink ← ProductVariantLinks if productLink.rightId === variantLink.leftId
        } yield (sku.id, productLink.leftId)

      for {
        skuProduct ← directProductQuery.union(withVariantProductQuery)
        (skuId, productId) = skuProduct
        product       ← Products if product.id === productId
        productForm   ← ObjectForms if productForm.id === product.formId
        productShadow ← ObjectShadows if productShadow.id === product.shadowId
      } yield (skuId, (product, productForm, productShadow))
    }

    val skuQ = for {
      sku       ← Skus
      skuForm   ← ObjectForms if skuForm.id === sku.formId
      skuShadow ← ObjectShadows if skuShadow.id === sku.shadowId
    } yield (sku.id, (sku, skuForm, skuShadow))

    (skuQ joinLeft productQ)
      .on { case ((skuId1, _), (skuId2, _)) ⇒ skuId1 === skuId2 }
      .map {
        case ((skuId, skuFiSH), maybeProduct) ⇒
          (skuId, skuFiSH, maybeProduct.map { case (_, productFiSH) ⇒ productFiSH })
      }

  }
}
