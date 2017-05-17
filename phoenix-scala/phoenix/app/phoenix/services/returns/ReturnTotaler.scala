package phoenix.services.returns

import phoenix.models.cord.lineitems.{CartLineItemAdjustments, OrderLineItems}
import phoenix.models.inventory.Skus
import models.objects.{ObjectForms, ObjectShadows}
import phoenix.models.payment.giftcard.GiftCards
import phoenix.models.returns._
import phoenix.utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._

object ReturnTotaler {
  def adjustmentsTotal(rma: Return)(implicit ec: EC): DbResultT[Int] = DbResultT.pure(0)

  def subTotal(rma: Return)(implicit ec: EC): DbResultT[Int] =
    (for {
      returnLineItems    ← ReturnLineItems if returnLineItems.returnId === rma.id
      returnLineItemSkus ← ReturnLineItemSkus if returnLineItemSkus.id === returnLineItems.id
      skus               ← returnLineItemSkus.sku
      form               ← ObjectForms if form.id === skus.formId
      shadow             ← ObjectShadows if shadow.id === returnLineItemSkus.skuShadowId

      total = ((form.attributes +> ((shadow.attributes +> "salePrice") +>> "ref")) +>> "value")
        .asColumnOf[Int]
    } yield total).sum.filter(_ > 0).getOrElse(0).result.dbresult
}
