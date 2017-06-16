package phoenix.services.returns

import objectframework.models.{ObjectForms, ObjectShadows}
import objectframework.DbObjectUtils._
import phoenix.models.returns._
import core.db.ExPostgresDriver.api._
import core.db._

object ReturnTotaler {
  def adjustmentsTotal(rma: Return)(implicit ec: EC): DbResultT[Long] = DbResultT.pure(0)

  def subTotal(rma: Return)(implicit ec: EC): DbResultT[Long] =
    (for {
      returnLineItems    ← ReturnLineItems if returnLineItems.returnId === rma.id
      returnLineItemSkus ← ReturnLineItemSkus if returnLineItemSkus.id === returnLineItems.id
      skus               ← returnLineItemSkus.sku
      form               ← ObjectForms if form.id === skus.formId
      shadow             ← ObjectShadows if shadow.id === returnLineItemSkus.skuShadowId
      illuminated = (form, shadow)
      total       = ((illuminated |→ "salePrice") +>> "value").asColumnOf[Long]
    } yield total).sum.filter(_ > 0L).getOrElse(0L).result.dbresult
}
