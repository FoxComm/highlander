package services.returns

import cats.implicits._
import models.inventory.Skus
import models.objects.{ObjectForms, ObjectShadows}
import models.payment.giftcard.GiftCards
import models.returns._
import utils.aliases._
import utils.db.ExPostgresDriver.api._

object ReturnTotaler {
  def subTotal(rma: Return)(implicit ec: EC): DBIO[Option[Int]] =
    (for {
      returnLineItems    ← ReturnLineItems if returnLineItems.returnId === rma.id
      returnLineItemSkus ← ReturnLineItemSkus if returnLineItemSkus.id === returnLineItems.id
      skus               ← Skus if skus.id === returnLineItemSkus.skuId
      form               ← ObjectForms if form.id === skus.formId
      shadow             ← ObjectShadows if shadow.id === returnLineItemSkus.skuShadowId
      gcli               ← ReturnLineItemGiftCards if gcli.id === returnLineItems.id
      gc                 ← GiftCards if gc.id === gcli.giftCardId

      total = ((form.attributes +> ((shadow.attributes +> "salePrice") +>> "ref")) +>> "value")
        .asColumnOf[Int]
    } yield gc.originalBalance + total).sum.result.map {
      case Some(count) if count > 0 ⇒ count.some
      case _                        ⇒ None
    }
}
